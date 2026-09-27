package org.aileme.shejiao.app.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.FriendService;
import org.aileme.shejiao.api.service.UserPersonaSnapshotService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.UserPersonaSnapshotEntity;
import org.aileme.shejiao.domain.param.app.*;
import org.aileme.shejiao.domain.vo.AgentGiftExecuteVo;
import org.aileme.shejiao.domain.vo.AgentGiftPlanVo;
import org.aileme.shejiao.domain.vo.AgentNextStepActionVo;
import org.aileme.shejiao.domain.vo.AgentSuggestionResponseVo;
import org.aileme.shejiao.domain.vo.AgentSuggestionVo;
import org.aileme.shejiao.gateway.chat.ChatModelGatewayService;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.util.*;

/**
 * Agent 编排服务
 */
@Slf4j
@Service
public class AgentOrchestratorService {

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserPersonaSnapshotService userPersonaSnapshotService;

    @Autowired
    private AgentSafetyService safetyService;

    @Autowired
    private IcebreakService icebreakService;

    @Autowired
    private ReplyAssistService replyAssistService;

    @Autowired
    private ProfilePolishService profilePolishService;

    @Autowired
    private VideoScriptService videoScriptService;

    @Autowired
    private GiftPlanService giftPlanService;

    @Autowired
    private GiftExecuteService giftExecuteService;

    @Autowired
    private AgentGovernanceService agentGovernanceService;

    @Autowired(required = false)
    private AgentRuntimeBridgeService agentRuntimeBridgeService;

    @Autowired(required = false)
    private ThirdPartyRouteConfigService routeConfigService;

    @Autowired(required = false)
    private ChatModelGatewayService chatModelGatewayService;

    @Autowired(required = false)
    private AgentAccessPolicyService agentAccessPolicyService;

    @Autowired(required = false)
    private AgentUsageBillingService agentUsageBillingService;

    public AgentSuggestionResponseVo icebreak(AppUserEntity me, AgentIcebreakForm form) {
        AppUserEntity owner = requireCompanionOwner(me);
        AgentSuggestionResponseVo runtimeResponse = agentRuntimeBridgeService == null ? null
                : tryRuntime(() -> agentRuntimeBridgeService.suggestIcebreak(owner, form), owner, "icebreak_suggest", estimateIcebreakInput(form));
        if (runtimeResponse != null && runtimeResponse.getSuggestions() != null && !runtimeResponse.getSuggestions().isEmpty()) {
            List<AgentSuggestionVo> suggestions = safetyService.enforcePolicy(runtimeResponse.getSuggestions(), false);
            if (!suggestions.isEmpty()) {
                return AgentSuggestionResponseVo.builder()
                        .scene(runtimeResponse.getScene())
                        .provider(runtimeResponse.getProvider())
                        .suggestions(suggestions)
                        .build();
            }
        }

        AppUserEntity target = requireUser(form.getTargetUid());
        boolean isFriend = friendService.checkIsFriend(me.getUid(), target.getUid());
        String personaSummary = getPersonaSummary(target.getUid());

        List<AgentSuggestionVo> suggestions = icebreakService.suggest(
                me,
                target,
                defaultScene(form.getScene(), "same_city"),
                Math.max(1, Math.min(5, defaultInt(form.getLimit(), 3))),
                isFriend,
                personaSummary
        );

        suggestions = safetyService.enforcePolicy(suggestions, isFriend);
        if (suggestions.isEmpty()) {
            suggestions = safetyService.fallbackSuggestions(form.getScene());
        }

        return AgentSuggestionResponseVo.builder()
                .scene(defaultScene(form.getScene(), "same_city"))
                .provider(currentProvider())
                .suggestions(suggestions)
                .build();
    }

    public AgentSuggestionResponseVo reply(AppUserEntity me, AgentReplyForm form) {
        AppUserEntity owner = requireCompanionOwner(me);
        AgentSuggestionResponseVo runtimeResponse = agentRuntimeBridgeService == null ? null
                : tryRuntime(() -> agentRuntimeBridgeService.suggestReply(owner, form), owner, "reply_suggest", estimateReplyInput(form));
        if (runtimeResponse != null && runtimeResponse.getSuggestions() != null && !runtimeResponse.getSuggestions().isEmpty()) {
            boolean isFriend = false;
            try {
                isFriend = friendService.checkIsFriend(owner.getUid(), form.getTargetUid());
            } catch (Exception ignored) {
            }
            List<AgentSuggestionVo> suggestions = safetyService.enforcePolicy(runtimeResponse.getSuggestions(), isFriend);
            if (!suggestions.isEmpty()) {
                return AgentSuggestionResponseVo.builder()
                        .scene(runtimeResponse.getScene())
                        .provider(runtimeResponse.getProvider())
                        .suggestions(suggestions)
                        .build();
            }
        }

        AppUserEntity target = requireUser(form.getTargetUid());
        boolean isFriend = friendService.checkIsFriend(me.getUid(), target.getUid());

        List<AgentSuggestionVo> suggestions = replyAssistService.suggest(
                me,
                target,
                form.getLastMessages(),
                StringUtils.defaultIfBlank(form.getTone(), "sincere"),
                isFriend
        );

        suggestions = safetyService.enforcePolicy(suggestions, isFriend);
        if (suggestions.isEmpty()) {
            suggestions = safetyService.fallbackSuggestions("reply");
        }

        return AgentSuggestionResponseVo.builder()
                .scene("reply")
                .provider(currentProvider())
                .suggestions(suggestions)
                .build();
    }

