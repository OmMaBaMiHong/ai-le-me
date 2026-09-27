package org.aileme.shejiao.app.service.agent;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class AgentActionRegistryService {

    public enum ExecutorType {
        GOVERNED_MESSAGE_SEND,
        GIFT_PLAN,
        WECHAT_REQUEST
    }

    public record ActionDescriptor(
            String actionType,
            String capabilityCode,
            ExecutorType executorType,
            boolean userExecutable
    ) {
    }

    private final Map<String, ActionDescriptor> actions = new LinkedHashMap<>();

    public AgentActionRegistryService() {
        register("message_auto_send", AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND, ExecutorType.GOVERNED_MESSAGE_SEND, true);
        register("date_invite_send", AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND, ExecutorType.GOVERNED_MESSAGE_SEND, true);
        register("wechat_request_send", AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND, ExecutorType.WECHAT_REQUEST, true);
        register("gift_plan", AgentGovernanceConstants.CAPABILITY_GIFT_PLAN, ExecutorType.GIFT_PLAN, true);
        register("moment_draft_create", AgentGovernanceConstants.CAPABILITY_MOMENT_DRAFT, null, false);
    }

    public ActionDescriptor resolve(String actionType) {
        if (StringUtils.isBlank(actionType)) {
            return null;
        }
        return actions.get(actionType.trim().toLowerCase(Locale.ROOT));
    }

    private void register(String actionType,
                          String capabilityCode,
                          ExecutorType executorType,
                          boolean userExecutable) {
        actions.put(actionType, new ActionDescriptor(actionType, capabilityCode, executorType, userExecutable));
    }
}
