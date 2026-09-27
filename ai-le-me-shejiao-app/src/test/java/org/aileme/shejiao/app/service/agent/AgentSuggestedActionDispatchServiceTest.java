package org.aileme.shejiao.app.service.agent;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.param.app.AgentSuggestedActionExecuteForm;
import org.aileme.shejiao.domain.vo.AgentGiftPlanVo;
import org.aileme.shejiao.domain.vo.AgentMessageAutoSendVo;
import org.aileme.shejiao.domain.vo.AgentSuggestedActionExecuteVo;
import org.aileme.shejiao.app.service.impl.SocialIntentService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgentSuggestedActionDispatchServiceTest {

    @Mock
    private AgentGovernanceService agentGovernanceService;

    @Mock
    private AgentOrchestratorService agentOrchestratorService;

    @Mock
    private SocialIntentService socialIntentService;

    private final AgentActionRegistryService agentActionRegistryService = new AgentActionRegistryService();

    @InjectMocks
    private AgentSuggestedActionDispatchService agentSuggestedActionDispatchService;

    @Test
    public void executeRoutesMessageSuggestionToGovernedSend() {
        AppUserEntity currentUser = user(1001);
        AgentSuggestedActionExecuteForm form = new AgentSuggestedActionExecuteForm();
        form.setActionType("message_auto_send");
        form.setTargetUid(2002);
        form.setPayloadJson(JSON.toJSONString(Map.of(
                "content", "刚想到你之前提过的徒步，最近有没有再出去走走？",
                "source", "next_step"
        )));

        AgentMessageAutoSendVo messageVo = AgentMessageAutoSendVo.builder()
                .messageId(8808)
                .targetUid(2002)
                .content("刚想到你之前提过的徒步，最近有没有再出去走走？")
                .taskStatus(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED)
                .approvalRequired(false)
                .build();

        when(agentGovernanceService.executeMessageAutoSendWithGovernance(
                eq(currentUser),
                argThat(item -> item != null
                        && Integer.valueOf(2002).equals(item.getTargetUid())
                        && "刚想到你之前提过的徒步，最近有没有再出去走走？".equals(item.getContent())
                        && "next_step".equals(item.getSource()))))
                .thenReturn(messageVo);

        AgentSuggestedActionExecuteVo response = agentSuggestedActionDispatchService.execute(currentUser, form);

        assertEquals("message_auto_send", response.getActionType());
        assertEquals(AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND, response.getCapabilityCode());
        assertEquals(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED, response.getStatus());
        assertSame(messageVo, response.getMessageAutoSend());
    }

    @Test
    public void executeRoutesGiftPlanSuggestionToPlanner() {
        AppUserEntity currentUser = user(1001);
        AgentSuggestedActionExecuteForm form = new AgentSuggestedActionExecuteForm();
        form.setActionType("gift_plan");
        form.setTargetUid(2002);
        form.setPayloadJson(JSON.toJSONString(Map.of(
                "scene", "social_intent",
                "objective", "break_ice",
                "budgetOptions", List.of(1314, 52100)
        )));

        AgentGiftPlanVo giftPlanVo = AgentGiftPlanVo.builder()
                .scene("social_intent")
                .provider("agent_runtime")
                .relationshipStage("early")
                .strategyNote("先用轻量礼物承接聊天。")
                .gifts(List.of())
                .build();

        when(agentOrchestratorService.giftPlan(
                eq(currentUser),
                argThat(item -> item != null
                        && Integer.valueOf(2002).equals(item.getTargetUid())
                        && "social_intent".equals(item.getScene())
                        && "break_ice".equals(item.getObjective())
                        && item.getBudgetOptions() != null
                        && item.getBudgetOptions().size() == 2
                        && Integer.valueOf(1314).equals(item.getBudgetOptions().get(0)))))
                .thenReturn(giftPlanVo);

        AgentSuggestedActionExecuteVo response = agentSuggestedActionDispatchService.execute(currentUser, form);

        assertEquals("gift_plan", response.getActionType());
        assertEquals(AgentGovernanceConstants.CAPABILITY_GIFT_PLAN, response.getCapabilityCode());
        assertEquals("planned", response.getStatus());
        assertSame(giftPlanVo, response.getGiftPlan());
    }

    @Test
    public void executeThrowsWhenActionTypeUnsupported() {
        AppUserEntity currentUser = user(1001);
        AgentSuggestedActionExecuteForm form = new AgentSuggestedActionExecuteForm();
        form.setActionType("date_coordinate");
        form.setTargetUid(2002);

        LinfengException exception = assertThrows(
                LinfengException.class,
                () -> agentSuggestedActionDispatchService.execute(currentUser, form)
        );

        assertEquals("暂不支持的建议动作类型", exception.getMsg());
        verifyNoInteractions(agentGovernanceService, agentOrchestratorService, socialIntentService);
    }

    @Test
    public void executeRoutesDateInviteSendToGovernedMessageSend() {
        AppUserEntity currentUser = user(1001);
        AgentSuggestedActionExecuteForm form = new AgentSuggestedActionExecuteForm();
        form.setActionType("date_invite_send");
        form.setTargetUid(2002);
        form.setSessionId("session-1");
        form.setPayloadJson(JSON.toJSONString(Map.of(
                "content", "这周末如果你方便，我们找个舒服的地方见一面？",
                "source", "next_step:date_invite_send"
        )));

        AgentMessageAutoSendVo messageVo = AgentMessageAutoSendVo.builder()
                .messageId(9001)
                .targetUid(2002)
                .content("这周末如果你方便，我们找个舒服的地方见一面？")
                .taskStatus(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED)
                .approvalRequired(false)
                .build();

        when(agentGovernanceService.executeMessageAutoSendWithGovernance(
                eq(currentUser),
                argThat(item -> item != null
                        && Integer.valueOf(2002).equals(item.getTargetUid())
                        && "这周末如果你方便，我们找个舒服的地方见一面？".equals(item.getContent())
                        && "next_step:date_invite_send".equals(item.getSource())
                        && "session-1".equals(item.getSessionId()))))
                .thenReturn(messageVo);

        AgentSuggestedActionExecuteVo response = agentSuggestedActionDispatchService.execute(currentUser, form);

        assertEquals("date_invite_send", response.getActionType());
        assertEquals(AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND, response.getCapabilityCode());
        assertEquals(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED, response.getStatus());
        assertSame(messageVo, response.getMessageAutoSend());
    }

    @Test
    public void executeRoutesWechatRequestSendToSocialIntent() {
        AppUserEntity currentUser = user(1001);
        AgentSuggestedActionExecuteForm form = new AgentSuggestedActionExecuteForm();
        form.setActionType("wechat_request_send");
        form.setTargetUid(2002);
        form.setSessionId("session-2");
        form.setPayloadJson(JSON.toJSONString(Map.of(
                "message", "如果你也觉得聊得不错，我们交换个微信？",
                "amount", 520,
                "source", "next_step:wechat_request_send"
        )));

        AgentMessageAutoSendVo messageVo = AgentMessageAutoSendVo.builder()
                .messageId(9002)
                .targetUid(2002)
                .content("如果你也觉得聊得不错，我们交换个微信？")
                .taskStatus(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED)
                .approvalRequired(false)
                .build();

        when(socialIntentService.initiateWechatRequest(
                eq(currentUser),
                eq(2002),
                eq("session-2"),
                eq("如果你也觉得聊得不错，我们交换个微信？"),
                eq(520),
                eq("next_step:wechat_request_send")))
                .thenReturn(messageVo);

        AgentSuggestedActionExecuteVo response = agentSuggestedActionDispatchService.execute(currentUser, form);

        assertEquals("wechat_request_send", response.getActionType());
        assertEquals(AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND, response.getCapabilityCode());
        assertEquals(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED, response.getStatus());
        assertSame(messageVo, response.getMessageAutoSend());
    }

    private AppUserEntity user(int uid) {
        AppUserEntity entity = new AppUserEntity();
        entity.setUid(uid);
        return entity;
    }
}
