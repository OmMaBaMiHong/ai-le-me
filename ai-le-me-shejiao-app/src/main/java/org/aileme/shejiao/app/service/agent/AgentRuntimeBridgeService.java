package org.aileme.shejiao.app.service.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.ChatMessageService;
import org.aileme.shejiao.api.service.FriendService;
import org.aileme.shejiao.api.service.PlatformBusinessConfigService;
import org.aileme.shejiao.api.service.UserPersonaSnapshotService;
import org.aileme.shejiao.app.service.TagProfileAggregateService;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.UserPersonaSnapshotEntity;
import org.aileme.shejiao.domain.entity.app.RecommendLoveEntity;
import org.aileme.shejiao.domain.entity.app.ChatMessageEntity;
import org.aileme.shejiao.domain.param.app.AgentGiftPlanForm;
import org.aileme.shejiao.domain.param.app.AgentIcebreakForm;
import org.aileme.shejiao.domain.param.app.AgentNextStepForm;
import org.aileme.shejiao.domain.param.app.AgentReplyForm;
import org.aileme.shejiao.domain.param.app.AgentDistillationGenerateForm;
import org.aileme.shejiao.domain.vo.AgentGiftPlanVo;
import org.aileme.shejiao.domain.vo.AgentCoachStyleVo;
import org.aileme.shejiao.domain.vo.AgentNextStepVo;
import org.aileme.shejiao.domain.vo.AgentNextStepActionVo;
import org.aileme.shejiao.domain.vo.AgentPermissionVo;
import org.aileme.shejiao.domain.vo.AgentProgramActionVo;
import org.aileme.shejiao.domain.vo.AgentProgramRunVo;
import org.aileme.shejiao.domain.vo.AgentRelationshipEvaluateVo;
import org.aileme.shejiao.domain.vo.AgentSuggestionResponseVo;
import org.aileme.shejiao.domain.vo.AgentSuggestionVo;
import org.aileme.shejiao.domain.vo.TagProfileAggregateVO;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Python runtime 桥接服务
 */
