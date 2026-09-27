package org.aileme.shejiao.app.service.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.app.service.impl.SocialIntentService;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.param.app.AgentGiftPlanForm;
import org.aileme.shejiao.domain.param.app.AgentMessageAutoSendForm;
import org.aileme.shejiao.domain.param.app.AgentSuggestedActionExecuteForm;
import org.aileme.shejiao.domain.vo.AgentGiftPlanVo;
import org.aileme.shejiao.domain.vo.AgentMessageAutoSendVo;
import org.aileme.shejiao.domain.vo.AgentProgramActionVo;
import org.aileme.shejiao.domain.vo.AgentSuggestedActionExecuteVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AgentSuggestedActionDispatchService {

    @Autowired
    private AgentActionRegistryService agentActionRegistryService;

    @Autowired
    private AgentGovernanceService agentGovernanceService;

    @Autowired
    private AgentOrchestratorService agentOrchestratorService;

    @Autowired
    private SocialIntentService socialIntentService;

    public AgentSuggestedActionExecuteVo execute(AppUserEntity currentUser, AgentSuggestedActionExecuteForm form) {
        String actionType = normalizeActionType(form == null ? null : form.getActionType());
        AgentActionRegistryService.ActionDescriptor descriptor = agentActionRegistryService.resolve(actionType);
        if (descriptor == null || !descriptor.userExecutable()) {
            throw new LinfengException("暂不支持的建议动作类型");
        }
        JSONObject payload = parsePayload(form == null ? null : form.getPayloadJson());
        if (descriptor.executorType() == AgentActionRegistryService.ExecutorType.GOVERNED_MESSAGE_SEND) {
            return executeGovernedTextAction(currentUser, form, payload, descriptor);
        }
        if (descriptor.executorType() == AgentActionRegistryService.ExecutorType.GIFT_PLAN) {
            return executeGiftPlan(currentUser, form, payload, descriptor);
        }
        if (descriptor.executorType() == AgentActionRegistryService.ExecutorType.WECHAT_REQUEST) {
            return executeWechatRequestAction(currentUser, form, payload, descriptor);
        }
        throw new LinfengException("暂不支持的建议动作类型");
    }

    public AgentSuggestedActionExecuteVo executeProgramAction(AppUserEntity currentUser,
                                                             String sessionId,
                                                             AgentProgramActionVo action) {
        if (action == null) {
            throw new LinfengException("自治动作不存在");
        }
        String actionType = normalizeActionType(action.getActionType());
        if ("reply_send".equals(actionType)
                || "proactive_opening_send".equals(actionType)
                || "hongniang_opening_send".equals(actionType)
                || "date_invite_send".equals(actionType)) {
            return executeProgramTextSend(currentUser, sessionId, action, actionType);
        }
        if ("wechat_request_send".equals(actionType)) {
            return executeWechatRequestSend(currentUser, sessionId, action);
        }
        throw new LinfengException("暂不支持的自治动作类型");
    }

    private AgentSuggestedActionExecuteVo executeGovernedTextAction(AppUserEntity currentUser,
                                                                   AgentSuggestedActionExecuteForm form,
                                                                   JSONObject payload,
                                                                   AgentActionRegistryService.ActionDescriptor descriptor) {
        String content = StringUtils.trimToEmpty(firstNotBlank(payload.getString("content"), payload.getString("message")));
        if (StringUtils.isBlank(content)) {
            throw new LinfengException("建议消息内容不能为空");
        }
        AgentMessageAutoSendForm messageForm = new AgentMessageAutoSendForm();
        messageForm.setTargetUid(form.getTargetUid());
        messageForm.setSessionId(firstNotBlank(payload.getString("sessionId"), form.getSessionId()));
        messageForm.setContent(content);
        messageForm.setSource(firstNotBlank(payload.getString("source"), "next_step:" + descriptor.actionType()));

        AgentMessageAutoSendVo messageVo = agentGovernanceService.executeMessageAutoSendWithGovernance(currentUser, messageForm);
        return AgentSuggestedActionExecuteVo.builder()
                .actionType(descriptor.actionType())
                .capabilityCode(descriptor.capabilityCode())
                .status(StringUtils.defaultIfBlank(messageVo == null ? null : messageVo.getTaskStatus(), AgentGovernanceConstants.TASK_STATUS_SUCCEEDED))
                .approvalRequired(messageVo != null && Boolean.TRUE.equals(messageVo.getApprovalRequired()))
                .messageAutoSend(messageVo)
                .build();
    }

    private AgentSuggestedActionExecuteVo executeGiftPlan(AppUserEntity currentUser,
                                                          AgentSuggestedActionExecuteForm form,
                                                          JSONObject payload,
                                                          AgentActionRegistryService.ActionDescriptor descriptor) {
        AgentGiftPlanForm giftPlanForm = new AgentGiftPlanForm();
        giftPlanForm.setTargetUid(form.getTargetUid());
        giftPlanForm.setScene(firstNotBlank(payload.getString("scene"), "social_intent"));
        giftPlanForm.setObjective(firstNotBlank(payload.getString("objective"), "break_ice"));
        giftPlanForm.setSessionId(firstNotBlank(payload.getString("sessionId"), form.getSessionId()));
        giftPlanForm.setPostId(payload.getInteger("postId"));

        List<Integer> budgetOptions = parseBudgetOptions(payload.getJSONArray("budgetOptions"));
        if (!budgetOptions.isEmpty()) {
            giftPlanForm.setBudgetOptions(budgetOptions);
        }

        AgentGiftPlanVo giftPlanVo = agentOrchestratorService.giftPlan(currentUser, giftPlanForm);
        return AgentSuggestedActionExecuteVo.builder()
                .actionType(descriptor.actionType())
                .capabilityCode(descriptor.capabilityCode())
                .status("planned")
                .approvalRequired(false)
                .giftPlan(giftPlanVo)
                .build();
    }

    private AgentSuggestedActionExecuteVo executeWechatRequestAction(AppUserEntity currentUser,
                                                                    AgentSuggestedActionExecuteForm form,
                                                                    JSONObject payload,
                                                                    AgentActionRegistryService.ActionDescriptor descriptor) {
        AgentMessageAutoSendVo messageVo = socialIntentService.initiateWechatRequest(
                currentUser,
                form.getTargetUid(),
                firstNotBlank(payload.getString("sessionId"), form.getSessionId()),
                firstNotBlank(payload.getString("message"), payload.getString("content")),
                payload.getInteger("amount"),
                firstNotBlank(payload.getString("source"), "next_step:" + descriptor.actionType())
        );
        return AgentSuggestedActionExecuteVo.builder()
                .actionType(descriptor.actionType())
                .capabilityCode(descriptor.capabilityCode())
                .status(StringUtils.defaultIfBlank(messageVo == null ? null : messageVo.getTaskStatus(), AgentGovernanceConstants.TASK_STATUS_SUCCEEDED))
                .approvalRequired(false)
                .messageAutoSend(messageVo)
                .build();
    }

    private AgentSuggestedActionExecuteVo executeProgramTextSend(AppUserEntity currentUser,
                                                                String sessionId,
                                                                AgentProgramActionVo action,
                                                                String actionType) {
        JSONObject payload = parsePayload(action.getPayloadJson());
        payload.put("content", StringUtils.trimToEmpty(action.getContent()));
        payload.put("sessionId", firstNotBlank(payload.getString("sessionId"), sessionId));
        payload.put("source", firstNotBlank(payload.getString("source"), "relationship_program:" + actionType));

        AgentSuggestedActionExecuteForm form = new AgentSuggestedActionExecuteForm();
        form.setActionType("message_auto_send");
        form.setTargetUid(action.getTargetUserId());
        form.setSessionId(sessionId);
        form.setPayloadJson(payload.toJSONString());
        AgentActionRegistryService.ActionDescriptor descriptor = agentActionRegistryService.resolve("message_auto_send");
        return executeGovernedTextAction(currentUser, form, payload, descriptor);
    }

    private AgentSuggestedActionExecuteVo executeWechatRequestSend(AppUserEntity currentUser,
                                                                  String sessionId,
                                                                  AgentProgramActionVo action) {
        JSONObject payload = parsePayload(action.getPayloadJson());
        AgentMessageAutoSendVo messageVo = socialIntentService.initiateWechatRequest(
                currentUser,
                action.getTargetUserId(),
                firstNotBlank(payload.getString("sessionId"), sessionId),
                firstNotBlank(payload.getString("message"), action.getContent()),
                payload.getInteger("amount"),
                firstNotBlank(payload.getString("source"), "relationship_program:wechat_request_send")
        );
        return AgentSuggestedActionExecuteVo.builder()
                .actionType("wechat_request_send")
                .capabilityCode(AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND)
                .status(StringUtils.defaultIfBlank(messageVo == null ? null : messageVo.getTaskStatus(), AgentGovernanceConstants.TASK_STATUS_SUCCEEDED))
                .approvalRequired(false)
                .messageAutoSend(messageVo)
                .build();
    }

    private JSONObject parsePayload(String payloadJson) {
        if (StringUtils.isBlank(payloadJson)) {
            return new JSONObject(true);
        }
        try {
            JSONObject payload = JSON.parseObject(payloadJson);
            return payload == null ? new JSONObject(true) : payload;
        } catch (Exception ex) {
            throw new LinfengException("建议动作载荷格式不正确");
        }
    }

    private List<Integer> parseBudgetOptions(JSONArray values) {
        if (values == null || values.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            Integer value = values.getInteger(i);
            if (value != null && value > 0) {
                result.add(value);
            }
        }
        return result;
    }

    private String normalizeActionType(String value) {
        return StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
    }

    private String firstNotBlank(String first, String fallback) {
        return StringUtils.isNotBlank(first) ? first.trim() : StringUtils.trimToNull(fallback);
    }
}
