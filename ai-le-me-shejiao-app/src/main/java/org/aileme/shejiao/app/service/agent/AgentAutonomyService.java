package org.aileme.shejiao.app.service.agent;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.ChatMessageEntity;
import org.aileme.shejiao.domain.param.app.AgentRelationshipEvaluateForm;
import org.aileme.shejiao.domain.param.app.AgentRelationshipProgramRunForm;
import org.aileme.shejiao.domain.vo.AgentCoachStyleVo;
import org.aileme.shejiao.domain.vo.AgentProgramActionVo;
import org.aileme.shejiao.domain.vo.AgentProgramRunVo;
import org.aileme.shejiao.domain.vo.AgentRelationshipEvaluateVo;
import org.aileme.shejiao.domain.vo.AgentSuggestedActionExecuteVo;

import java.util.Collections;

@Slf4j
@Service
public class AgentAutonomyService {

    private static final String SOCIAL_INTENT_PREFIX = "__SOCIAL_INTENT__:";

    @Autowired(required = false)
    private AgentRuntimeBridgeService agentRuntimeBridgeService;

    @Autowired
    private AppUserService appUserService;

    @Autowired(required = false)
    private AgentSuggestedActionDispatchService agentSuggestedActionDispatchService;

    @Autowired(required = false)
    private AgentAccessPolicyService agentAccessPolicyService;

    @Autowired(required = false)
    private AgentUsageBillingService agentUsageBillingService;

    public AgentRelationshipEvaluateVo evaluate(AppUserEntity owner, AgentRelationshipEvaluateForm form) {
        if (owner == null || form == null || agentRuntimeBridgeService == null) {
            return null;
        }
        AppUserEntity permittedOwner = requireCompanionOwner(owner);
        ensureBudget(permittedOwner, "relationship_evaluate", joinedLength(form.getLastMessages()) + 120);
        AgentRelationshipEvaluateVo response = agentRuntimeBridgeService.evaluateRelationship(
                owner,
                form.getTargetUid(),
                form.getSessionId(),
                form.getLastMessages(),
                null
        );
        settleUsage(permittedOwner, form.getTargetUid(), "relationship_evaluate", joinedLength(form.getLastMessages()) + 120, form.getSessionId());
        return response;
    }

    public AgentCoachStyleVo selectStyle(AppUserEntity owner, AgentRelationshipEvaluateForm form) {
        if (owner == null || form == null || agentRuntimeBridgeService == null) {
            return null;
        }
        AppUserEntity permittedOwner = requireCompanionOwner(owner);
        AgentRelationshipEvaluateVo state = evaluate(permittedOwner, form);
        if (state == null) {
            return null;
        }
        ensureBudget(permittedOwner, "style_select", joinedLength(form.getLastMessages()) + 100);
        AgentCoachStyleVo response = agentRuntimeBridgeService.selectCoachStyle(permittedOwner, form.getTargetUid(), state, null);
        settleUsage(permittedOwner, form.getTargetUid(), "style_select", joinedLength(form.getLastMessages()) + 100, form.getSessionId());
        return response;
    }

    public AgentProgramRunVo runProgram(AppUserEntity owner, AgentRelationshipProgramRunForm form) {
        if (owner == null || form == null || agentRuntimeBridgeService == null) {
            return null;
        }
        AppUserEntity permittedOwner = requireCompanionOwner(owner);
        ensureBudget(permittedOwner, "program_run", joinedLength(form.getLastMessages()) + safeLength(form.getTriggerCode()) + 140);
        AgentProgramRunVo program = agentRuntimeBridgeService.runRelationshipProgram(
                permittedOwner,
                form.getTargetUid(),
                StringUtils.defaultIfBlank(form.getTriggerCode(), "manual_check"),
                form.getSessionId(),
                form.getLastMessages(),
                null,
                buildMatchOpeningSignal(form)
        );
        settleUsage(permittedOwner, form.getTargetUid(), "program_run", joinedLength(form.getLastMessages()) + safeLength(form.getTriggerCode()) + 140, form.getSessionId());
        return maybeAutoExecute(permittedOwner, form.getSessionId(), program, Boolean.TRUE.equals(form.getAutoExecute()));
    }

    public void ingestChatMessageEvent(ChatMessageEntity message) {
        if (message == null || agentRuntimeBridgeService == null) {
            return;
        }
        if (!shouldIngest(message)) {
            return;
        }
        if (!agentRuntimeBridgeService.isCompanionEnabled()) {
            return;
        }
        Integer senderUid = parseUid(message.getSenderId());
        Integer receiverUid = parseUid(message.getReceiverId());
        if (senderUid == null || receiverUid == null || senderUid.equals(receiverUid)) {
            return;
        }
        AppUserEntity sender = appUserService.getById(senderUid);
        AppUserEntity receiver = appUserService.getById(receiverUid);
        if (sender == null || receiver == null) {
            return;
        }
        try {
            receiver = requireCompanionOwner(receiver);
            ensureBudget(receiver, "program_run", safeLength(message.getContent()) + 160);
        } catch (Exception ex) {
            log.info("skip autonomy program, receiverUid={}, sessionId={}, reason={}",
                    receiverUid, message.getSessionId(), ex.getMessage());
            return;
        }
        JSONObject payload = buildMessagePayload(message);
        agentRuntimeBridgeService.ingestRelationshipEvent(
                receiver,
                sender,
                "incoming_message",
                message.getSendTime(),
                payload
        );
        agentRuntimeBridgeService.ingestRelationshipEvent(
                sender,
                receiver,
                "outgoing_message",
                message.getSendTime(),
                payload
        );
        AgentProgramRunVo program = agentRuntimeBridgeService.runRelationshipProgram(
                receiver,
                senderUid,
                "incoming_message",
                message.getSessionId(),
                null,
                null,
                null
        );
        settleUsage(receiver, senderUid, "program_run", safeLength(message.getContent()) + 160, message.getSessionId());
        maybeAutoExecute(receiver, message.getSessionId(), program, true);
    }

