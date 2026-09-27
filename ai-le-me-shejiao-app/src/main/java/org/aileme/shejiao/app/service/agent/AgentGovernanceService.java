package org.aileme.shejiao.app.service.agent;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
import org.aileme.shejiao.domain.param.app.AgentApprovalDecisionForm;
import org.aileme.shejiao.domain.param.app.AgentGiftExecuteForm;
import org.aileme.shejiao.domain.param.app.AgentMessageAutoSendForm;
import org.aileme.shejiao.domain.param.app.AgentPermissionSaveForm;
import org.aileme.shejiao.domain.vo.AgentActionTaskVo;
import org.aileme.shejiao.domain.vo.AgentGiftExecuteVo;
import org.aileme.shejiao.domain.vo.AgentActionLogVo;
import org.aileme.shejiao.domain.vo.AgentMessageAutoSendVo;
import org.aileme.shejiao.domain.vo.AgentPermissionVo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AgentGovernanceService {

    private static final List<String> DEFAULT_CAPABILITIES = Arrays.asList(
            AgentGovernanceConstants.CAPABILITY_COMPANION_ENABLED,
            AgentGovernanceConstants.CAPABILITY_PHOTO_READ_SELECTED,
            AgentGovernanceConstants.CAPABILITY_MESSAGE_DRAFT,
            AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND,
            AgentGovernanceConstants.CAPABILITY_GIFT_PLAN,
            AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE,
            AgentGovernanceConstants.CAPABILITY_MOMENT_DRAFT,
            AgentGovernanceConstants.CAPABILITY_MOMENT_PUBLISH,
            AgentGovernanceConstants.CAPABILITY_DATE_COORDINATE,
            AgentGovernanceConstants.CAPABILITY_FOLLOW_RECOMMEND
    );

    @Autowired
    private AgentPermissionDao agentPermissionDao;

    @Autowired
    private AgentActionTaskDao agentActionTaskDao;

    @Autowired
    private AgentActionApprovalDao agentActionApprovalDao;

    @Autowired
    private AgentActionLogDao agentActionLogDao;

    @Autowired
    private FriendService friendService;

    @Autowired
    private GiftExecuteService giftExecuteService;

    @Autowired
    private AgentChatSendService agentChatSendService;

    public List<AgentPermissionVo> listPermissions(Integer userId) {
        List<AgentPermissionVo> result = new ArrayList<>();
        for (String capabilityCode : DEFAULT_CAPABILITIES) {
            AgentPermissionEntity entity = findPermissionEntity(userId, capabilityCode);
            result.add(toPermissionVo(entity == null ? buildDefaultPermission(userId, capabilityCode) : entity));
        }
        return result;
    }

    public AgentPermissionVo savePermission(Integer userId, AgentPermissionSaveForm form) {
        if (userId == null || userId <= 0) {
            throw new LinfengException("用户不存在");
        }
        AgentPermissionEntity entity = findPermissionEntity(userId, form.getCapabilityCode());
        Date now = new Date();
        boolean insert = entity == null;
        if (insert) {
            entity = new AgentPermissionEntity();
            entity.setUserId(userId);
            entity.setCapabilityCode(form.getCapabilityCode());
            entity.setCreateTime(now);
        }
        entity.setEnabled(normalizeEnabled(form.getEnabled()));
        entity.setAuthorizeMode(defaultAuthorizeMode(form.getAuthorizeMode()));
        entity.setTargetScope(defaultTargetScope(form.getTargetScope()));
        entity.setRiskLevel(defaultRiskLevel(form.getRiskLevel()));
        entity.setMaxAmountPerAction(defaultInt(form.getMaxAmountPerAction()));
        entity.setMaxAmountPerDay(defaultInt(form.getMaxAmountPerDay()));
        entity.setMaxActionsPerDay(defaultInt(form.getMaxActionsPerDay()));
        entity.setQuietHoursJson(StringUtils.trimToNull(form.getQuietHoursJson()));
        entity.setPolicyJson(StringUtils.trimToNull(form.getPolicyJson()));
        entity.setConsentVersion(StringUtils.defaultString(form.getConsentVersion()));
        entity.setConfirmedAt(entity.getEnabled() != null && entity.getEnabled() == 1 ? now : null);
        entity.setUpdateTime(now);
        if (insert) {
            agentPermissionDao.insert(entity);
        } else {
            agentPermissionDao.updateById(entity);
        }
        return toPermissionVo(entity);
    }

    public boolean isCapabilityEnabled(Integer userId, String capabilityCode) {
        AgentPermissionEntity permission = findPermissionEntity(userId, capabilityCode);
        return permission != null && Objects.equals(permission.getEnabled(), 1);
    }

    public AgentPermissionVo getPermission(Integer userId, String capabilityCode) {
        AgentPermissionEntity permission = findPermissionEntity(userId, capabilityCode);
        if (permission == null) {
            permission = buildDefaultPermission(userId, capabilityCode);
        }
        return toPermissionVo(permission);
    }

    public AgentGiftExecuteVo executeGiftWithGovernance(AppUserEntity currentUser, AgentGiftExecuteForm form) {
        Integer currentUid = currentUser == null ? null : currentUser.getUid();
        if (form != null && Boolean.TRUE.equals(form.getManualTrigger())) {
            return giftExecuteService.execute(currentUser, form);
        }
        AgentPermissionEntity permission = findPermissionEntity(currentUid, AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE);
        if (permission == null || !Objects.equals(permission.getEnabled(), 1)) {
            throw new LinfengException("当前未开启自动送礼权限");
        }

        boolean isFriend = Boolean.TRUE.equals(friendService.checkIsFriend(currentUid, form.getTargetUid()));
        requiresApproval(permission, AgentGovernanceConstants.RISK_LEVEL_MEDIUM, form.getAmount(), isFriend);

        AgentGiftExecuteVo response = giftExecuteService.execute(currentUser, form);
        if (response != null) {
            response.setApprovalRequired(false);
            response.setApprovalId(null);
            response.setApprovalStatus(null);
            response.setGovernanceTaskId(null);
        }
        return response;
    }

    public AgentMessageAutoSendVo executeMessageAutoSendWithGovernance(AppUserEntity currentUser, AgentMessageAutoSendForm form) {
        Integer currentUid = currentUser == null ? null : currentUser.getUid();
        AgentPermissionEntity permission = findPermissionEntity(currentUid, AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND);
        if (permission == null || !Objects.equals(permission.getEnabled(), 1)) {
            throw new LinfengException("当前未开启自动私聊权限");
        }

        String riskLevel = resolveMessageRiskLevel(form == null ? null : form.getContent());
        boolean isFriend = Boolean.TRUE.equals(friendService.checkIsFriend(currentUid, form.getTargetUid()));
        requiresApproval(permission, riskLevel, null, isFriend);

        AgentMessageAutoSendVo response = agentChatSendService.sendText(currentUser, form);
        if (response != null) {
            response.setApprovalRequired(false);
            response.setApprovalId(null);
            response.setApprovalStatus(null);
            response.setGovernanceTaskId(null);
        }
        return response;
    }

    public boolean requiresApproval(AgentPermissionEntity permission,
                                    String riskLevel,
                                    Integer amount,
                                    boolean isFriend) {
        if (permission == null || !Objects.equals(permission.getEnabled(), 1)) {
            return true;
        }
        String targetScope = defaultTargetScope(permission.getTargetScope());
        if (AgentGovernanceConstants.TARGET_SCOPE_NONE.equalsIgnoreCase(targetScope)) {
            throw new LinfengException("当前权限未授予可执行目标范围");
        }
        if (AgentGovernanceConstants.TARGET_SCOPE_FRIENDS.equalsIgnoreCase(targetScope) && !isFriend) {
            throw new LinfengException("当前仅允许对好友自动执行");
        }
        Integer maxAmountPerAction = permission.getMaxAmountPerAction();
        if (maxAmountPerAction != null
                && maxAmountPerAction > 0
                && amount != null
                && amount > maxAmountPerAction) {
            throw new LinfengException("已超过当前单次额度上限");
        }
        return false;
    }

    public List<AgentActionTaskVo> listPendingTasks(Integer userId) {
        List<AgentActionTaskEntity> entities = agentActionTaskDao.selectList(new LambdaQueryWrapper<AgentActionTaskEntity>()
                .eq(AgentActionTaskEntity::getOwnerUserId, userId)
                .eq(AgentActionTaskEntity::getStatus, AgentGovernanceConstants.TASK_STATUS_PENDING_APPROVAL)
                .orderByDesc(AgentActionTaskEntity::getId));
        List<AgentActionTaskVo> result = new ArrayList<>();
        for (AgentActionTaskEntity entity : entities) {
            result.add(toTaskVo(entity, loadApproval(entity.getApprovalId()), null, null, entity.getErrorMessage()));
        }
        return result;
    }

    public AgentActionTaskVo getTaskDetail(AppUserEntity currentUser, Long taskId) {
        AgentActionTaskEntity task = requireTask(currentUser, taskId);
        AgentActionApprovalEntity approval = loadApproval(task.getApprovalId());
        return toTaskVo(task, approval, null, null, task.getErrorMessage());
    }

    public List<AgentActionTaskVo> listActionHistory(Integer userId, String capabilityCode, String status, Integer limit) {
        LambdaQueryWrapper<AgentActionTaskEntity> wrapper = new LambdaQueryWrapper<AgentActionTaskEntity>()
                .eq(AgentActionTaskEntity::getOwnerUserId, userId);
        if (StringUtils.isNotBlank(capabilityCode)) {
            wrapper.eq(AgentActionTaskEntity::getCapabilityCode, capabilityCode.trim());
        }
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(AgentActionTaskEntity::getStatus, status.trim());
        }
        wrapper.orderByDesc(AgentActionTaskEntity::getId)
                .last("limit " + normalizeHistoryLimit(limit));
        List<AgentActionTaskEntity> entities = agentActionTaskDao.selectList(wrapper);
        List<AgentActionTaskVo> result = new ArrayList<>();
        for (AgentActionTaskEntity entity : entities) {
            result.add(toTaskVo(entity, loadApproval(entity.getApprovalId()), null, null, entity.getErrorMessage()));
        }
        return result;
    }

    public List<AgentActionLogVo> listTaskLogs(AppUserEntity currentUser, Long taskId) {
        AgentActionTaskEntity task = requireTask(currentUser, taskId);
        List<AgentActionLogEntity> entities = agentActionLogDao.selectList(new LambdaQueryWrapper<AgentActionLogEntity>()
                .eq(AgentActionLogEntity::getTaskId, task.getId())
                .orderByAsc(AgentActionLogEntity::getId));
        List<AgentActionLogVo> result = new ArrayList<>();
        for (AgentActionLogEntity entity : entities) {
            result.add(toActionLogVo(entity));
        }
        return result;
    }

    public AgentActionTaskVo approveTask(AppUserEntity currentUser, Long taskId, String note) {
        AgentActionTaskEntity task = requireTask(currentUser, taskId);
        AgentActionApprovalEntity approval = requireApproval(task);
        if (!AgentGovernanceConstants.TASK_STATUS_PENDING_APPROVAL.equals(task.getStatus())) {
            return toTaskVo(task, approval, null, null, task.getErrorMessage());
        }
        if (!AgentGovernanceConstants.APPROVAL_STATUS_PENDING.equals(approval.getStatus())) {
            return toTaskVo(task, approval, null, null, task.getErrorMessage());
        }

        Date now = new Date();
        approval.setStatus(AgentGovernanceConstants.APPROVAL_STATUS_APPROVED);
        approval.setDecisionByUserId(currentUser.getUid());
        approval.setDecisionNote(limitText(note, 255));
        approval.setDecidedAt(now);
        approval.setUpdateTime(now);
        agentActionApprovalDao.updateById(approval);

        try {
            ApprovalExecutionResult executionResult = executeApprovedTask(currentUser, task);
            task.setStatus(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED);
            task.setRequiresApproval(0);
            task.setResultJson(executionResult.resultJson());
            task.setErrorMessage("");
            task.setUpdateTime(now);
            agentActionTaskDao.updateById(task);
            writeActionLog(task, "approval_approved", "success", executionResult.taskMessage());
            return toTaskVo(task, approval, executionResult.businessId(), executionResult.resultJson(), executionResult.taskMessage());
        } catch (RuntimeException ex) {
            task.setStatus(AgentGovernanceConstants.TASK_STATUS_FAILED);
            task.setErrorMessage(limitText(ex.getMessage(), 1000));
            task.setUpdateTime(now);
            agentActionTaskDao.updateById(task);
            writeActionLog(task, "approval_execute_failed", "failed", ex.getMessage());
            throw ex;
        }
    }

    public AgentActionTaskVo rejectTask(AppUserEntity currentUser, Long taskId, String note) {
        AgentActionTaskEntity task = requireTask(currentUser, taskId);
        AgentActionApprovalEntity approval = requireApproval(task);
        if (!AgentGovernanceConstants.TASK_STATUS_PENDING_APPROVAL.equals(task.getStatus())) {
            return toTaskVo(task, approval, null, null, task.getErrorMessage());
        }

        Date now = new Date();
        approval.setStatus(AgentGovernanceConstants.APPROVAL_STATUS_REJECTED);
        approval.setDecisionByUserId(currentUser.getUid());
        approval.setDecisionNote(limitText(note, 255));
        approval.setDecidedAt(now);
        approval.setUpdateTime(now);
        agentActionApprovalDao.updateById(approval);

        task.setStatus(AgentGovernanceConstants.TASK_STATUS_REJECTED);
        task.setErrorMessage(limitText(firstNonBlank(note, "审批已拒绝"), 1000));
        task.setUpdateTime(now);
        agentActionTaskDao.updateById(task);
        writeActionLog(task, "approval_rejected", "success", task.getErrorMessage());
        return toTaskVo(task, approval, null, null, task.getErrorMessage());
    }

    private AgentGiftExecuteVo createPendingGiftApproval(AppUserEntity currentUser,
                                                         AgentGiftExecuteForm form,
                                                         AgentPermissionEntity permission) {
        Date now = new Date();
        String requestId = UUID.randomUUID().toString().replace("-", "");

        AgentActionTaskEntity task = new AgentActionTaskEntity();
        task.setRequestId(requestId);
        task.setOwnerUserId(currentUser.getUid());
        task.setTargetUserId(form.getTargetUid());
        task.setCapabilityCode(AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE);
        task.setSceneCode("gift_execute");
        task.setRiskLevel(AgentGovernanceConstants.RISK_LEVEL_MEDIUM);
        task.setStatus(AgentGovernanceConstants.TASK_STATUS_PENDING_APPROVAL);
        task.setRequiresApproval(1);
        task.setOperatorMode("assistant");
        task.setPolicySnapshotJson(JSON.toJSONString(permission));
        task.setExecutionJson(JSON.toJSONString(form));
        task.setResultJson("");
        task.setErrorMessage("");
        task.setCreateTime(now);
        task.setUpdateTime(now);
        agentActionTaskDao.insert(task);

        AgentActionApprovalEntity approval = new AgentActionApprovalEntity();
        approval.setTaskId(task.getId());
        approval.setOwnerUserId(currentUser.getUid());
        approval.setTargetUserId(form.getTargetUid());
        approval.setCapabilityCode(AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE);
        approval.setRiskLevel(AgentGovernanceConstants.RISK_LEVEL_MEDIUM);
        approval.setStatus(AgentGovernanceConstants.APPROVAL_STATUS_PENDING);
        approval.setApprovalPayloadJson(JSON.toJSONString(form));
        approval.setDecisionNote("");
        approval.setDecisionByUserId(0);
        approval.setRequestedAt(now);
        approval.setExpireTime(new Date(now.getTime() + 24L * 60 * 60 * 1000));
        approval.setCreateTime(now);
        approval.setUpdateTime(now);
        agentActionApprovalDao.insert(approval);

        task.setApprovalId(approval.getId());
        task.setUpdateTime(now);
        agentActionTaskDao.updateById(task);
        writeActionLog(task, "approval_requested", "success", "已创建送礼审批任务");

        return AgentGiftExecuteVo.builder()
                .requestId(requestId)
                .amount(form.getAmount())
                .giftName(form.getGiftName())
                .sessionId(form.getSessionId())
                .businessStatus("pending_approval")
                .taskStatus(AgentGovernanceConstants.TASK_STATUS_PENDING_APPROVAL)
                .taskMessage("已提交审批，等待确认后自动送礼")
                .assetUrls(new ArrayList<>())
                .currentBalance(null)
                .generated(false)
                .approvalRequired(true)
                .governanceTaskId(task.getId())
                .approvalId(approval.getId())
                .approvalStatus(AgentGovernanceConstants.APPROVAL_STATUS_PENDING)
                .build();
    }

    private AgentMessageAutoSendVo createPendingMessageApproval(AppUserEntity currentUser,
                                                                AgentMessageAutoSendForm form,
                                                                AgentPermissionEntity permission,
                                                                String riskLevel) {
        Date now = new Date();
        String requestId = UUID.randomUUID().toString().replace("-", "");

        AgentActionTaskEntity task = new AgentActionTaskEntity();
        task.setRequestId(requestId);
        task.setOwnerUserId(currentUser.getUid());
        task.setTargetUserId(form.getTargetUid());
        task.setCapabilityCode(AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND);
        task.setSceneCode("message_auto_send");
        task.setRiskLevel(riskLevel);
        task.setStatus(AgentGovernanceConstants.TASK_STATUS_PENDING_APPROVAL);
        task.setRequiresApproval(1);
        task.setOperatorMode("assistant");
        task.setPolicySnapshotJson(JSON.toJSONString(permission));
        task.setExecutionJson(JSON.toJSONString(form));
        task.setResultJson("");
        task.setErrorMessage("");
        task.setCreateTime(now);
        task.setUpdateTime(now);
        agentActionTaskDao.insert(task);

        AgentActionApprovalEntity approval = new AgentActionApprovalEntity();
        approval.setTaskId(task.getId());
        approval.setOwnerUserId(currentUser.getUid());
        approval.setTargetUserId(form.getTargetUid());
        approval.setCapabilityCode(AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND);
        approval.setRiskLevel(riskLevel);
        approval.setStatus(AgentGovernanceConstants.APPROVAL_STATUS_PENDING);
        approval.setApprovalPayloadJson(JSON.toJSONString(form));
        approval.setDecisionNote("");
        approval.setDecisionByUserId(0);
        approval.setRequestedAt(now);
        approval.setExpireTime(new Date(now.getTime() + 24L * 60 * 60 * 1000));
        approval.setCreateTime(now);
        approval.setUpdateTime(now);
        agentActionApprovalDao.insert(approval);

        task.setApprovalId(approval.getId());
        task.setUpdateTime(now);
        agentActionTaskDao.updateById(task);
        writeActionLog(task, "approval_requested", "success", "已创建自动私聊审批任务");

        return AgentMessageAutoSendVo.builder()
                .requestId(requestId)
                .targetUid(form.getTargetUid())
                .sessionId(StringUtils.trimToEmpty(form.getSessionId()))
                .content(limitText(form.getContent(), 255))
                .source(StringUtils.trimToEmpty(form.getSource()))
                .taskStatus(AgentGovernanceConstants.TASK_STATUS_PENDING_APPROVAL)
                .taskMessage("已提交审批，等待确认后自动发送")
                .approvalRequired(true)
                .governanceTaskId(task.getId())
                .approvalId(approval.getId())
                .approvalStatus(AgentGovernanceConstants.APPROVAL_STATUS_PENDING)
                .build();
    }

    private ApprovalExecutionResult executeApprovedTask(AppUserEntity currentUser, AgentActionTaskEntity task) {
        if (AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE.equals(task.getCapabilityCode())) {
            AgentGiftExecuteVo giftExecuteVo = executeApprovedGift(currentUser, task);
            return new ApprovalExecutionResult(
                    giftExecuteVo == null || giftExecuteVo.getTaskId() == null ? null : giftExecuteVo.getTaskId(),
                    giftExecuteVo == null ? "" : JSON.toJSONString(giftExecuteVo),
                    "审批通过并已执行"
            );
        }
        if (AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND.equals(task.getCapabilityCode())) {
            AgentMessageAutoSendVo messageAutoSendVo = executeApprovedMessage(currentUser, task);
            return new ApprovalExecutionResult(
                    messageAutoSendVo == null || messageAutoSendVo.getMessageId() == null ? null : Long.valueOf(messageAutoSendVo.getMessageId()),
                    messageAutoSendVo == null ? "" : JSON.toJSONString(messageAutoSendVo),
                    "审批通过并已发送消息"
            );
        }
        throw new LinfengException("当前任务暂不支持自动执行");
    }

    private AgentGiftExecuteVo executeApprovedGift(AppUserEntity currentUser, AgentActionTaskEntity task) {
        if (!AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE.equals(task.getCapabilityCode())) {
            throw new LinfengException("当前任务暂不支持自动执行");
        }
        AgentGiftExecuteForm form = JSON.parseObject(task.getExecutionJson(), AgentGiftExecuteForm.class);
        if (form == null) {
            throw new LinfengException("审批任务缺少执行参数");
        }
        AgentGiftExecuteVo response = giftExecuteService.execute(currentUser, form);
        if (response != null) {
            response.setApprovalRequired(false);
            response.setApprovalId(task.getApprovalId());
            response.setApprovalStatus(AgentGovernanceConstants.APPROVAL_STATUS_APPROVED);
            response.setGovernanceTaskId(task.getId());
        }
        return response;
    }

    private AgentMessageAutoSendVo executeApprovedMessage(AppUserEntity currentUser, AgentActionTaskEntity task) {
        if (!AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND.equals(task.getCapabilityCode())) {
            throw new LinfengException("当前任务暂不支持自动执行");
        }
        AgentMessageAutoSendForm form = JSON.parseObject(task.getExecutionJson(), AgentMessageAutoSendForm.class);
        if (form == null) {
            throw new LinfengException("审批任务缺少执行参数");
        }
        AgentMessageAutoSendVo response = agentChatSendService.sendText(currentUser, form);
        if (response != null) {
            response.setRequestId(task.getRequestId());
            response.setApprovalRequired(false);
            response.setApprovalId(task.getApprovalId());
            response.setApprovalStatus(AgentGovernanceConstants.APPROVAL_STATUS_APPROVED);
            response.setGovernanceTaskId(task.getId());
            response.setTaskStatus(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED);
            response.setTaskMessage("审批通过并已发送消息");
        }
        return response;
    }

    private void writeActionLog(AgentActionTaskEntity task, String eventType, String eventStatus, String message) {
        AgentActionLogEntity entity = new AgentActionLogEntity();
        entity.setTraceId(StringUtils.defaultIfBlank(task.getRuntimeTraceId(), task.getRequestId()));
        entity.setRequestId(task.getRequestId());
        entity.setTaskId(task.getId());
        entity.setApprovalId(task.getApprovalId());
        entity.setOwnerUserId(task.getOwnerUserId());
        entity.setTargetUserId(task.getTargetUserId());
        entity.setCapabilityCode(task.getCapabilityCode());
        entity.setEventType(eventType);
        entity.setEventStatus(eventStatus);
        entity.setSourceType("java");
        entity.setRiskLevel(task.getRiskLevel());
        entity.setMessage(limitText(message, 255));
        entity.setPayloadJson(task.getExecutionJson());
        entity.setCreateTime(new Date());
        agentActionLogDao.insert(entity);
    }

    private AgentActionTaskEntity requireTask(AppUserEntity currentUser, Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new LinfengException("审批任务不存在");
        }
        AgentActionTaskEntity task = agentActionTaskDao.selectById(taskId);
        if (task == null) {
            throw new LinfengException("审批任务不存在");
        }
        Integer currentUid = currentUser == null ? null : currentUser.getUid();
        if (!Objects.equals(task.getOwnerUserId(), currentUid)) {
            throw new LinfengException("无权操作该审批任务");
        }
        return task;
    }

    private AgentActionApprovalEntity requireApproval(AgentActionTaskEntity task) {
        AgentActionApprovalEntity approval = loadApproval(task.getApprovalId());
        if (approval == null) {
            throw new LinfengException("审批记录不存在");
        }
        return approval;
    }

    private AgentActionApprovalEntity loadApproval(Long approvalId) {
        if (approvalId == null || approvalId <= 0) {
            return null;
        }
        return agentActionApprovalDao.selectById(approvalId);
    }

    private AgentPermissionEntity findPermissionEntity(Integer userId, String capabilityCode) {
        if (userId == null || userId <= 0 || StringUtils.isBlank(capabilityCode)) {
            return null;
        }
        return agentPermissionDao.selectOne(new LambdaQueryWrapper<AgentPermissionEntity>()
                .eq(AgentPermissionEntity::getUserId, userId)
                .eq(AgentPermissionEntity::getCapabilityCode, capabilityCode)
                .last("limit 1"));
    }

    private AgentPermissionEntity buildDefaultPermission(Integer userId, String capabilityCode) {
        AgentPermissionEntity entity = new AgentPermissionEntity();
        entity.setUserId(userId);
        entity.setCapabilityCode(capabilityCode);
        entity.setEnabled(0);
        entity.setAuthorizeMode(AgentGovernanceConstants.AUTHORIZE_MODE_CONDITIONAL_AUTO);
        entity.setTargetScope(AgentGovernanceConstants.TARGET_SCOPE_ALL);
        entity.setRiskLevel(AgentGovernanceConstants.RISK_LEVEL_MEDIUM);
        entity.setMaxAmountPerAction(0);
        entity.setMaxAmountPerDay(0);
        entity.setMaxActionsPerDay(0);
        entity.setConsentVersion("");
        return entity;
    }

    private AgentPermissionVo toPermissionVo(AgentPermissionEntity entity) {
        return AgentPermissionVo.builder()
                .id(entity.getId())
                .capabilityCode(entity.getCapabilityCode())
                .enabled(entity.getEnabled())
                .authorizeMode(entity.getAuthorizeMode())
                .targetScope(entity.getTargetScope())
                .riskLevel(entity.getRiskLevel())
                .maxAmountPerAction(entity.getMaxAmountPerAction())
                .maxAmountPerDay(entity.getMaxAmountPerDay())
                .maxActionsPerDay(entity.getMaxActionsPerDay())
                .quietHoursJson(entity.getQuietHoursJson())
                .policyJson(entity.getPolicyJson())
                .consentVersion(entity.getConsentVersion())
                .build();
    }

    private AgentActionTaskVo toTaskVo(AgentActionTaskEntity task,
                                       AgentActionApprovalEntity approval,
                                       Long businessId,
                                       String resultJson,
                                       String taskMessage) {
        return AgentActionTaskVo.builder()
                .taskId(task.getId())
                .requestId(task.getRequestId())
                .capabilityCode(task.getCapabilityCode())
                .sceneCode(task.getSceneCode())
                .status(task.getStatus())
                .approvalRequired(task.getRequiresApproval() != null && task.getRequiresApproval() == 1)
                .approvalId(task.getApprovalId())
                .approvalStatus(approval == null ? null : approval.getStatus())
                .targetUserId(task.getTargetUserId())
                .businessId(businessId)
                .taskMessage(limitText(firstNonBlank(taskMessage, task.getErrorMessage()), 255))
                .resultJson(StringUtils.defaultString(firstNonBlank(resultJson, task.getResultJson())))
                .build();
    }

    private AgentActionLogVo toActionLogVo(AgentActionLogEntity entity) {
        return AgentActionLogVo.builder()
                .id(entity.getId())
                .taskId(entity.getTaskId())
                .approvalId(entity.getApprovalId())
                .capabilityCode(entity.getCapabilityCode())
                .eventType(entity.getEventType())
                .eventStatus(entity.getEventStatus())
                .sourceType(entity.getSourceType())
                .riskLevel(entity.getRiskLevel())
                .message(entity.getMessage())
                .payloadJson(entity.getPayloadJson())
                .createTime(entity.getCreateTime())
                .build();
    }

    private String resolveMessageRiskLevel(String content) {
        String text = StringUtils.defaultString(content).toLowerCase();
        if (StringUtils.isBlank(text)) {
            return AgentGovernanceConstants.RISK_LEVEL_MEDIUM;
        }
        if (text.contains("微信")
                || text.contains("vx")
                || text.contains("vx:")
                || text.contains("加我")
                || text.contains("手机号")
                || text.matches(".*\\d{11}.*")) {
            return AgentGovernanceConstants.RISK_LEVEL_HIGH;
        }
        if (text.length() >= 120) {
            return AgentGovernanceConstants.RISK_LEVEL_MEDIUM;
        }
        return AgentGovernanceConstants.RISK_LEVEL_LOW;
    }

    private int normalizeHistoryLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, 50);
    }

    private Integer normalizeEnabled(Integer enabled) {
        return enabled != null && enabled == 1 ? 1 : 0;
    }

    private Integer defaultInt(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private String defaultAuthorizeMode(String value) {
        if (StringUtils.equalsAnyIgnoreCase(value,
                AgentGovernanceConstants.AUTHORIZE_MODE_MANUAL_ONLY,
                AgentGovernanceConstants.AUTHORIZE_MODE_AUTO_DRAFT,
                AgentGovernanceConstants.AUTHORIZE_MODE_CONDITIONAL_AUTO)) {
            return value;
        }
        return AgentGovernanceConstants.AUTHORIZE_MODE_CONDITIONAL_AUTO;
    }

    private String defaultTargetScope(String value) {
        if (StringUtils.equalsAnyIgnoreCase(value,
                AgentGovernanceConstants.TARGET_SCOPE_NONE,
                AgentGovernanceConstants.TARGET_SCOPE_ALL,
                AgentGovernanceConstants.TARGET_SCOPE_WHITELIST,
                AgentGovernanceConstants.TARGET_SCOPE_FRIENDS,
                AgentGovernanceConstants.TARGET_SCOPE_MATCHED_ONLY,
                AgentGovernanceConstants.TARGET_SCOPE_CUSTOM)) {
            return value;
        }
        return AgentGovernanceConstants.TARGET_SCOPE_ALL;
    }

    private String defaultRiskLevel(String value) {
        if (StringUtils.equalsAnyIgnoreCase(value,
                AgentGovernanceConstants.RISK_LEVEL_LOW,
                AgentGovernanceConstants.RISK_LEVEL_MEDIUM,
                AgentGovernanceConstants.RISK_LEVEL_HIGH,
                AgentGovernanceConstants.RISK_LEVEL_CRITICAL)) {
            return value;
        }
        return AgentGovernanceConstants.RISK_LEVEL_MEDIUM;
    }

    private String limitText(String value, int maxLength) {
        String text = StringUtils.defaultString(value);
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private record ApprovalExecutionResult(Long businessId, String resultJson, String taskMessage) {
    }
}