@Slf4j
@Service
public class AgentRuntimeBridgeService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:8091";
    private static final int AGENT_RUNTIME_CONNECT_TIMEOUT_MS = 5_000;
    private static final int AGENT_RUNTIME_READ_TIMEOUT_MS = 120_000;

    @Autowired
    private PlatformBusinessConfigService businessConfigService;
    @Autowired
    private AppUserService appUserService;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private TagProfileAggregateService tagProfileAggregateService;
    @Lazy
    @Autowired
    private UserPersonaSnapshotService userPersonaSnapshotService;
    @Autowired
    private FriendService friendService;
    @Autowired
    private AgentSafetyService safetyService;
    @Autowired(required = false)
    private AgentAccessPolicyService agentAccessPolicyService;
    @Autowired(required = false)
    private AgentGovernanceService agentGovernanceService;

    private volatile RestTemplate agentRuntimeRestTemplate;

    public AgentSuggestionResponseVo suggestIcebreak(AppUserEntity me, AgentIcebreakForm form) {
        if (!isCompanionEnabled()) {
            return null;
        }
        AppUserEntity target = appUserService.getById(form.getTargetUid());
        if (target == null) {
            return null;
        }
        JSONObject payload = buildCompanionPayload(
                me,
                target,
                null,
                form.getScene(),
                "warm",
                "icebreak",
                true
        );
        JSONObject response = postJson("/companion/reply/suggest", payload);
        if (response == null) {
            return null;
        }
        return toSuggestionResponse("icebreak", response, Math.max(1, Math.min(5, form.getLimit())));
    }

    public AgentSuggestionResponseVo suggestReply(AppUserEntity me, AgentReplyForm form) {
        if (!isCompanionEnabled()) {
            return null;
        }
        AppUserEntity target = appUserService.getById(form.getTargetUid());
        if (target == null) {
            return null;
        }
        JSONObject payload = buildCompanionPayload(
                me,
                target,
                form.getSessionId(),
                "reply",
                StringUtils.defaultIfBlank(form.getTone(), "warm"),
                "build_trust",
                false
        );
        if (form.getLastMessages() != null && !form.getLastMessages().isEmpty()) {
            payload.put("recent_messages", buildRecentMessagesFromForm(form.getLastMessages()));
        }
        JSONObject response = postJson("/companion/reply/suggest", payload);
        if (response == null) {
            return null;
        }
        return toSuggestionResponse("reply", response, 3);
    }

    public AgentGiftPlanVo suggestGiftPlan(AppUserEntity me, AgentGiftPlanForm form) {
        if (!isCompanionEnabled()) {
            return null;
        }
        AppUserEntity target = appUserService.getById(form.getTargetUid());
        if (target == null) {
            return null;
        }
        JSONObject payload = buildCompanionPayload(
                me,
                target,
                form.getSessionId(),
                StringUtils.defaultIfBlank(form.getScene(), "social_intent"),
                "warm",
                StringUtils.defaultIfBlank(form.getObjective(), "break_ice"),
                true
        );
        payload.put("scene", StringUtils.defaultIfBlank(form.getScene(), "social_intent"));
        payload.put("objective", StringUtils.defaultIfBlank(form.getObjective(), "break_ice"));
        payload.put("budget_options", newJsonArray(normalizeBudgets(form.getBudgetOptions())));
        payload.put("gift_context", buildGiftContext(form, target));

        JSONObject response = postJson("/companion/gift/plan", payload);
        if (response == null) {
            return null;
        }
        JSONArray gifts = response.getJSONArray("gifts");
        if (gifts == null || gifts.isEmpty()) {
            return null;
        }
        List<AgentGiftPlanVo.GiftOption> options = new ArrayList<>();
        for (int i = 0; i < gifts.size(); i++) {
            JSONObject item = gifts.getJSONObject(i);
            if (item == null) {
                continue;
            }
            options.add(AgentGiftPlanVo.GiftOption.builder()
                    .code(StringUtils.defaultIfBlank(item.getString("code"), "gift_" + i))
                    .name(StringUtils.defaultIfBlank(item.getString("name"), "礼物推荐"))
                    .desc(StringUtils.defaultIfBlank(item.getString("desc"), "先把诚意表达出来"))
                    .scene(StringUtils.defaultIfBlank(item.getString("scene"), "心动时刻"))
                    .icon(StringUtils.defaultIfBlank(item.getString("icon"), "💌"))
                    .theme(StringUtils.defaultIfBlank(item.getString("theme"), "heart"))
                    .templateCode(StringUtils.defaultIfBlank(item.getString("template_code"), item.getString("templateCode")))
                    .animationPreset(StringUtils.defaultIfBlank(item.getString("animation_preset"), item.getString("animationPreset")))
                    .revealEffect(StringUtils.defaultIfBlank(item.getString("reveal_effect"), item.getString("revealEffect")))
                    .soundEffectKey(StringUtils.defaultIfBlank(item.getString("sound_effect_key"), item.getString("soundEffectKey")))
                    .soundEffectUrl(StringUtils.defaultIfBlank(item.getString("sound_effect_url"), item.getString("soundEffectUrl")))
                    .amount(item.getInteger("amount"))
                    .displayAmount(StringUtils.defaultIfBlank(item.getString("display_amount"), "13.14"))
                    .messageDraft(StringUtils.defaultIfBlank(item.getString("message_draft"), "想先认真和你聊聊。"))
                    .rationale(StringUtils.defaultIfBlank(item.getString("rationale"), "基于当前关系阶段推荐"))
                    .visualPrompt(item.getString("visual_prompt"))
                    .motionPrompt(item.getString("motion_prompt"))
                    .nextAction(StringUtils.defaultIfBlank(item.getString("next_action"), "open_chat"))
                    .riskLevel(StringUtils.defaultIfBlank(item.getString("risk_level"), "low"))
                    .recommended(Boolean.TRUE.equals(item.getBoolean("recommended")))
                    .build());
        }
        if (options.isEmpty()) {
            return null;
        }
        return AgentGiftPlanVo.builder()
                .scene(StringUtils.defaultIfBlank(response.getString("scene"), StringUtils.defaultIfBlank(form.getScene(), "social_intent")))
                .provider(StringUtils.defaultIfBlank(response.getString("provider"), "agent_runtime"))
                .relationshipStage(StringUtils.defaultIfBlank(response.getString("relationship_stage"), "early"))
                .strategyNote(StringUtils.defaultIfBlank(response.getString("strategy_note"), "先用轻心意打开聊天。"))
                .wechatPrompt(StringUtils.defaultIfBlank(response.getString("wechat_prompt"), "先聊得自然，再顺势申请微信。"))
                .gifts(options)
                .build();
    }

    public AgentNextStepVo suggestNextStep(AppUserEntity me, AgentNextStepForm form) {
        if (!isCompanionEnabled()) {
            return null;
        }
        AppUserEntity target = appUserService.getById(form.getTargetUid());
        if (target == null) {
            return null;
        }
        JSONObject payload = buildCompanionPayload(
                me,
                target,
                form.getSessionId(),
                "next_step",
                "warm",
                StringUtils.defaultIfBlank(form.getObjective(), "move_relationship_forward"),
                false
        );
        payload.put("objective", StringUtils.defaultIfBlank(form.getObjective(), "move_relationship_forward"));
        payload.put("policy_context", buildCompanionPolicyContext(me, target));
        if (form.getLastMessages() != null && !form.getLastMessages().isEmpty()) {
            payload.put("recent_messages", buildRecentMessagesFromForm(form.getLastMessages()));
        }
        JSONObject response = postJson("/companion/strategy/next-step", payload);
        if (response == null) {
            return null;
        }
        return AgentNextStepVo.builder()
                .provider(StringUtils.defaultIfBlank(response.getString("provider"), "agent_runtime"))
                .relationshipStage(StringUtils.defaultIfBlank(response.getString("relationship_stage"), "early"))
                .heatScore(toDouble(response.get("heat_score")))
                .trustScore(toDouble(response.get("trust_score")))
                .toneWarmth(toDouble(response.get("tone_warmth")))
                .intimacyScore(toDouble(response.get("intimacy_score")))
                .progressionScore(toDouble(response.get("progression_score")))
                .dateReadyScore(toDouble(response.get("date_ready_score")))
                .wechatReadyScore(toDouble(response.get("wechat_ready_score")))
                .riskScore(toDouble(response.get("risk_score")))
                .nextBestAction(StringUtils.defaultIfBlank(response.getString("next_best_action"), "先保持自然互动。"))
                .next24hPlan(toStringList(response.getJSONArray("next_24h_plan")))
                .guardrails(toStringList(response.getJSONArray("guardrails")))
                .suggestedActions(toNextStepActions(response.getJSONArray("suggested_actions")))
                .shouldAutoExecute(Boolean.TRUE.equals(response.getBoolean("should_auto_execute")))
                .build();
    }

    public AgentRelationshipEvaluateVo evaluateRelationship(AppUserEntity me,
                                                            Integer targetUid,
                                                            String sessionId,
                                                            List<String> lastMessages,
                                                            String personaSummary) {
        if (!isCompanionEnabled()) {
            return null;
        }
        AppUserEntity target = appUserService.getById(targetUid);
        if (target == null) {
            return null;
        }
        JSONObject payload = buildAutonomyPayload(me, target, sessionId, lastMessages, personaSummary);
        JSONObject response = postJson("/companion/relationship/evaluate", payload);
        if (response == null) {
            return null;
        }
        return AgentRelationshipEvaluateVo.builder()
                .stageCode(StringUtils.defaultIfBlank(response.getString("stage_code"), "early"))
                .heatScore(toDouble(response.get("heat_score")))
                .trustScore(toDouble(response.get("trust_score")))
                .toneWarmth(toDouble(response.get("tone_warmth")))
                .intimacyScore(toDouble(response.get("intimacy_score")))
                .progressionScore(toDouble(response.get("progression_score")))
                .dateReadyScore(toDouble(response.get("date_ready_score")))
                .wechatReadyScore(toDouble(response.get("wechat_ready_score")))
                .riskScore(toDouble(response.get("risk_score")))
                .masterStyleCode(StringUtils.defaultIfBlank(response.getString("master_style_code"), "steady_partner"))
                .recommendedNextAction(StringUtils.defaultIfBlank(response.getString("recommended_next_action"), "reply_send"))
                .reasoningSummary(StringUtils.defaultIfBlank(response.getString("reasoning_summary"), ""))
                .triggerHints(toStringList(response.getJSONArray("trigger_hints")))
                .build();
    }

    public AgentCoachStyleVo selectCoachStyle(AppUserEntity me,
                                              Integer targetUid,
                                              AgentRelationshipEvaluateVo state,
                                              String personaSummary) {
        if (!isCompanionEnabled() || state == null) {
            return null;
        }
        AppUserEntity target = appUserService.getById(targetUid);
        if (target == null) {
            return null;
        }
        JSONObject payload = new JSONObject(true);
        JSONObject personaRoot = loadPersonaRoot(targetUid);
        payload.put("owner", buildUserCard(me));
        payload.put("target", buildUserCard(target));
        payload.put("relationship_tags", buildRelationshipTags(targetUid));
        payload.put("graph_facts", buildGraphFacts(me, target));
        payload.put("persona_summary", resolvePersonaSummary(personaRoot, target, personaSummary));
        appendPersonaKernel(payload, personaRoot);
        payload.put("stage_code", StringUtils.defaultIfBlank(state.getStageCode(), "early"));
        payload.put("date_ready_score", state.getDateReadyScore());
        payload.put("wechat_ready_score", state.getWechatReadyScore());
        JSONObject response = postJson("/companion/style/select", payload);
        if (response == null) {
            return null;
        }
        return AgentCoachStyleVo.builder()
                .styleCode(StringUtils.defaultIfBlank(response.getString("style_code"), "steady_partner"))
                .styleName(StringUtils.defaultIfBlank(response.getString("style_name"), "成熟稳重型"))
                .toneHint(StringUtils.defaultIfBlank(response.getString("tone_hint"), "真诚、克制、可靠"))
                .openingHint(StringUtils.defaultIfBlank(response.getString("opening_hint"), "用生活感和兑现感建立信任"))
                .doNotUseStyles(toStringList(response.getJSONArray("do_not_use_styles")))
                .reasoningSummary(StringUtils.defaultIfBlank(response.getString("reasoning_summary"), ""))
                .build();
    }

    public AgentProgramRunVo runRelationshipProgram(AppUserEntity me,
                                                    Integer targetUid,
                                                    String triggerCode,
                                                    String sessionId,
                                                    List<String> lastMessages,
                                                    String personaSummary,
                                                    JSONObject matchOpeningSignal) {
        if (!isCompanionEnabled()) {
            return null;
        }
        AppUserEntity target = appUserService.getById(targetUid);
        if (target == null) {
            return null;
        }
        JSONObject payload = buildAutonomyPayload(me, target, sessionId, lastMessages, personaSummary);
        payload.put("trigger_code", StringUtils.defaultIfBlank(triggerCode, "incoming_message"));
        if (matchOpeningSignal != null && !matchOpeningSignal.isEmpty()) {
            payload.put("match_opening_signal", matchOpeningSignal);
        }
        JSONObject response = postJson("/companion/program/run", payload);
        if (response == null) {
            return null;
        }
        JSONObject nextAction = response.getJSONObject("next_action");
        return AgentProgramRunVo.builder()
                .workflowCode(StringUtils.defaultIfBlank(response.getString("workflow_code"), "relationship_program"))
                .stageCode(StringUtils.defaultIfBlank(response.getString("stage_code"), "early"))
                .masterStyleCode(StringUtils.defaultIfBlank(response.getString("master_style_code"), "steady_partner"))
                .nextAction(nextAction == null ? null : AgentProgramActionVo.builder()
                        .actionType(StringUtils.defaultIfBlank(nextAction.getString("action_type"), "reply_send"))
                        .content(StringUtils.defaultIfBlank(nextAction.getString("content"), ""))
                        .targetUserId(nextAction.getInteger("target_user_id"))
                        .riskLevel(StringUtils.defaultIfBlank(nextAction.getString("risk_level"), "low"))
                        .requiresApproval(Boolean.TRUE.equals(nextAction.getBoolean("requires_approval")))
                        .masterStyleCode(StringUtils.defaultIfBlank(nextAction.getString("master_style_code"), "steady_partner"))
                        .executeMode(StringUtils.defaultIfBlank(nextAction.getString("execute_mode"), "draft_only"))
                        .payloadJson(nextAction.getJSONObject("payload") == null ? "{}" : nextAction.getJSONObject("payload").toJSONString())
                        .build())
                .next24hPlan(toStringList(response.getJSONArray("next_24h_plan")))
                .reasoningSummary(StringUtils.defaultIfBlank(response.getString("reasoning_summary"), ""))
                .build();
    }

    public boolean ingestRelationshipEvent(AppUserEntity owner,
                                           AppUserEntity target,
                                           String eventType,
                                           String eventTime,
                                           JSONObject eventPayload) {
        if (!isCompanionEnabled() || owner == null || target == null || StringUtils.isBlank(eventType)) {
            return false;
        }
        JSONObject payload = new JSONObject(true);
        payload.put("event_type", StringUtils.defaultIfBlank(eventType, "incoming_message"));
        payload.put("owner", buildUserCard(owner));
        payload.put("target", buildUserCard(target));
        payload.put("event_time", StringUtils.defaultIfBlank(eventTime, null));
        payload.put("payload", eventPayload == null ? new JSONObject(true) : eventPayload);
        JSONObject response = postJson("/companion/event/ingest", payload);
        return response != null && !Boolean.FALSE.equals(response.getBoolean("accepted"));
    }

    public JSONObject generatePersonaReport(AppUserEntity owner, AppUserEntity target) {
        if (!isPersonaEnabled() || target == null) {
            return null;
        }
        JSONObject payload = new JSONObject(true);
        if (owner != null) {
            payload.put("owner", buildUserCard(owner));
        }
        payload.put("target", buildUserCard(target));
        payload.put("recent_messages", buildPersonaMessages(target.getUid()));
        payload.put("relationship_tags", buildRelationshipTags(target.getUid()));
        payload.put("graph_facts", buildGraphFacts(owner, target));
        payload.put("recent_moments", new JSONArray());
        payload.put("behavior_signals", buildBehaviorSignals(target.getUid()));
        payload.put("report_goal", owner != null && Objects.equals(owner.getUid(), target.getUid()) ? "self_persona" : "dating_companion");
        return postJson("/persona/report/generate", payload);
    }

    public JSONObject generateDistillation(AppUserEntity owner, AgentDistillationGenerateForm form) {
        if (!isDistillationEnabled()) {
            return null;
        }
        AgentDistillationGenerateForm safeForm = form == null ? new AgentDistillationGenerateForm() : form;
        JSONObject payload = new JSONObject(true);
        payload.put("scene_type", StringUtils.defaultIfBlank(safeForm.getSceneType(), "self_bootstrap"));
        payload.put("subject_type", StringUtils.defaultIfBlank(safeForm.getSubjectType(), "private_person"));
        payload.put("relation_label", safeForm.getRelationLabel());
        payload.put("analysis_goal", safetyService.sanitizeText(StringUtils.defaultString(safeForm.getAnalysisGoal()), 200));
        if (owner != null) {
            payload.put("owner", buildUserCard(owner));
        }
        JSONObject subject = new JSONObject(true);
        subject.put("nickname", StringUtils.defaultIfBlank(safeForm.getSubjectName(),
                owner == null ? "我自己" : firstNonBlank(owner.getUsername(), owner.getMobile(), "我自己")));
        payload.put("subject", subject);
        payload.put("answers", buildDistillationAnswers(safeForm.getAnswers()));
        payload.put("materials", buildDistillationMaterials(safeForm.getMaterials()));
        payload.put("signals", new JSONArray());
        payload.put("policy_context", new JSONObject(true));
        return postJson("/distillation/profile/generate", payload);
    }

    public JSONObject recommendSmartMatches(AppUserEntity owner,
                                            RecommendLoveEntity setting,
                                            List<AppUserEntity> candidates,
                                            Set<Integer> likedUids,
                                            Set<Integer> fanUids,
                                            Integer limit) {
        if (!isCompanionEnabled() || owner == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        JSONObject payload = buildSmartMatchPayload(owner, setting, candidates, likedUids, fanUids);
        payload.put("limit", limit == null ? 6 : Math.max(1, Math.min(20, limit)));
        return postJson("/match/recommend", payload);
    }

    public JSONObject preGenerateSmartMatches(AppUserEntity owner,
                                              RecommendLoveEntity setting,
                                              List<AppUserEntity> candidates,
                                              Set<Integer> likedUids,
                                              Set<Integer> fanUids,
                                              Integer limit) {
        if (!isCompanionEnabled() || owner == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        JSONObject payload = buildSmartMatchPayload(owner, setting, candidates, likedUids, fanUids);
        payload.put("limit", limit == null ? 30 : Math.max(1, Math.min(100, limit)));
        return postJson("/match/pregenerate", payload);
    }

    public boolean isPersonaEnabled() {
        return isEnabled("agent.runtime.enabled", true)
                && isEnabled("agent.runtime.persona.enabled", true)
                && StringUtils.isNotBlank(getBaseUrl());
    }

    public boolean isCompanionEnabled() {
        return isEnabled("agent.runtime.enabled", true)
                && isEnabled("agent.runtime.companion.enabled", true)
                && StringUtils.isNotBlank(getBaseUrl());
    }

    public boolean isDistillationEnabled() {
        return isEnabled("agent.runtime.enabled", true)
                && isEnabled("agent.runtime.distillation.enabled", true)
                && StringUtils.isNotBlank(getBaseUrl());
    }

    public String buildLegacyPersonaJson(JSONObject runtimeReport) {
        if (runtimeReport == null) {
            return "";
        }
        JSONObject root = new JSONObject(true);
        JSONObject personaKernel = runtimeReport.getJSONObject("persona_kernel");
        root.put("summary", StringUtils.defaultIfBlank(runtimeReport.getString("summary"), "画像生成成功"));
        if (personaKernel != null && !personaKernel.isEmpty()) {
            root.put("persona_kernel", personaKernel);
        }
        root.put("preferred_master_style_code",
                StringUtils.defaultIfBlank(runtimeReport.getString("preferred_master_style_code"),
                        personaKernel == null ? null : personaKernel.getString("preferred_master_style_code")));
        root.put("style_router_reason",
                StringUtils.defaultIfBlank(runtimeReport.getString("style_router_reason"),
                        personaKernel == null ? null : personaKernel.getString("style_router_reason")));
        JSONArray coachStyleCandidates = runtimeReport.getJSONArray("coach_style_candidates");
        if (coachStyleCandidates == null && personaKernel != null) {
            coachStyleCandidates = personaKernel.getJSONArray("coach_style_candidates");
        }
        if (coachStyleCandidates != null) {
            root.put("coach_style_candidates", toJsonArray(coachStyleCandidates));
        }

        JSONObject personality = new JSONObject(true);
        JSONArray coreTraits = runtimeReport.getJSONArray("core_traits");
        personality.put("introvert_extrovert", findTraitText(coreTraits, "openness", "开放度中等"));
        personality.put("stability", findTraitText(coreTraits, "stability", "情绪相对稳定"));
        personality.put("openness", findTraitText(coreTraits, "engagement", "互动意愿适中"));
        root.put("personality", personality);

        JSONObject loveStyle = new JSONObject(true);
        loveStyle.put("attitude", StringUtils.defaultIfBlank(runtimeReport.getString("attachment_style"),
                personaKernel == null ? "慢热认真" : StringUtils.defaultIfBlank(personaKernel.getString("attachment_style"), "慢热认真")));
        loveStyle.put("risk_points", toJsonArray(runtimeReport.getJSONArray("risk_flags")));
        if (personaKernel != null) {
            root.put("love_language", toJsonArray(personaKernel.getJSONArray("love_language")));
            root.put("romance_pace", StringUtils.defaultIfBlank(personaKernel.getString("romance_pace"), ""));
            if (loveStyle.getJSONArray("risk_points") == null || loveStyle.getJSONArray("risk_points").isEmpty()) {
                loveStyle.put("risk_points", toJsonArray(personaKernel.getJSONArray("taboo_rules")));
            }
        }
        root.put("love_style", loveStyle);

        JSONObject socialStyle = new JSONObject(true);
        String expressionStyle = personaKernel == null ? null : personaKernel.getString("expression_style");
        socialStyle.put("online", StringUtils.defaultIfBlank(runtimeReport.getString("emotional_style"),
                StringUtils.defaultIfBlank(expressionStyle, "线上表达偏自然")));
        socialStyle.put("offline", StringUtils.defaultIfBlank(expressionStyle,
                StringUtils.defaultIfBlank(runtimeReport.getString("emotional_style"), "线下需要更多安全感")));
        root.put("social_style", socialStyle);

        root.put("tags_highlight", toJsonArray(runtimeReport.getJSONArray("interest_clusters")));
        root.put("suggestions", toJsonArray(runtimeReport.getJSONArray("approach_suggestions")));
        root.put("core_traits", toJsonArray(coreTraits));
        root.put("evidence_digest", toJsonArray(runtimeReport.getJSONArray("evidence_digest")));
        root.put("provider", StringUtils.defaultIfBlank(runtimeReport.getString("provider"), "agent_runtime"));
        root.put("route_profile", runtimeReport.getString("route_profile"));
        return root.toJSONString();
    }

    private JSONObject buildCompanionPayload(AppUserEntity me,
                                             AppUserEntity target,
                                             String sessionId,
                                             String scene,
                                             String desiredTone,
                                             String ownerGoal,
                                             boolean allowEmptyRecentMessages) {
        JSONObject payload = new JSONObject(true);
        JSONObject personaRoot = loadPersonaRoot(target == null ? null : target.getUid());
        payload.put("owner", buildUserCard(me));
        payload.put("target", buildUserCard(target));
        payload.put("memory", buildRelationshipMemory(me, target));
        appendPersonaKernel(payload, personaRoot);
        JSONArray recentMessages = buildConversationMessages(me.getUid(), target.getUid(), sessionId);
        if (recentMessages.isEmpty() && !allowEmptyRecentMessages) {
            recentMessages = buildRecentMessagesFromForm(Collections.emptyList());
        }
        payload.put("recent_messages", recentMessages);
        payload.put("owner_goal", ownerGoal);
        payload.put("desired_tone", desiredTone);
        payload.put("available_skills", new JSONArray(Arrays.asList("relationship_radar")));
        return payload;
    }

    private JSONObject buildCompanionPolicyContext(AppUserEntity owner, AppUserEntity target) {
        JSONObject context = new JSONObject(true);
        AgentPermissionVo companionPermission = agentGovernanceService == null ? null
                : agentGovernanceService.getPermission(owner == null ? null : owner.getUid(), AgentGovernanceConstants.CAPABILITY_COMPANION_ENABLED);
        AgentPermissionVo messagePermission = agentGovernanceService == null ? null
                : agentGovernanceService.getPermission(owner == null ? null : owner.getUid(), AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND);
        AgentPermissionVo giftPlanPermission = agentGovernanceService == null ? null
                : agentGovernanceService.getPermission(owner == null ? null : owner.getUid(), AgentGovernanceConstants.CAPABILITY_GIFT_PLAN);
        org.aileme.shejiao.domain.vo.AgentCompanionStatusVo status = agentAccessPolicyService == null || owner == null
                ? null
                : agentAccessPolicyService.getStatus(owner);
        boolean isFriend = owner != null
                && target != null
                && Boolean.TRUE.equals(friendService.checkIsFriend(owner.getUid(), target.getUid()));
        boolean targetAuthorized = isTargetAuthorized(target, isFriend, companionPermission, messagePermission, giftPlanPermission);

        JSONObject switches = new JSONObject(true);
        switches.put(AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND, permissionEnabled(messagePermission));
        switches.put(AgentGovernanceConstants.CAPABILITY_GIFT_PLAN, permissionEnabled(giftPlanPermission));

        JSONArray authorizedTargetIds = new JSONArray();
        if (targetAuthorized && target != null && target.getUid() != null) {
            authorizedTargetIds.add(target.getUid());
        }

        context.put("vip_active", status != null && Boolean.TRUE.equals(status.getVipActive()));
        context.put("assistant_enabled", status != null && Boolean.TRUE.equals(status.getCompanionEnabled()));
        context.put("capability_switches", switches);
        context.put("authorized_target_ids", authorizedTargetIds);
        context.put("channel_allowed", true);
        context.put("quiet_hours_active", false);
        context.put("frequency_limit_reached", false);
        context.put("love_coin_balance", status == null ? 0 : defaultInt(status.getCoinBalance()));
        context.put("provider_budget_available", status == null
                || !Boolean.TRUE.equals(status.getBillingEnabled())
                || defaultInt(status.getVipDailyFreeRemaining()) > 0
                || defaultInt(status.getCoinBalance()) >= defaultInt(status.getEstimatedReplyCoin()));
        context.put("risk_score", targetAuthorized ? 0.18D : 0.82D);
        return context;
    }

    private JSONObject buildAutonomyPayload(AppUserEntity me,
                                            AppUserEntity target,
                                            String sessionId,
                                            List<String> lastMessages,
                                            String personaSummary) {
        JSONObject payload = new JSONObject(true);
        JSONObject personaRoot = loadPersonaRoot(target == null ? null : target.getUid());
        payload.put("owner", buildUserCard(me));
        payload.put("target", buildUserCard(target));
        JSONArray recentMessages = buildConversationMessages(me == null ? null : me.getUid(), target.getUid(), sessionId);
        if ((recentMessages == null || recentMessages.isEmpty()) && lastMessages != null && !lastMessages.isEmpty()) {
            recentMessages = buildRecentMessagesFromForm(lastMessages);
        }
        payload.put("recent_messages", recentMessages == null ? new JSONArray() : recentMessages);
        payload.put("behavior_signals", buildRelationshipBehaviorSignals(me, target, sessionId, recentMessages));
        payload.put("relationship_tags", buildRelationshipTags(target.getUid()));
        payload.put("graph_facts", buildGraphFacts(me, target));
        payload.put("persona_summary", resolvePersonaSummary(personaRoot, target, personaSummary));
        appendPersonaKernel(payload, personaRoot);
        return payload;
    }

    private JSONObject loadPersonaRoot(Integer userId) {
        if (userId == null || userPersonaSnapshotService == null) {
            return null;
        }
        try {
            UserPersonaSnapshotEntity snapshot = userPersonaSnapshotService.getLatestByUserId(userId);
            if (snapshot == null || StringUtils.isBlank(snapshot.getPersonaJson())) {
                return null;
            }
            return JSON.parseObject(snapshot.getPersonaJson());
        } catch (Exception ex) {
            log.warn("加载画像内核失败, userId={}, err={}", userId, ex.getMessage());
            return null;
        }
    }

    private void appendPersonaKernel(JSONObject payload, JSONObject personaRoot) {
        if (payload == null || personaRoot == null) {
            return;
        }
        JSONObject kernel = personaRoot.getJSONObject("persona_kernel");
        if (kernel != null && !kernel.isEmpty()) {
            payload.put("persona_kernel", kernel);
        }
    }

    private String resolvePersonaSummary(JSONObject personaRoot, AppUserEntity target, String explicitSummary) {
        if (StringUtils.isNotBlank(explicitSummary)) {
            return explicitSummary;
        }
        if (personaRoot != null) {
            String summary = personaRoot.getString("summary");
            if (StringUtils.isNotBlank(summary)) {
                return summary;
            }
            JSONObject kernel = personaRoot.getJSONObject("persona_kernel");
            if (kernel != null) {
                String routerReason = kernel.getString("style_router_reason");
                if (StringUtils.isNotBlank(routerReason)) {
                    return routerReason;
                }
                String relationshipStyle = kernel.getString("relationship_style");
                if (StringUtils.isNotBlank(relationshipStyle)) {
                    return relationshipStyle;
                }
            }
        }
        return buildUserSummary(target);
    }

    private JSONObject buildSmartMatchPayload(AppUserEntity owner,
                                              RecommendLoveEntity setting,
                                              List<AppUserEntity> candidates,
                                              Set<Integer> likedUids,
                                              Set<Integer> fanUids) {
        JSONObject payload = new JSONObject(true);
        payload.put("owner", buildUserCard(owner));

        JSONObject preference = new JSONObject(true);
        List<String> cities = parseCsv(setting == null ? null : setting.getCityids());
        preference.put("preferred_cities", newJsonArray(cities));
        List<Integer> ageRange = parseIntegerList(setting == null ? null : setting.getAge());
        if (!ageRange.isEmpty()) {
            preference.put("age_min", ageRange.get(0));
            preference.put("age_max", ageRange.get(ageRange.size() > 1 ? 1 : 0));
        }
        List<Integer> heightRange = parseIntegerList(setting == null ? null : setting.getHeight());
        if (!heightRange.isEmpty()) {
            preference.put("height_min", heightRange.get(0));
            preference.put("height_max", heightRange.get(heightRange.size() > 1 ? 1 : 0));
        }
        preference.put("education_levels", newJsonArray(parseIntegerList(setting == null ? null : setting.getEdu())));
        preference.put("prefer_fans", setting != null && Objects.equals(setting.getRecFollowus(), 1));
        preference.put("prefer_liked", setting != null && Objects.equals(setting.getRecBefollowus(), 1));
        preference.put("only_liked", setting != null && Objects.equals(setting.getRecOnlyBefollowus(), 1));
        payload.put("preference", preference);

        JSONArray candidateArray = new JSONArray();
        for (AppUserEntity candidate : candidates) {
            if (candidate == null || candidate.getUid() == null) {
                continue;
            }
            JSONObject item = buildUserCard(candidate);
            item.put("height", parseInteger(candidate.getHeight()));
            item.put("education", candidate.getEducation());
            item.put("job", safetyService.sanitizeText(candidate.getJob(), 30));
            item.put("interests", newJsonArray(splitKeywords(candidate.getInterest())));
            item.put("vip", Objects.equals(candidate.getVip(), 1));
            item.put("verified", isVerifiedCandidate(candidate));
            item.put("identity_verified", Objects.equals(candidate.getIdentyCertifStatus(), 1));
            item.put("education_verified", Objects.equals(candidate.getEduCertifStatus(), 1));
            item.put("completeness", calculateProfileCompleteness(candidate));
            item.put("last_active_hours", hoursSince(candidate.getUpdateTime()));
            item.put("liked_by_owner", likedUids != null && likedUids.contains(candidate.getUid()));
            item.put("likes_owner", fanUids != null && fanUids.contains(candidate.getUid()));
            candidateArray.add(item);
        }
        payload.put("candidates", candidateArray);
        return payload;
    }

    private JSONObject buildUserCard(AppUserEntity user) {
        JSONObject card = new JSONObject(true);
        if (user == null) {
            return card;
        }
        card.put("user_id", user.getUid());
        card.put("nickname", StringUtils.defaultIfBlank(user.getUsername(), "用户" + user.getUid()));
        card.put("gender", resolveGender(user.getGender()));
        card.put("city", firstNonBlank(user.getLocationCity(), user.getCity(), user.getAbodeCity()));
        card.put("age", user.getAge());
        card.put("summary", buildUserSummary(user));

        TagProfileAggregateVO aggregate = tagProfileAggregateService.aggregate(user.getUid());
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (aggregate != null) {
            safeAdd(tags, aggregate.getSelfTags());
            safeAdd(tags, aggregate.getBehaviorTags());
            safeAdd(tags, aggregate.getImpressionTop());
        }
        card.put("tags", newJsonArray(new ArrayList<>(tags).subList(0, Math.min(tags.size(), 6))));
        return card;
    }

    private JSONObject buildRelationshipMemory(AppUserEntity me, AppUserEntity target) {
        JSONObject memory = new JSONObject(true);
        boolean isFriend = Boolean.TRUE.equals(friendService.checkIsFriend(me.getUid(), target.getUid()));
        memory.put("stage", isFriend ? "warming" : "early");
        memory.put("known_preferences", extractKnownPreferences(target.getUid()));
        memory.put("taboo_topics", new JSONArray());
        memory.put("recent_promises", new JSONArray());
        memory.put("mood_hint", isFriend ? "有一定熟悉度" : "关系尚浅");
        return memory;
    }

    private JSONObject buildGiftContext(AgentGiftPlanForm form, AppUserEntity target) {
        JSONObject context = new JSONObject(true);
        context.put("post_id", form.getPostId());
        context.put("target_city", firstNonBlank(target.getLocationCity(), target.getCity(), target.getAbodeCity()));
        context.put("target_interest", safetyService.sanitizeText(firstNonBlank(
                target.getInterest(),
                target.getLoveDeclaration(),
                target.getIntro()
        ), 40));
        return context;
    }

    private JSONArray buildConversationMessages(Integer myUid, Integer targetUid, String sessionId) {
        JSONArray turns = new JSONArray();
        List<ChatMessageEntity> messages = Collections.emptyList();
        if (StringUtils.isNotBlank(sessionId)) {
            messages = chatMessageService.lambdaQuery()
                    .eq(ChatMessageEntity::getSessionId, sessionId)
                    .orderByDesc(ChatMessageEntity::getId)
                    .last("LIMIT 12")
                    .list();
        } else if (myUid != null && targetUid != null) {
            String myId = String.valueOf(myUid);
            String otherId = String.valueOf(targetUid);
            messages = chatMessageService.lambdaQuery()
                    .and(wrapper -> wrapper
                            .and(w -> w.eq(ChatMessageEntity::getSenderId, myId).eq(ChatMessageEntity::getReceiverId, otherId))
                            .or(w -> w.eq(ChatMessageEntity::getSenderId, otherId).eq(ChatMessageEntity::getReceiverId, myId)))
                    .orderByDesc(ChatMessageEntity::getId)
                    .last("LIMIT 12")
                    .list();
        }
        Collections.reverse(messages);
        for (ChatMessageEntity message : messages) {
            if (message == null || Objects.equals(message.getIsWithdrawn(), 1)) {
                continue;
            }
            String content = StringUtils.trimToEmpty(message.getContent());
            if (StringUtils.isBlank(content)) {
                continue;
            }
            JSONObject turn = new JSONObject(true);
            turn.put("role", String.valueOf(myUid).equals(StringUtils.trimToEmpty(message.getSenderId())) ? "owner" : "target");
            turn.put("content", safetyService.sanitizeText(content, 300));
            turn.put("timestamp", StringUtils.trimToEmpty(message.getSendTime()));
            turns.add(turn);
        }
        return turns;
    }

    private JSONArray buildRecentMessagesFromForm(List<String> messages) {
        JSONArray turns = new JSONArray();
        if (messages == null) {
            return turns;
        }
        for (String message : messages) {
            String clean = safetyService.sanitizeText(message, 300);
            if (StringUtils.isBlank(clean)) {
                continue;
            }
            JSONObject turn = new JSONObject(true);
            turn.put("role", "system");
            turn.put("content", clean);
            turns.add(turn);
        }
        return turns;
    }

    private JSONArray buildPersonaMessages(Integer userId) {
        String uid = String.valueOf(userId);
        List<ChatMessageEntity> recentMessages = chatMessageService.lambdaQuery()
                .and(wrapper -> wrapper.eq(ChatMessageEntity::getSenderId, uid)
                        .or()
                        .eq(ChatMessageEntity::getReceiverId, uid))
                .orderByDesc(ChatMessageEntity::getId)
                .last("LIMIT 40")
                .list();
        Collections.reverse(recentMessages);
        JSONArray turns = new JSONArray();
        for (ChatMessageEntity message : recentMessages) {
            if (message == null || Objects.equals(message.getIsWithdrawn(), 1)) {
                continue;
            }
            String content = safetyService.sanitizeText(message.getContent(), 300);
            if (StringUtils.isBlank(content)) {
                continue;
            }
            JSONObject turn = new JSONObject(true);
            turn.put("role", uid.equals(StringUtils.trimToEmpty(message.getSenderId())) ? "owner" : "target");
            turn.put("content", content);
            turn.put("timestamp", StringUtils.trimToEmpty(message.getSendTime()));
            turns.add(turn);
        }
        return turns;
    }

    private JSONArray buildBehaviorSignals(Integer userId) {
        JSONArray signals = new JSONArray();
        String uid = String.valueOf(userId);
        List<ChatMessageEntity> recentMessages = chatMessageService.lambdaQuery()
                .and(wrapper -> wrapper.eq(ChatMessageEntity::getSenderId, uid)
                        .or()
                        .eq(ChatMessageEntity::getReceiverId, uid))
                .orderByDesc(ChatMessageEntity::getId)
                .last("LIMIT 200")
                .list();
        int sent = 0;
        int received = 0;
        Set<LocalDate> activeDays = new HashSet<>();
        Set<String> chattedUsers = new HashSet<>();
        for (ChatMessageEntity message : recentMessages) {
            if (message == null || Objects.equals(message.getIsWithdrawn(), 1)) {
                continue;
            }
            boolean selfSent = uid.equals(StringUtils.trimToEmpty(message.getSenderId()));
            if (selfSent) {
                sent++;
                chattedUsers.add(StringUtils.trimToEmpty(message.getReceiverId()));
            } else {
                received++;
                chattedUsers.add(StringUtils.trimToEmpty(message.getSenderId()));
            }
            LocalDate day = resolveDay(message.getSendTime());
            if (day != null) {
                activeDays.add(day);
            }
        }
        signals.add(signal("sent_message_count", sent, "最近消息发送数"));
        signals.add(signal("received_message_count", received, "最近消息接收数"));
        signals.add(signal("active_chat_days", activeDays.size(), "最近活跃聊天日"));
        signals.add(signal("unique_chat_partners", chattedUsers.stream().filter(StringUtils::isNotBlank).count(), "最近聊天对象数"));
        return signals;
    }

    private JSONArray buildRelationshipBehaviorSignals(AppUserEntity owner,
                                                       AppUserEntity target,
                                                       String sessionId,
                                                       JSONArray recentMessages) {
        JSONArray signals = new JSONArray();
        if (recentMessages != null) {
            int ownerCount = 0;
            int targetCount = 0;
            for (int i = 0; i < recentMessages.size(); i++) {
                JSONObject item = recentMessages.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                if (StringUtils.equalsIgnoreCase(item.getString("role"), "owner")) {
                    ownerCount++;
                } else if (StringUtils.equalsIgnoreCase(item.getString("role"), "target")) {
                    targetCount++;
                }
            }
            signals.add(signal("conversation_round_count", Math.min(ownerCount, targetCount), "当前会话往返轮次"));
        }
        int stableChatDays = resolveStableChatDays(owner, target, sessionId);
        signals.add(signal("stable_chat_days", stableChatDays, "连续聊天天数估算"));
        signals.add(signal("positive_reply_ratio", 0.72, "默认正向承接比，后续改为真实计算"));
        signals.add(signal("avg_reply_latency_minutes", 60, "默认回复时延，后续改为真实计算"));
        return signals;
    }

    private int resolveStableChatDays(AppUserEntity owner, AppUserEntity target, String sessionId) {
        if (owner == null || target == null || StringUtils.isBlank(sessionId)) {
            return 0;
        }
        List<ChatMessageEntity> messages = chatMessageService.lambdaQuery()
                .eq(ChatMessageEntity::getSessionId, sessionId)
                .orderByDesc(ChatMessageEntity::getId)
                .last("LIMIT 200")
                .list();
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        Set<LocalDate> days = new HashSet<>();
        for (ChatMessageEntity message : messages) {
            LocalDate day = resolveDay(message == null ? null : message.getSendTime());
            if (day != null) {
                days.add(day);
            }
        }
        return days.size();
    }

    private JSONArray buildRelationshipTags(Integer userId) {
        TagProfileAggregateVO aggregate = tagProfileAggregateService.aggregate(userId);
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (aggregate != null) {
            safeAdd(tags, aggregate.getImpressionTop());
            safeAdd(tags, aggregate.getBehaviorTags());
            safeAdd(tags, aggregate.getSelfTags());
        }
        return newJsonArray(new ArrayList<>(tags).subList(0, Math.min(tags.size(), 8)));
    }

    private JSONArray buildGraphFacts(AppUserEntity owner, AppUserEntity target) {
        List<String> facts = new ArrayList<>();
        if (owner != null && target != null) {
            boolean isFriend = Boolean.TRUE.equals(friendService.checkIsFriend(owner.getUid(), target.getUid()));
            facts.add(isFriend ? "双方已有一定熟悉度" : "双方仍处于陌生或浅关系阶段");
            if (StringUtils.isNotBlank(target.getJob())) {
                facts.add("目标职业是" + target.getJob());
            }
            if (StringUtils.isNotBlank(target.getCity())) {
                facts.add("目标所在城市是" + target.getCity());
            }
        }
        return newJsonArray(facts);
    }

    private JSONObject signal(String name, Object value, String note) {
        JSONObject signal = new JSONObject(true);
        signal.put("name", name);
        signal.put("value", value);
        signal.put("note", note);
        return signal;
    }

    private List<String> extractKnownPreferences(Integer userId) {
        TagProfileAggregateVO aggregate = tagProfileAggregateService.aggregate(userId);
        LinkedHashSet<String> preferences = new LinkedHashSet<>();
        if (aggregate != null) {
            safeAdd(preferences, aggregate.getSelfTags());
            safeAdd(preferences, aggregate.getFollowTopicTags());
        }
        return new ArrayList<>(preferences).subList(0, Math.min(preferences.size(), 4));
    }

    private AgentSuggestionResponseVo toSuggestionResponse(String scene, JSONObject response, int limit) {
        JSONArray suggestions = response.getJSONArray("suggestions");
        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }
        List<AgentSuggestionVo> items = new ArrayList<>();
        for (int i = 0; i < suggestions.size() && items.size() < limit; i++) {
            JSONObject item = suggestions.getJSONObject(i);
            if (item == null || StringUtils.isBlank(item.getString("text"))) {
                continue;
            }
            items.add(AgentSuggestionVo.builder()
                    .suggestionId(safetyService.newSuggestionId())
                    .text(safetyService.sanitizeText(item.getString("text"), 120))
                    .styleTag(StringUtils.defaultIfBlank(item.getString("style"), "sincere"))
                    .riskLevel(StringUtils.defaultIfBlank(item.getString("risk_level"), "low"))
                    .reason(safetyService.sanitizeText(item.getString("rationale"), 120))
                    .nextAction("fill_draft")
                    .build());
        }
        if (items.isEmpty()) {
            return null;
        }
        return AgentSuggestionResponseVo.builder()
                .scene(scene)
                .provider(StringUtils.defaultIfBlank(response.getString("provider"), "agent_runtime"))
                .suggestions(items)
                .build();
    }

    private JSONObject postJson(String path, JSONObject payload) {
        String url = getBaseUrl() + path;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> request = new HttpEntity<>(payload.toJSONString(), headers);
            ResponseEntity<String> response = getAgentRuntimeRestTemplate().postForEntity(url, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || StringUtils.isBlank(response.getBody())) {
                return null;
            }
            return JSON.parseObject(response.getBody());
        } catch (Exception ex) {
            log.warn("[agent-runtime] POST {} failed: {}", url, ex.getMessage());
            return null;
        }
    }

    private RestTemplate getAgentRuntimeRestTemplate() {
        RestTemplate local = agentRuntimeRestTemplate;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (agentRuntimeRestTemplate == null) {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(AGENT_RUNTIME_CONNECT_TIMEOUT_MS);
                factory.setReadTimeout(AGENT_RUNTIME_READ_TIMEOUT_MS);
                agentRuntimeRestTemplate = new RestTemplate(factory);
            }
            return agentRuntimeRestTemplate;
        }
    }

    private String getBaseUrl() {
        String value = StringUtils.trimToEmpty(businessConfigService.getString("agent.runtime.baseUrl"));
        return StringUtils.removeEnd(StringUtils.defaultIfBlank(value, DEFAULT_BASE_URL), "/");
    }

    private boolean isEnabled(String key, boolean defaultValue) {
        try {
            String value = businessConfigService.getString(key);
            if (StringUtils.isBlank(value)) {
                return defaultValue;
            }
            return !"0".equals(value.trim()) && !"false".equalsIgnoreCase(value.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private String resolveGender(Integer gender) {
        if (gender == null) {
            return "unknown";
        }
        if (gender == 1) {
            return "male";
        }
        if (gender == 2) {
            return "female";
        }
        return "unknown";
    }

    private String buildUserSummary(AppUserEntity user) {
        if (user != null && user.getUid() != null && userPersonaSnapshotService != null) {
            try {
                UserPersonaSnapshotEntity snapshot = userPersonaSnapshotService.getLatestByUserId(user.getUid());
                if (snapshot != null && StringUtils.isNotBlank(snapshot.getSummary())) {
                    return safetyService.sanitizeText(snapshot.getSummary(), 160);
                }
            } catch (Exception ex) {
                log.debug("读取画像快照摘要失败, uid={}, err={}", user.getUid(), ex.getMessage());
            }
        }
        List<String> parts = Arrays.asList(
                user.getSelfIntroduction(),
                user.getInterest(),
                user.getLoveDeclaration(),
                user.getIntro()
        );
        return parts.stream()
                .filter(StringUtils::isNotBlank)
                .map(item -> safetyService.sanitizeText(item, 80))
                .limit(3)
                .collect(Collectors.joining("；"));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private void safeAdd(Set<String> target, List<String> values) {
        if (target == null || values == null) {
            return;
        }
        values.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .forEach(target::add);
    }

    private LocalDate resolveDay(String sendTime) {
        if (StringUtils.isBlank(sendTime)) {
            return null;
        }
        String value = sendTime.trim();
        try {
            if (value.matches("^\\d{10,13}$")) {
                long ts = Long.parseLong(value);
                if (value.length() == 10) {
                    ts = ts * 1000L;
                }
                return Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate();
            }
            if (value.length() >= 10) {
                return LocalDate.parse(value.substring(0, 10));
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String findTraitText(JSONArray coreTraits, String name, String fallback) {
        if (coreTraits == null) {
            return fallback;
        }
        for (int i = 0; i < coreTraits.size(); i++) {
            JSONObject trait = coreTraits.getJSONObject(i);
            if (trait == null) {
                continue;
            }
            if (StringUtils.equalsIgnoreCase(name, trait.getString("name"))) {
                Integer score = trait.getInteger("score");
                String reason = trait.getString("reason");
                if (score == null) {
                    return StringUtils.defaultIfBlank(reason, fallback);
                }
                return score + "分 - " + StringUtils.defaultIfBlank(reason, fallback);
            }
        }
        return fallback;
    }

    private JSONArray toJsonArray(JSONArray source) {
        return source == null ? new JSONArray() : source;
    }

    private JSONArray newJsonArray(Collection<?> items) {
        JSONArray array = new JSONArray();
        if (items != null) {
            array.addAll(items);
        }
        return array;
    }

    private JSONArray buildDistillationAnswers(List<AgentDistillationGenerateForm.AnswerItem> answers) {
        JSONArray items = new JSONArray();
        if (answers == null) {
            return items;
        }
        for (AgentDistillationGenerateForm.AnswerItem answer : answers) {
            if (answer == null || StringUtils.isBlank(answer.getAnswerText())) {
                continue;
            }
            JSONObject item = new JSONObject(true);
            item.put("question_code", StringUtils.defaultIfBlank(answer.getQuestionCode(), "custom"));
            item.put("question_label", safetyService.sanitizeText(StringUtils.defaultString(answer.getQuestionLabel()), 80));
            item.put("answer_text", safetyService.sanitizeText(answer.getAnswerText(), 500));
            items.add(item);
        }
        return items;
    }

    private JSONArray buildDistillationMaterials(List<AgentDistillationGenerateForm.MaterialItem> materials) {
        JSONArray items = new JSONArray();
        if (materials == null) {
            return items;
        }
        for (AgentDistillationGenerateForm.MaterialItem material : materials) {
            if (material == null || StringUtils.isBlank(material.getMaterialType())) {
                continue;
            }
            JSONObject item = new JSONObject(true);
            item.put("material_type", material.getMaterialType());
            item.put("label", safetyService.sanitizeText(StringUtils.defaultString(material.getLabel()), 80));
            item.put("content", safetyService.sanitizeText(StringUtils.defaultString(material.getContent()), 600));
            item.put("file_url", StringUtils.defaultString(material.getFileUrl()));
            items.add(item);
        }
        return items;
    }

    private List<Integer> normalizeBudgets(List<Integer> budgets) {
        List<Integer> result = new ArrayList<>();
        if (budgets != null) {
            for (Integer budget : budgets) {
                if (budget != null && budget >= 100 && budget <= 999999) {
                    result.add(budget);
                }
            }
        }
        if (result.isEmpty()) {
            result.addAll(Arrays.asList(1314, 52100, 66600, 168800));
        }
        return result;
    }

    private List<String> toStringList(JSONArray source) {
        if (source == null) {
            return Collections.emptyList();
        }
        return source.toJavaList(String.class);
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return 0D;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return 0D;
        }
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean permissionEnabled(AgentPermissionVo permission) {
        return permission != null && Objects.equals(permission.getEnabled(), 1);
    }

    private boolean isTargetAuthorized(AppUserEntity target,
                                       boolean isFriend,
                                       AgentPermissionVo... permissions) {
        if (target == null || target.getUid() == null) {
            return false;
        }
        if (permissions == null || permissions.length == 0) {
            return isFriend;
        }
        for (AgentPermissionVo permission : permissions) {
            if (!permissionEnabled(permission)) {
                continue;
            }
            String scope = StringUtils.defaultIfBlank(permission.getTargetScope(), AgentGovernanceConstants.TARGET_SCOPE_ALL);
            if (AgentGovernanceConstants.TARGET_SCOPE_ALL.equalsIgnoreCase(scope)) {
                return true;
            }
            if (AgentGovernanceConstants.TARGET_SCOPE_FRIENDS.equalsIgnoreCase(scope) && isFriend) {
                return true;
            }
        }
        return false;
    }

    private List<AgentNextStepActionVo> toNextStepActions(JSONArray source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<AgentNextStepActionVo> result = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            JSONObject item = source.getJSONObject(i);
            if (item == null) {
                continue;
            }
            result.add(AgentNextStepActionVo.builder()
                    .actionType(StringUtils.defaultIfBlank(item.getString("action_type"), "draft"))
                    .capabilityCode(StringUtils.defaultIfBlank(item.getString("capability_code"), "message.draft"))
                    .title(StringUtils.defaultIfBlank(item.getString("title"), "动作建议"))
                    .summary(StringUtils.defaultIfBlank(item.getString("summary"), "建议先人工确认后再执行"))
                    .payloadJson(item.getJSONObject("payload") == null ? "{}" : item.getJSONObject("payload").toJSONString())
                    .riskLevel(StringUtils.defaultIfBlank(item.getString("risk_level"), "low"))
                    .executeMode(StringUtils.defaultIfBlank(item.getString("execute_mode"), "draft_only"))
                    .requiresApproval(Boolean.TRUE.equals(item.getBoolean("requires_approval")))
                    .build());
        }
        return result;
    }

    private boolean isVerifiedCandidate(AppUserEntity user) {
        return Objects.equals(user.getAuditStatus(), 1)
                || Objects.equals(user.getIdentyCertifStatus(), 1)
                || Objects.equals(user.getEduCertifStatus(), 1);
    }

    private int calculateProfileCompleteness(AppUserEntity user) {
        int score = 18;
        if (StringUtils.isNotBlank(user.getAvatar()) || StringUtils.isNotBlank(user.getFigur())) {
            score += 14;
        }
        if (StringUtils.isNotBlank(user.getSelfIntroduction()) || StringUtils.isNotBlank(user.getIntro())) {
            score += 16;
        }
        if (StringUtils.isNotBlank(user.getInterest())) {
            score += 16;
        }
        if (StringUtils.isNotBlank(user.getLoveDeclaration())) {
            score += 12;
        }
        if (StringUtils.isNotBlank(user.getJob())) {
            score += 10;
        }
        if (user.getEducation() != null) {
            score += 8;
        }
        if (user.getAge() != null) {
            score += 6;
        }
        return Math.max(0, Math.min(100, score));
    }

    private long hoursSince(Date value) {
        if (value == null) {
            return Long.MAX_VALUE;
        }
        long diff = System.currentTimeMillis() - value.getTime();
        if (diff <= 0) {
            return 0;
        }
        return diff / (60 * 60 * 1000);
    }

    private Integer parseInteger(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String matched = value.trim().replaceAll("[^0-9-]", "");
        if (StringUtils.isBlank(matched)) {
            return null;
        }
        try {
            return Integer.parseInt(matched);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> splitKeywords(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split("[,，、/\\s]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .limit(8)
                .collect(Collectors.toList());
    }

    private List<String> parseCsv(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    private List<Integer> parseIntegerList(String value) {
        List<Integer> result = new ArrayList<>();
        if (StringUtils.isBlank(value)) {
            return result;
        }
        for (String item : value.split(",")) {
            if (StringUtils.isBlank(item)) {
                continue;
            }
            try {
                result.add(Integer.parseInt(item.trim()));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public String buildRuntimeReportText(Integer userId, JSONObject runtimeReport) {
        StringBuilder builder = new StringBuilder();
        JSONObject personaKernel = runtimeReport == null ? null : runtimeReport.getJSONObject("persona_kernel");
        builder.append("# AI画像报告 V2\n");
        builder.append("- 用户ID：").append(userId).append("\n");
        builder.append("- 生成时间：").append(DATE_TIME_FORMATTER.format(java.time.LocalDateTime.now())).append("\n\n");
        builder.append("## 总结\n");
        builder.append(StringUtils.defaultIfBlank(runtimeReport.getString("summary"), "暂无总结")).append("\n\n");
        builder.append("## 情感与关系\n");
        builder.append("- 情绪风格：").append(StringUtils.defaultIfBlank(runtimeReport.getString("emotional_style"), "暂无")).append("\n");
        builder.append("- 依恋倾向：").append(StringUtils.defaultIfBlank(runtimeReport.getString("attachment_style"), "暂无")).append("\n\n");
        if (personaKernel != null && !personaKernel.isEmpty()) {
            builder.append("## 画像内核\n");
            builder.append("- 表达风格：").append(StringUtils.defaultIfBlank(personaKernel.getString("expression_style"), "暂无")).append("\n");
            builder.append("- 关系风格：").append(StringUtils.defaultIfBlank(personaKernel.getString("relationship_style"), "暂无")).append("\n");
            builder.append("- 爱的语言：").append(String.join("、", toStringList(personaKernel.getJSONArray("love_language")))).append("\n");
            builder.append("- 恋爱节奏：").append(StringUtils.defaultIfBlank(personaKernel.getString("romance_pace"), "暂无")).append("\n");
            builder.append("- 主风格路由：").append(StringUtils.defaultIfBlank(personaKernel.getString("preferred_master_style_code"), "暂无")).append("\n\n");
        }
        builder.append("## 兴趣簇\n");
        builder.append("- ").append(String.join("、", toStringList(runtimeReport.getJSONArray("interest_clusters")))).append("\n\n");
        builder.append("## 风险提示\n");
        builder.append("- ").append(String.join("；", toStringList(runtimeReport.getJSONArray("risk_flags")))).append("\n\n");
        builder.append("## 建议\n");
        builder.append("- ").append(String.join("；", toStringList(runtimeReport.getJSONArray("approach_suggestions")))).append("\n");
        if (personaKernel != null && personaKernel.getJSONArray("taboo_rules") != null) {
            builder.append("\n## 推进雷区\n");
            builder.append("- ").append(String.join("；", toStringList(personaKernel.getJSONArray("taboo_rules")))).append("\n");
        }
        return builder.toString();
    }
}