    public AgentSuggestionResponseVo polish(AppUserEntity me, AgentProfilePolishForm form) {
        String source = safetyService.sanitizeText(form.getText(), 300);
        if (StringUtils.isBlank(source)) {
            throw new LinfengException("文案不能为空");
        }

        List<AgentSuggestionVo> suggestions = profilePolishService.polish(source, form.getStyle());
        suggestions = safetyService.enforcePolicy(suggestions, true);
        if (suggestions.isEmpty()) {
            suggestions = safetyService.fallbackSuggestions("profile_polish");
        }

        return AgentSuggestionResponseVo.builder()
                .scene("profile_polish")
                .provider(currentProvider())
                .suggestions(suggestions)
                .build();
    }

    public AgentSuggestionResponseVo videoScript(AppUserEntity me, AgentVideoScriptForm form) {
        AppUserEntity target = requireUser(form.getTargetUid());
        String personaSummary = getPersonaSummary(target.getUid());

        List<AgentSuggestionVo> suggestions = videoScriptService.suggest(
                me,
                target,
                StringUtils.defaultIfBlank(form.getTemplateCode(), "self_intro"),
                personaSummary
        );

        suggestions = safetyService.enforcePolicy(suggestions, true);
        if (suggestions.isEmpty()) {
            suggestions = safetyService.fallbackSuggestions("video_script");
        }

        return AgentSuggestionResponseVo.builder()
                .scene("video_script")
                .provider(currentProvider())
                .suggestions(suggestions)
                .build();
    }

    public AgentGiftPlanVo giftPlan(AppUserEntity me, AgentGiftPlanForm form) {
        AppUserEntity owner = requireCompanionOwner(me);
        AgentGiftPlanVo runtimeResponse = agentRuntimeBridgeService == null ? null
                : tryRuntime(() -> agentRuntimeBridgeService.suggestGiftPlan(owner, form), owner, "gift_plan", estimateGiftPlanInput(form));
        if (runtimeResponse != null && runtimeResponse.getGifts() != null && !runtimeResponse.getGifts().isEmpty()) {
            return runtimeResponse;
        }

        AppUserEntity target = requireUser(form.getTargetUid());
        boolean isFriend = friendService.checkIsFriend(owner.getUid(), target.getUid());
        String personaSummary = getPersonaSummary(target.getUid());
        return giftPlanService.suggest(owner, target, form, isFriend, personaSummary);
    }

    public AgentGiftPlanVo giftCatalog(AppUserEntity me) {
        requireCompanionOwner(me);
        return giftPlanService.catalog();
    }

    public AgentGiftExecuteVo executeGift(AppUserEntity me, AgentGiftExecuteForm form) {
        return agentGovernanceService.executeGiftWithGovernance(me, form);
    }

    public AgentGiftExecuteVo getGiftTask(AppUserEntity me, Long taskId) {
        return giftExecuteService.getTaskDetail(me, taskId);
    }

    public org.aileme.shejiao.domain.vo.AgentNextStepVo nextStep(AppUserEntity me, AgentNextStepForm form) {
        AppUserEntity owner = requireCompanionOwner(me);
        org.aileme.shejiao.domain.vo.AgentNextStepVo runtime = agentRuntimeBridgeService == null ? null
                : tryRuntime(() -> agentRuntimeBridgeService.suggestNextStep(owner, form), owner, "next_step", estimateNextStepInput(form));
        if (runtime != null) {
            return runtime;
        }
        return org.aileme.shejiao.domain.vo.AgentNextStepVo.builder()
                .provider(currentProvider())
                .relationshipStage("early")
                .heatScore(0.24)
                .trustScore(0.28)
                .toneWarmth(0.35)
                .intimacyScore(0.22)
                .progressionScore(0.18)
                .dateReadyScore(0.16)
                .wechatReadyScore(0.12)
                .riskScore(0.22)
                .nextBestAction("先发送一条低压力、可延续的话题消息。")
                .next24hPlan(Arrays.asList(
                        "先围绕最近一个自然话题继续聊。",
                        "根据对方反馈决定是否再推进一步。",
                        "如果对方长时间未回复，不连续追问。"
                ))
                .guardrails(Arrays.asList(
                        "不要直接自动线下邀约。",
                        "不要在低样本情况下过度推进。",
                        "涉及高风险动作先暂停自动执行。"
                ))
                .suggestedActions(Arrays.asList(
                        AgentNextStepActionVo.builder()
                                .actionType("message_auto_send")
                                .capabilityCode(AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND)
                                .title("发一条轻压力消息")
                                .summary("先用低压力话题继续互动，适合关系早期。")
                                .payloadJson("{\"source\":\"next_step\"}")
                                .riskLevel(AgentGovernanceConstants.RISK_LEVEL_LOW)
                                .executeMode("auto_if_permitted")
                                .requiresApproval(false)
                                .build(),
                        AgentNextStepActionVo.builder()
                                .actionType("gift_plan")
                                .capabilityCode(AgentGovernanceConstants.CAPABILITY_GIFT_PLAN)
                                .title("先看礼物策划")
                                .summary("如果聊天氛围顺畅，再考虑轻量礼物策划。")
                                .payloadJson("{\"objective\":\"break_ice\"}")
                                .riskLevel(AgentGovernanceConstants.RISK_LEVEL_LOW)
                                .executeMode("draft_only")
                                .requiresApproval(false)
                                .build()
                ))
                .shouldAutoExecute(false)
                .build();
    }

