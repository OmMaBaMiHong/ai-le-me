package org.aileme.shejiao.app.service.agent;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgentAutonomyServiceTest {

    @Mock
    private AgentRuntimeBridgeService agentRuntimeBridgeService;

    @Mock
    private AppUserService appUserService;

    @Mock
    private AgentSuggestedActionDispatchService agentSuggestedActionDispatchService;

    @InjectMocks
    private AgentAutonomyService agentAutonomyService;

    @Test
    public void evaluateDelegatesToRuntimeBridge() {
        AppUserEntity owner = user(1001);
        AgentRelationshipEvaluateForm form = new AgentRelationshipEvaluateForm();
        form.setTargetUid(2002);
        form.setSessionId("90001");
        form.setLastMessages(List.of("你好", "周末有安排吗"));

        AgentRelationshipEvaluateVo expected = AgentRelationshipEvaluateVo.builder()
                .stageCode("warming")
                .build();

        when(agentRuntimeBridgeService.evaluateRelationship(owner, 2002, "90001", form.getLastMessages(), null))
                .thenReturn(expected);

        AgentRelationshipEvaluateVo actual = agentAutonomyService.evaluate(owner, form);

        assertSame(expected, actual);
    }

    @Test
    public void selectStyleEvaluatesRelationshipBeforeChoosingCoachStyle() {
        AppUserEntity owner = user(1001);
        AgentRelationshipEvaluateForm form = new AgentRelationshipEvaluateForm();
        form.setTargetUid(2002);
        form.setSessionId("90001");
        form.setLastMessages(List.of("最近工作忙吗"));

        AgentRelationshipEvaluateVo state = AgentRelationshipEvaluateVo.builder()
                .stageCode("warming")
                .wechatReadyScore(0.62)
                .dateReadyScore(0.48)
                .build();
        AgentCoachStyleVo expected = AgentCoachStyleVo.builder()
                .styleCode("steady_partner")
                .build();

        when(agentRuntimeBridgeService.evaluateRelationship(owner, 2002, "90001", form.getLastMessages(), null))
                .thenReturn(state);
        when(agentRuntimeBridgeService.selectCoachStyle(owner, 2002, state, null)).thenReturn(expected);

        AgentCoachStyleVo actual = agentAutonomyService.selectStyle(owner, form);

        assertSame(expected, actual);
    }

    @Test
    public void runProgramBuildsMatchOpeningSignalForHongniangTrigger() {
        AppUserEntity owner = user(1001);
        AgentRelationshipProgramRunForm form = new AgentRelationshipProgramRunForm();
        form.setTargetUid(2002);
        form.setSessionId("90001");
        form.setTriggerCode("hongniang_recommendation");
        form.setLastMessages(List.of("你好呀"));
        form.setMatchScore(91);
        form.setOpeningReadinessScore(0.86);
        form.setFitTags(List.of("同城", "看展"));
        form.setIcebreakOpeners(List.of("你平时周末会不会去看展或者找家舒服的小店坐坐？"));
        form.setRecommendedAction("open_chat");

        AgentProgramRunVo expected = AgentProgramRunVo.builder()
                .workflowCode("relationship_program")
                .build();

        when(agentRuntimeBridgeService.runRelationshipProgram(
                eq(owner),
                eq(2002),
                eq("hongniang_recommendation"),
                eq("90001"),
                eq(form.getLastMessages()),
                isNull(),
                argThat(signal -> signal != null
                        && Integer.valueOf(91).equals(signal.getInteger("match_score"))
                        && Double.valueOf(0.86).equals(signal.getDouble("opening_readiness_score"))
                        && "open_chat".equals(signal.getString("recommended_action"))
                        && signal.getJSONArray("fit_tags") != null
                        && signal.getJSONArray("fit_tags").contains("同城"))))
                .thenReturn(expected);

        AgentProgramRunVo actual = agentAutonomyService.runProgram(owner, form);

        assertSame(expected, actual);
    }

    @Test
    public void runProgramAutoExecutesSendActionWhenEnabled() {
        AppUserEntity owner = user(1001);
        AgentRelationshipProgramRunForm form = new AgentRelationshipProgramRunForm();
        form.setTargetUid(2002);
        form.setSessionId("90001");
        form.setTriggerCode("incoming_message");
        form.setAutoExecute(true);

        AgentProgramRunVo runtimePlan = AgentProgramRunVo.builder()
                .workflowCode("relationship_program")
                .nextAction(AgentProgramActionVo.builder()
                        .actionType("reply_send")
                        .content("你上次提到的那家店，我后来还真去看了下。")
                        .targetUserId(2002)
                        .executeMode("auto_if_permitted")
                        .riskLevel("low")
                        .build())
                .build();
        AgentSuggestedActionExecuteVo execution = AgentSuggestedActionExecuteVo.builder()
                .actionType("message_auto_send")
                .status("succeeded")
                .approvalRequired(false)
                .build();

        when(agentRuntimeBridgeService.runRelationshipProgram(owner, 2002, "incoming_message", "90001", null, null, null))
                .thenReturn(runtimePlan);
        when(agentSuggestedActionDispatchService.executeProgramAction(
                eq(owner),
                eq("90001"),
                argThat(action -> action != null
                        && "reply_send".equals(action.getActionType())
                        && "你上次提到的那家店，我后来还真去看了下。".equals(action.getContent()))))
                .thenReturn(execution);

        AgentProgramRunVo actual = agentAutonomyService.runProgram(owner, form);

        assertTrue(Boolean.TRUE.equals(actual.getAutoExecuted()));
        assertSame(execution, actual.getExecution());
    }

    @Test
    public void ingestChatMessageEventPublishesIncomingAndOutgoingPerspectives() {
        ChatMessageEntity message = ChatMessageEntity.builder()
                .sessionId("90001")
                .senderId("2002")
                .receiverId("1001")
                .sendTime("2026-04-03 10:00:00")
                .content("你周末喜欢看展吗")
                .messageType("text")
                .isWithdrawn(0)
                .build();
        AppUserEntity receiver = user(1001);
        AppUserEntity sender = user(2002);

        when(agentRuntimeBridgeService.isCompanionEnabled()).thenReturn(true);
        when(appUserService.getById(1001)).thenReturn(receiver);
        when(appUserService.getById(2002)).thenReturn(sender);

        agentAutonomyService.ingestChatMessageEvent(message);

        verify(agentRuntimeBridgeService).ingestRelationshipEvent(
                eq(receiver),
                eq(sender),
                eq("incoming_message"),
                eq("2026-04-03 10:00:00"),
                argThat(payload -> matchesMessagePayload(payload, "90001", "text", "2002", "1001", "你周末喜欢看展吗"))
        );
        verify(agentRuntimeBridgeService).ingestRelationshipEvent(
                eq(sender),
                eq(receiver),
                eq("outgoing_message"),
                eq("2026-04-03 10:00:00"),
                argThat(payload -> matchesMessagePayload(payload, "90001", "text", "2002", "1001", "你周末喜欢看展吗"))
        );
    }

    @Test
    public void ingestChatMessageEventRunsProgramAndExecutesAutoReplyForReceiver() {
        ChatMessageEntity message = ChatMessageEntity.builder()
                .sessionId("90001")
                .senderId("2002")
                .receiverId("1001")
                .sendTime("2026-04-03 10:00:00")
                .content("你周末喜欢看展吗")
                .messageType("text")
                .isWithdrawn(0)
                .build();
        AppUserEntity receiver = user(1001);
        AppUserEntity sender = user(2002);
        AgentProgramRunVo runtimePlan = AgentProgramRunVo.builder()
                .nextAction(AgentProgramActionVo.builder()
                        .actionType("reply_send")
                        .content("我平时还挺喜欢这种轻松一点的安排。")
                        .targetUserId(2002)
                        .executeMode("auto_if_permitted")
                        .build())
                .build();
        AgentSuggestedActionExecuteVo execution = AgentSuggestedActionExecuteVo.builder()
                .actionType("message_auto_send")
                .status("succeeded")
                .build();

        when(agentRuntimeBridgeService.isCompanionEnabled()).thenReturn(true);
        when(appUserService.getById(1001)).thenReturn(receiver);
        when(appUserService.getById(2002)).thenReturn(sender);
        when(agentRuntimeBridgeService.runRelationshipProgram(
                receiver,
                2002,
                "incoming_message",
                "90001",
                null,
                null,
                null
        )).thenReturn(runtimePlan);
        when(agentSuggestedActionDispatchService.executeProgramAction(
                eq(receiver),
                eq("90001"),
                argThat(action -> action != null
                        && "reply_send".equals(action.getActionType())
                        && "我平时还挺喜欢这种轻松一点的安排。".equals(action.getContent()))))
                .thenReturn(execution);

        agentAutonomyService.ingestChatMessageEvent(message);

        verify(agentSuggestedActionDispatchService).executeProgramAction(
                eq(receiver),
                eq("90001"),
                argThat(action -> action != null && "reply_send".equals(action.getActionType()))
        );
    }

    @Test
    public void ingestChatMessageEventSkipsStructuredOrBlankMessages() {
        ChatMessageEntity message = ChatMessageEntity.builder()
                .sessionId("90001")
                .senderId("2002")
                .receiverId("1001")
                .content("__SOCIAL_INTENT__:{\"type\":\"wechat_request\"}")
                .messageType("text")
                .build();

        agentAutonomyService.ingestChatMessageEvent(message);

        verifyNoInteractions(appUserService);
    }

    @Test
    public void runProgramSkipsExecutionWhenPlanIsDraftOnly() {
        AppUserEntity owner = user(1001);
        AgentRelationshipProgramRunForm form = new AgentRelationshipProgramRunForm();
        form.setTargetUid(2002);
        form.setSessionId("90001");
        form.setAutoExecute(true);

        AgentProgramRunVo runtimePlan = AgentProgramRunVo.builder()
                .nextAction(AgentProgramActionVo.builder()
                        .actionType("date_invite_draft")
                        .content("我们找个时间喝杯咖啡吧")
                        .executeMode("draft_only")
                        .build())
                .build();

        when(agentRuntimeBridgeService.runRelationshipProgram(owner, 2002, "manual_check", "90001", null, null, null))
                .thenReturn(runtimePlan);

        AgentProgramRunVo actual = agentAutonomyService.runProgram(owner, form);

        assertFalse(Boolean.TRUE.equals(actual.getAutoExecuted()));
        verifyNoInteractions(agentSuggestedActionDispatchService);
    }

    private boolean matchesMessagePayload(JSONObject payload,
                                          String sessionId,
                                          String messageType,
                                          String senderId,
                                          String receiverId,
                                          String content) {
        return payload != null
                && sessionId.equals(payload.getString("session_id"))
                && messageType.equals(payload.getString("message_type"))
                && senderId.equals(payload.getString("sender_id"))
                && receiverId.equals(payload.getString("receiver_id"))
                && content.equals(payload.getString("content"));
    }

    private AppUserEntity user(int uid) {
        AppUserEntity entity = new AppUserEntity();
        entity.setUid(uid);
        entity.setUsername("用户" + uid);
        return entity;
    }
}