    private AgentProgramRunVo maybeAutoExecute(AppUserEntity owner,
                                               String sessionId,
                                               AgentProgramRunVo program,
                                               boolean autoExecute) {
        if (program == null) {
            return null;
        }
        if (!autoExecute) {
            return program;
        }
        AgentProgramActionVo action = program.getNextAction();
        if (!shouldAutoExecute(action) || agentSuggestedActionDispatchService == null) {
            return copyProgramWithExecution(program, false, null, null);
        }
        try {
            AgentSuggestedActionExecuteVo execution = agentSuggestedActionDispatchService.executeProgramAction(owner, sessionId, action);
            return copyProgramWithExecution(program, true, execution, null);
        } catch (Exception ex) {
            log.warn("auto execute program action failed, ownerUid={}, sessionId={}, actionType={}, message={}",
                    owner == null ? null : owner.getUid(),
                    sessionId,
                    action == null ? null : action.getActionType(),
                    ex.getMessage());
            return copyProgramWithExecution(program, false, null, ex.getMessage());
        }
    }

    private boolean shouldAutoExecute(AgentProgramActionVo action) {
        return action != null && StringUtils.equalsIgnoreCase(action.getExecuteMode(), "auto_if_permitted");
    }

    private AgentProgramRunVo copyProgramWithExecution(AgentProgramRunVo program,
                                                       boolean autoExecuted,
                                                       AgentSuggestedActionExecuteVo execution,
                                                       String executionErrorMessage) {
        return AgentProgramRunVo.builder()
                .workflowCode(program.getWorkflowCode())
                .stageCode(program.getStageCode())
                .masterStyleCode(program.getMasterStyleCode())
                .nextAction(program.getNextAction())
                .autoExecuted(autoExecuted)
                .execution(execution)
                .executionErrorMessage(StringUtils.trimToNull(executionErrorMessage))
                .next24hPlan(program.getNext24hPlan())
                .reasoningSummary(program.getReasoningSummary())
                .build();
    }

    private boolean shouldIngest(ChatMessageEntity message) {
        if (message == null) {
            return false;
        }
        if (StringUtils.isBlank(message.getContent()) || !StringUtils.equalsIgnoreCase(message.getMessageType(), "text")) {
            return false;
        }
        return !StringUtils.startsWith(message.getContent(), SOCIAL_INTENT_PREFIX);
    }

    private JSONObject buildMatchOpeningSignal(AgentRelationshipProgramRunForm form) {
        boolean hasStructuredInput = form.getMatchScore() != null
                || form.getOpeningReadinessScore() != null
                || (form.getFitTags() != null && !form.getFitTags().isEmpty())
                || (form.getIcebreakOpeners() != null && !form.getIcebreakOpeners().isEmpty())
                || StringUtils.isNotBlank(form.getRecommendedAction())
                || StringUtils.isNotBlank(form.getDoNotOpenReason());
        if (!hasStructuredInput) {
            return null;
        }
        JSONObject signal = new JSONObject(true);
        signal.put("match_score", form.getMatchScore() == null ? 0 : form.getMatchScore());
        signal.put("opening_readiness_score", form.getOpeningReadinessScore() == null ? 0D : form.getOpeningReadinessScore());
        signal.put("fit_tags", form.getFitTags() == null ? Collections.emptyList() : form.getFitTags());
        signal.put("icebreak_openers", form.getIcebreakOpeners() == null ? Collections.emptyList() : form.getIcebreakOpeners());
        signal.put("recommended_action", StringUtils.defaultIfBlank(form.getRecommendedAction(), "open_chat"));
        if (StringUtils.isNotBlank(form.getDoNotOpenReason())) {
            signal.put("do_not_open_reason", form.getDoNotOpenReason());
        }
        return signal;
    }

    private JSONObject buildMessagePayload(ChatMessageEntity message) {
        JSONObject payload = new JSONObject(true);
        payload.put("session_id", message.getSessionId());
        payload.put("message_type", message.getMessageType());
        payload.put("sender_id", message.getSenderId());
        payload.put("receiver_id", message.getReceiverId());
        payload.put("content", message.getContent());
        payload.put("message_id", message.getId());
        return payload;
    }

    private Integer parseUid(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private AppUserEntity requireCompanionOwner(AppUserEntity owner) {
        if (agentAccessPolicyService == null) {
            return owner;
        }
        return agentAccessPolicyService.requireCompanionAccess(owner);
    }

    private void ensureBudget(AppUserEntity owner, String sceneCode, int inputChars) {
        if (agentUsageBillingService == null || owner == null) {
            return;
        }
        agentUsageBillingService.ensureSufficientBudget(owner, sceneCode, inputChars);
    }

    private void settleUsage(AppUserEntity owner, Integer targetUid, String sceneCode, int inputChars, String referenceId) {
        if (agentUsageBillingService == null || owner == null) {
            return;
        }
        agentUsageBillingService.settleUsage(owner, targetUid, sceneCode, inputChars, referenceId);
    }

    private int joinedLength(java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return values.stream().filter(java.util.Objects::nonNull).mapToInt(String::length).sum();
    }

    private int safeLength(String value) {
        return StringUtils.length(StringUtils.defaultString(value));
    }
}