    public Map<String, Object> feedback(AppUserEntity me, AgentFeedbackForm form) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> result = new HashMap<>();
        result.put("traceId", traceId);
        result.put("accepted", true);

        log.info("[agent-feedback] uid={}, scene={}, suggestionId={}, action={}, reason={}, traceId={}",
                me.getUid(),
                safetyService.sanitizeText(form.getScene(), 40),
                safetyService.sanitizeText(form.getSuggestionId(), 64),
                safetyService.sanitizeText(form.getAction(), 20),
                safetyService.sanitizeText(form.getReason(), 120),
                traceId
        );

        return result;
    }

    private AppUserEntity requireUser(Integer uid) {
        AppUserEntity user = appUserService.getById(uid);
        if (user == null) {
            throw new LinfengException("目标用户不存在");
        }
        return user;
    }

    private String getPersonaSummary(Integer uid) {
        UserPersonaSnapshotEntity snapshot = userPersonaSnapshotService.getLatestByUserId(uid);
        if (snapshot == null) {
            return "";
        }
        return StringUtils.defaultIfBlank(snapshot.getSummary(), "");
    }

    private String currentProvider() {
        try {
            if (chatModelGatewayService != null) {
                String routed = chatModelGatewayService.currentProviderCode();
                if (StringUtils.isNotBlank(routed)) {
                    return routed;
                }
            }
            if (routeConfigService != null) {
                String routed = routeConfigService.getCurrentProviderCode("ai", "openai_codex");
                if (StringUtils.isNotBlank(routed)) {
                    return routed;
                }
            }
        } catch (Exception ex) {
            log.warn("[agent] read provider failed: {}", ex.getMessage());
        }
        return "openai_codex";
    }

    private String defaultScene(String scene, String fallback) {
        return StringUtils.defaultIfBlank(scene, fallback);
    }

    private int defaultInt(Integer val, int fallback) {
        return val == null ? fallback : val;
    }

    private AppUserEntity requireCompanionOwner(AppUserEntity me) {
        if (agentAccessPolicyService == null) {
            return me;
        }
        return agentAccessPolicyService.requireCompanionAccess(me);
    }

    private <T> T tryRuntime(RuntimeCall<T> runtimeCall,
                             AppUserEntity owner,
                             String sceneCode,
                             int inputChars) {
        if (runtimeCall == null) {
            return null;
        }
        try {
            if (agentUsageBillingService != null) {
                agentUsageBillingService.ensureSufficientBudget(owner, sceneCode, inputChars);
            }
            T result = runtimeCall.call();
            if (result != null && agentUsageBillingService != null) {
                agentUsageBillingService.settleUsage(owner, null, sceneCode, inputChars, sceneCode);
            }
            return result;
        } catch (LinfengException ex) {
            log.info("[agent] runtime skipped, scene={}, uid={}, reason={}", sceneCode, owner == null ? null : owner.getUid(), ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("[agent] runtime call failed, scene={}, uid={}, reason={}", sceneCode, owner == null ? null : owner.getUid(), ex.getMessage());
            return null;
        }
    }

    private int estimateIcebreakInput(AgentIcebreakForm form) {
        return safeLength(form == null ? null : form.getScene()) + 120;
    }

    private int estimateReplyInput(AgentReplyForm form) {
        return safeLength(form == null ? null : form.getTone()) + joinedLength(form == null ? null : form.getLastMessages()) + 120;
    }

    private int estimateGiftPlanInput(AgentGiftPlanForm form) {
        return safeLength(form == null ? null : form.getScene())
                + safeLength(form == null ? null : form.getObjective())
                + 160;
    }

    private int estimateNextStepInput(AgentNextStepForm form) {
        return safeLength(form == null ? null : form.getObjective()) + joinedLength(form == null ? null : form.getLastMessages()) + 180;
    }

    private int joinedLength(List<String> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return values.stream().filter(Objects::nonNull).mapToInt(String::length).sum();
    }

    private int safeLength(String value) {
        return StringUtils.length(StringUtils.defaultString(value));
    }

    @FunctionalInterface
    private interface RuntimeCall<T> {
        T call();
    }
}
