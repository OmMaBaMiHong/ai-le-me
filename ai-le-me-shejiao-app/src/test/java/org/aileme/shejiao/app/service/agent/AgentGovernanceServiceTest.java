package org.aileme.shejiao.app.service.agent;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.aileme.shejiao.api.service.FriendService;
import org.aileme.shejiao.app.dao.AgentActionApprovalDao;
import org.aileme.shejiao.app.dao.AgentActionLogDao;
import org.aileme.shejiao.app.dao.AgentActionTaskDao;
import org.aileme.shejiao.app.dao.AgentPermissionDao;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.AgentActionApprovalEntity;
import org.aileme.shejiao.domain.entity.app.AgentActionLogEntity;
import org.aileme.shejiao.domain.entity.app.AgentActionTaskEntity;
import org.aileme.shejiao.domain.entity.app.AgentPermissionEntity;
import org.aileme.shejiao.domain.param.app.AgentGiftExecuteForm;
import org.aileme.shejiao.domain.param.app.AgentMessageAutoSendForm;
import org.aileme.shejiao.domain.param.app.AgentPermissionSaveForm;
import org.aileme.shejiao.domain.vo.AgentActionTaskVo;
import org.aileme.shejiao.domain.vo.AgentActionLogVo;
import org.aileme.shejiao.domain.vo.AgentGiftExecuteVo;
import org.aileme.shejiao.domain.vo.AgentMessageAutoSendVo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgentGovernanceServiceTest {

    @Mock
    private AgentPermissionDao agentPermissionDao;

    @Mock
    private AgentActionTaskDao agentActionTaskDao;

    @Mock
    private AgentActionApprovalDao agentActionApprovalDao;

    @Mock
    private AgentActionLogDao agentActionLogDao;

    @Mock
    private FriendService friendService;

    @Mock
    private GiftExecuteService giftExecuteService;

    @Mock
    private AgentChatSendService agentChatSendService;

    @InjectMocks
    private AgentGovernanceService agentGovernanceService;

    @Test
    public void executeGiftWithGovernanceThrowsWhenCapabilityDisabled() {
        AppUserEntity currentUser = user(1001);
        AgentGiftExecuteForm form = giftForm(2002, 99);

        when(agentPermissionDao.selectOne(any())).thenReturn(null);

        LinfengException exception = assertThrows(
                LinfengException.class,
                () -> agentGovernanceService.executeGiftWithGovernance(currentUser, form)
        );

        assertEquals("当前未开启自动送礼权限", exception.getMsg());
        verifyNoInteractions(giftExecuteService, agentActionTaskDao, agentActionApprovalDao);
    }

    @Test
    public void executeGiftWithGovernanceAllowsManualGiftWhenCapabilityDisabled() {
        AppUserEntity currentUser = user(1001);
        AgentGiftExecuteForm form = giftForm(2002, 99);
        form.setManualTrigger(true);
        AgentGiftExecuteVo expected = AgentGiftExecuteVo.builder()
                .taskId(10001L)
                .giftName("星光玫瑰")
                .taskStatus("pending")
                .approvalRequired(false)
                .build();

        when(giftExecuteService.execute(currentUser, form)).thenReturn(expected);

        AgentGiftExecuteVo actual = agentGovernanceService.executeGiftWithGovernance(currentUser, form);

        assertSame(expected, actual);
        verify(giftExecuteService).execute(currentUser, form);
        verifyNoInteractions(agentActionTaskDao, agentActionApprovalDao);
    }

    @Test
    public void executeGiftWithGovernanceThrowsWhenTargetOutOfScope() {
        AppUserEntity currentUser = user(1001);
        AgentGiftExecuteForm form = giftForm(2002, 99);
        AgentPermissionEntity permission = permission(
                AgentGovernanceConstants.AUTHORIZE_MODE_CONDITIONAL_AUTO,
                AgentGovernanceConstants.TARGET_SCOPE_FRIENDS,
                19
        );

        when(agentPermissionDao.selectOne(any())).thenReturn(permission);
        when(friendService.checkIsFriend(1001, 2002)).thenReturn(false);

        LinfengException exception = assertThrows(
                LinfengException.class,
                () -> agentGovernanceService.executeGiftWithGovernance(currentUser, form)
        );

        assertEquals("当前仅允许对好友自动执行", exception.getMsg());
        verify(giftExecuteService, never()).execute(any(), any());
    }

    @Test
    public void executeGiftWithGovernanceDelegatesToGiftServiceWhenPolicyAllowsAutoExecution() {
        AppUserEntity currentUser = user(1001);
        AgentGiftExecuteForm form = giftForm(2002, 19);
        AgentPermissionEntity permission = permission(
                AgentGovernanceConstants.AUTHORIZE_MODE_CONDITIONAL_AUTO,
                AgentGovernanceConstants.TARGET_SCOPE_FRIENDS,
                99
        );
        AgentGiftExecuteVo expected = AgentGiftExecuteVo.builder()
                .taskId(999L)
                .giftName("星光玫瑰")
                .taskStatus("pending")
                .approvalRequired(false)
                .build();

        when(agentPermissionDao.selectOne(any())).thenReturn(permission);
        when(friendService.checkIsFriend(1001, 2002)).thenReturn(true);
        when(giftExecuteService.execute(currentUser, form)).thenReturn(expected);

        AgentGiftExecuteVo actual = agentGovernanceService.executeGiftWithGovernance(currentUser, form);

        assertSame(expected, actual);
        verify(agentActionTaskDao, never()).insert(org.mockito.ArgumentMatchers.<AgentActionTaskEntity>any());
        verify(agentActionApprovalDao, never()).insert(org.mockito.ArgumentMatchers.<AgentActionApprovalEntity>any());
    }

    @Test
    public void executeMessageAutoSendWithGovernanceThrowsWhenCapabilityDisabled() {
        AppUserEntity currentUser = user(1001);
        AgentMessageAutoSendForm form = messageForm(2002, "周末你方便喝杯咖啡吗？");

        when(agentPermissionDao.selectOne(any())).thenReturn(null);

        LinfengException exception = assertThrows(
                LinfengException.class,
                () -> agentGovernanceService.executeMessageAutoSendWithGovernance(currentUser, form)
        );

        assertEquals("当前未开启自动私聊权限", exception.getMsg());
        verifyNoInteractions(agentChatSendService, agentActionTaskDao, agentActionApprovalDao);
    }

    @Test
    public void executeMessageAutoSendWithGovernanceThrowsWhenTargetOutOfScope() {
        AppUserEntity currentUser = user(1001);
        AgentMessageAutoSendForm form = messageForm(2002, "想和你认真认识一下，方便加我微信吗？");
        AgentPermissionEntity permission = permission(
                AgentGovernanceConstants.AUTHORIZE_MODE_CONDITIONAL_AUTO,
                AgentGovernanceConstants.TARGET_SCOPE_FRIENDS,
                0
        );

        when(agentPermissionDao.selectOne(any())).thenReturn(permission);
        when(friendService.checkIsFriend(1001, 2002)).thenReturn(false);

        LinfengException exception = assertThrows(
                LinfengException.class,
                () -> agentGovernanceService.executeMessageAutoSendWithGovernance(currentUser, form)
        );

        assertEquals("当前仅允许对好友自动执行", exception.getMsg());
        verify(agentChatSendService, never()).sendText(any(), any());
    }

    @Test
    public void executeMessageAutoSendWithGovernanceDelegatesToChatServiceWhenSwitchEnabled() {
        AppUserEntity currentUser = user(1001);
        AgentMessageAutoSendForm form = messageForm(2002, "想和你认真认识一下，方便加我微信吗？");
        AgentPermissionEntity permission = permission(
                AgentGovernanceConstants.AUTHORIZE_MODE_CONDITIONAL_AUTO,
                AgentGovernanceConstants.TARGET_SCOPE_ALL,
                0
        );
        AgentMessageAutoSendVo expected = AgentMessageAutoSendVo.builder()
                .messageId(8801)
                .sessionId("90001")
                .content(form.getContent())
                .taskStatus(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED)
                .approvalRequired(false)
                .build();

        when(agentPermissionDao.selectOne(any())).thenReturn(permission);
        when(friendService.checkIsFriend(1001, 2002)).thenReturn(false);
        when(agentChatSendService.sendText(currentUser, form)).thenReturn(expected);

        AgentMessageAutoSendVo actual = agentGovernanceService.executeMessageAutoSendWithGovernance(currentUser, form);

        assertSame(expected, actual);
        verify(agentActionTaskDao, never()).insert(org.mockito.ArgumentMatchers.<AgentActionTaskEntity>any());
        verify(agentActionApprovalDao, never()).insert(org.mockito.ArgumentMatchers.<AgentActionApprovalEntity>any());
    }

    @Test
    public void approveTaskExecutesGiftAndMarksApprovalComplete() {
        AppUserEntity currentUser = user(1001);
        AgentGiftExecuteForm form = giftForm(2002, 19);
        AgentActionTaskEntity task = new AgentActionTaskEntity();
        task.setId(301L);
        task.setOwnerUserId(1001);
        task.setTargetUserId(2002);
        task.setCapabilityCode(AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE);
        task.setStatus(AgentGovernanceConstants.TASK_STATUS_PENDING_APPROVAL);
        task.setApprovalId(401L);
        task.setExecutionJson(JSON.toJSONString(form));
        task.setRequestId("governance_req_1");

        AgentActionApprovalEntity approval = new AgentActionApprovalEntity();
        approval.setId(401L);
        approval.setTaskId(301L);
        approval.setOwnerUserId(1001);
        approval.setStatus(AgentGovernanceConstants.APPROVAL_STATUS_PENDING);

        AgentGiftExecuteVo giftExecuteVo = AgentGiftExecuteVo.builder()
                .taskId(555L)
                .requestId("gift_req_1")
                .taskStatus("pending")
                .giftName("星光玫瑰")
                .approvalRequired(false)
                .build();

        when(agentActionTaskDao.selectById(301L)).thenReturn(task);
        when(agentActionApprovalDao.selectById(401L)).thenReturn(approval);
        when(giftExecuteService.execute(currentUser, form)).thenReturn(giftExecuteVo);

        AgentActionTaskVo response = agentGovernanceService.approveTask(currentUser, 301L, "允许自动送礼");

        assertEquals(301L, response.getTaskId());
        assertEquals(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED, response.getStatus());
        assertEquals(AgentGovernanceConstants.APPROVAL_STATUS_APPROVED, response.getApprovalStatus());
        assertEquals(555L, response.getBusinessId());
        assertFalse(Boolean.TRUE.equals(response.getApprovalRequired()));

        ArgumentCaptor<AgentActionTaskEntity> taskCaptor = ArgumentCaptor.forClass(AgentActionTaskEntity.class);
        verify(agentActionTaskDao).updateById(taskCaptor.capture());
        assertEquals(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED, taskCaptor.getValue().getStatus());
        assertTrue(taskCaptor.getValue().getResultJson().contains("gift_req_1"));

        verify(agentActionApprovalDao).updateById(argThat((AgentActionApprovalEntity item) ->
                AgentGovernanceConstants.APPROVAL_STATUS_APPROVED.equals(item.getStatus())
                        && "允许自动送礼".equals(item.getDecisionNote())));
    }

    @Test
    public void approveTaskExecutesMessageAndMarksApprovalComplete() {
        AppUserEntity currentUser = user(1001);
        AgentMessageAutoSendForm form = messageForm(2002, "下周三晚上如果你有空，我们可以一起吃个饭。");
        AgentActionTaskEntity task = new AgentActionTaskEntity();
        task.setId(302L);
        task.setOwnerUserId(1001);
        task.setTargetUserId(2002);
        task.setCapabilityCode(AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND);
        task.setStatus(AgentGovernanceConstants.TASK_STATUS_PENDING_APPROVAL);
        task.setApprovalId(402L);
        task.setExecutionJson(JSON.toJSONString(form));
        task.setRequestId("governance_msg_req_1");

        AgentActionApprovalEntity approval = new AgentActionApprovalEntity();
        approval.setId(402L);
        approval.setTaskId(302L);
        approval.setOwnerUserId(1001);
        approval.setStatus(AgentGovernanceConstants.APPROVAL_STATUS_PENDING);

        AgentMessageAutoSendVo messageVo = AgentMessageAutoSendVo.builder()
                .messageId(8802)
                .sessionId("91001")
                .content(form.getContent())
                .taskStatus(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED)
                .approvalRequired(false)
                .build();

        when(agentActionTaskDao.selectById(302L)).thenReturn(task);
        when(agentActionApprovalDao.selectById(402L)).thenReturn(approval);
        when(agentChatSendService.sendText(currentUser, form)).thenReturn(messageVo);

        AgentActionTaskVo response = agentGovernanceService.approveTask(currentUser, 302L, "允许自动代聊");

        assertEquals(302L, response.getTaskId());
        assertEquals(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED, response.getStatus());
        assertEquals(AgentGovernanceConstants.APPROVAL_STATUS_APPROVED, response.getApprovalStatus());
        assertEquals(8802L, response.getBusinessId());
        assertFalse(Boolean.TRUE.equals(response.getApprovalRequired()));
        assertTrue(response.getResultJson().contains("91001"));
    }

    @Test
    public void getTaskDetailReturnsOwnedTask() {
        AppUserEntity currentUser = user(1001);
        AgentActionTaskEntity task = new AgentActionTaskEntity();
        task.setId(501L);
        task.setOwnerUserId(1001);
        task.setTargetUserId(2002);
        task.setCapabilityCode(AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE);
        task.setStatus(AgentGovernanceConstants.TASK_STATUS_PENDING_APPROVAL);
        task.setApprovalId(601L);
        task.setRequestId("detail_req_1");
        task.setResultJson("{\"ok\":true}");

        AgentActionApprovalEntity approval = new AgentActionApprovalEntity();
        approval.setId(601L);
        approval.setStatus(AgentGovernanceConstants.APPROVAL_STATUS_PENDING);

        when(agentActionTaskDao.selectById(501L)).thenReturn(task);
        when(agentActionApprovalDao.selectById(601L)).thenReturn(approval);

        AgentActionTaskVo response = agentGovernanceService.getTaskDetail(currentUser, 501L);

        assertEquals(501L, response.getTaskId());
        assertEquals(AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE, response.getCapabilityCode());
        assertEquals(AgentGovernanceConstants.APPROVAL_STATUS_PENDING, response.getApprovalStatus());
        assertEquals("{\"ok\":true}", response.getResultJson());
    }

    @Test
    public void listTaskLogsReturnsMappedAuditTrail() {
        AppUserEntity currentUser = user(1001);
        AgentActionTaskEntity task = new AgentActionTaskEntity();
        task.setId(701L);
        task.setOwnerUserId(1001);

        AgentActionLogEntity log = new AgentActionLogEntity();
        log.setId(801L);
        log.setTaskId(701L);
        log.setApprovalId(901L);
        log.setCapabilityCode(AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND);
        log.setEventType("approval_requested");
        log.setEventStatus("success");
        log.setSourceType("java");
        log.setRiskLevel(AgentGovernanceConstants.RISK_LEVEL_LOW);
        log.setMessage("已创建自动私聊审批任务");
        log.setPayloadJson("{\"content\":\"你好\"}");

        when(agentActionTaskDao.selectById(701L)).thenReturn(task);
        when(agentActionLogDao.selectList(any())).thenReturn(java.util.List.of(log));

        java.util.List<AgentActionLogVo> logs = agentGovernanceService.listTaskLogs(currentUser, 701L);

        assertEquals(1, logs.size());
        assertEquals(801L, logs.get(0).getId());
        assertEquals("approval_requested", logs.get(0).getEventType());
        assertEquals(AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND, logs.get(0).getCapabilityCode());
    }

    @Test
    public void savePermissionUpsertsCapabilityPolicy() {
        AgentPermissionSaveForm form = new AgentPermissionSaveForm();
        form.setCapabilityCode(AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE);
        form.setEnabled(1);
        form.setAuthorizeMode(AgentGovernanceConstants.AUTHORIZE_MODE_CONDITIONAL_AUTO);
        form.setTargetScope(AgentGovernanceConstants.TARGET_SCOPE_FRIENDS);
        form.setRiskLevel(AgentGovernanceConstants.RISK_LEVEL_MEDIUM);
        form.setMaxAmountPerAction(19);
        form.setConsentVersion("v1");

        when(agentPermissionDao.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            AgentPermissionEntity entity = invocation.getArgument(0);
            entity.setId(701L);
            return 1;
        }).when(agentPermissionDao).insert(org.mockito.ArgumentMatchers.<AgentPermissionEntity>any());

        var response = agentGovernanceService.savePermission(1001, form);

        assertEquals(701L, response.getId());
        assertEquals(AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE, response.getCapabilityCode());
        assertEquals(19, response.getMaxAmountPerAction());
        verify(agentPermissionDao).insert(org.mockito.ArgumentMatchers.<AgentPermissionEntity>any());
    }

    @Test
    public void savePermissionUsesSwitchOnlyDefaultsWhenOptionalFieldsMissing() {
        AgentPermissionSaveForm form = new AgentPermissionSaveForm();
        form.setCapabilityCode(AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND);
        form.setEnabled(1);

        when(agentPermissionDao.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            AgentPermissionEntity entity = invocation.getArgument(0);
            entity.setId(702L);
            return 1;
        }).when(agentPermissionDao).insert(org.mockito.ArgumentMatchers.<AgentPermissionEntity>any());

        var response = agentGovernanceService.savePermission(1001, form);

        assertEquals(AgentGovernanceConstants.AUTHORIZE_MODE_CONDITIONAL_AUTO, response.getAuthorizeMode());
        assertEquals(AgentGovernanceConstants.TARGET_SCOPE_ALL, response.getTargetScope());
    }

    private AppUserEntity user(int uid) {
        AppUserEntity entity = new AppUserEntity();
        entity.setUid(uid);
        return entity;
    }

    private AgentGiftExecuteForm giftForm(int targetUid, int amount) {
        AgentGiftExecuteForm form = new AgentGiftExecuteForm();
        form.setTargetUid(targetUid);
        form.setPostId(88);
        form.setAmount(amount);
        form.setGiftCode("rose_1314");
        form.setGiftName("星光玫瑰");
        form.setGiftScene("heart");
        form.setGiftTheme("romantic");
        form.setRelationshipStage("early");
        form.setReason("适合轻量破冰");
        form.setNote("想认真认识你");
        return form;
    }

    private AgentPermissionEntity permission(String authorizeMode, String targetScope, int maxAmountPerAction) {
        AgentPermissionEntity entity = new AgentPermissionEntity();
        entity.setId(11L);
        entity.setUserId(1001);
        entity.setCapabilityCode(AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE);
        entity.setEnabled(1);
        entity.setAuthorizeMode(authorizeMode);
        entity.setTargetScope(targetScope);
        entity.setRiskLevel(AgentGovernanceConstants.RISK_LEVEL_MEDIUM);
        entity.setMaxAmountPerAction(maxAmountPerAction);
        return entity;
    }

    private AgentMessageAutoSendForm messageForm(int targetUid, String content) {
        AgentMessageAutoSendForm form = new AgentMessageAutoSendForm();
        form.setTargetUid(targetUid);
        form.setSessionId("90001");
        form.setContent(content);
        form.setSource("reply_suggestion");
        return form;
    }
}
