package org.aileme.shejiao.app.service.impl;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aileme.common.redis.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.admin.dao.HongniangMatchCaseDao;
import org.aileme.shejiao.admin.dao.HongniangMatchProgressDao;
import org.aileme.shejiao.admin.dao.HongniangMatchRequestDao;
import org.aileme.shejiao.admin.service.impl.HongniangMatchCaseServiceImpl;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.BillService;
import org.aileme.shejiao.api.service.ChatMessageService;
import org.aileme.shejiao.api.service.FriendService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.api.service.UserLevelService;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.common.utils.RedisKeys;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchCaseEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchProgressEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchRequestEntity;
import org.aileme.shejiao.domain.entity.app.ChatMessageEntity;
import org.aileme.shejiao.domain.entity.app.FriendEntity;
import org.aileme.shejiao.domain.param.app.ConfirmWechatIntentForm;
import org.aileme.shejiao.domain.param.app.RejectWechatIntentForm;
import org.aileme.shejiao.domain.vo.AgentMessageAutoSendVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@DS("master")
@Service
public class SocialIntentService {
    private static final String SOCIAL_INTENT_PREFIX = SocialIntentMessageService.SOCIAL_INTENT_PREFIX;
    private static final String SOCIAL_INTENT_WECHAT_REQUEST = "wechat_request";
    private static final String SOCIAL_INTENT_WECHAT_RESULT = "wechat_result";
    private static final String SOCIAL_INTENT_WECHAT_STATUS = "wechat_status";
    private static final String SOCIAL_INTENT_MATCH_REQUEST = "hongniang_match_request";
    private static final String SOCIAL_INTENT_MATCH_STATUS = "hongniang_match_status";
    private static final String SOCIAL_INTENT_STATUS_PENDING = "pending";
    private static final String SOCIAL_INTENT_STATUS_AGREED = "agreed";
    private static final String SOCIAL_INTENT_STATUS_REJECTED = "rejected";
    private static final String SOCIAL_INTENT_DONE_KEY = "social_intent:wechat:done:";
    private static final String SOCIAL_INTENT_STATE_KEY = "social_intent:wechat:state:";

    @Autowired
    private FriendService friendService;
    @Autowired
    private AppUserService appUserService;
    @Autowired
    private BillService billService;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private UserLevelService userLevelService;
    @Autowired
    private SysConfigService configService;
    @Autowired
    private SocialIntentMessageService socialIntentMessageService;
    @Autowired
    private HongniangMatchRequestDao matchRequestDao;
    @Autowired
    private HongniangMatchCaseDao matchCaseDao;
    @Autowired
    private HongniangMatchProgressDao matchProgressDao;
    @Autowired
    private HongniangService hongniangService;

    @DSTransactional
    public AgentMessageAutoSendVo initiateWechatRequest(AppUserEntity currentUser,
                                                        Integer targetUid,
                                                        String sessionId,
                                                        String requestMessage,
                                                        Integer amount,
                                                        String source) {
        Integer requesterUid = currentUser == null ? null : currentUser.getUid();
        if (requesterUid == null || requesterUid <= 0) {
            throw new LinfengException("当前用户不存在");
        }
        if (targetUid == null || targetUid <= 0 || Objects.equals(requesterUid, targetUid)) {
            throw new LinfengException("目标用户不存在");
        }

        Long relationSessionId = friendService.getOrCreateSession(requesterUid, targetUid);
        String resolvedSessionId = String.valueOf(relationSessionId);
        if (StringUtils.isNotBlank(sessionId) && !StringUtils.equals(sessionId, resolvedSessionId)) {
            throw new LinfengException("会话已变更，请刷新后重试");
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        int safeAmount = Math.max(0, Math.min(toInt(amount, 0), 5000));
        String safeMessage = StringUtils.left(StringUtils.trimToEmpty(requestMessage), 120);
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", SOCIAL_INTENT_WECHAT_REQUEST);
        payload.put("requestId", requestId);
        payload.put("requesterUid", requesterUid);
        payload.put("targetUid", targetUid);
        payload.put("amount", safeAmount);
        payload.put("status", SOCIAL_INTENT_STATUS_PENDING);
        if (StringUtils.isNotBlank(safeMessage)) {
            payload.put("message", safeMessage);
        }
        if (StringUtils.isNotBlank(source)) {
            payload.put("source", StringUtils.left(source.trim(), 40));
        }

        ChatMessageEntity message = sendSocialIntentMessage(resolvedSessionId, requesterUid, targetUid, payload);
        RedisUtils.setCacheObject(SOCIAL_INTENT_STATE_KEY + requestId, SOCIAL_INTENT_STATUS_PENDING, Duration.ofDays(30));
        return AgentMessageAutoSendVo.builder()
                .messageId(message == null ? null : message.getId())
                .requestId(requestId)
                .targetUid(targetUid)
                .sessionId(resolvedSessionId)
                .content(StringUtils.defaultIfBlank(safeMessage, "申请交换微信"))
                .source(StringUtils.defaultIfBlank(source, "relationship_program"))
                .taskStatus("succeeded")
                .taskMessage("微信申请已发送")
                .approvalRequired(false)
                .build();
    }

    @DSTransactional
    public Map<String, Object> confirmWechat(AppUserEntity currentUser, ConfirmWechatIntentForm form) {
        String requestId = StringUtils.trimToEmpty(form.getRequestId());
        if (StringUtils.isBlank(requestId)) {
            throw new LinfengException("请求标识不能为空");
        }
        Integer requesterUid = form.getRequesterUid();
        if (requesterUid == null || requesterUid <= 0) {
            throw new LinfengException("发起人信息无效");
        }
        if (Objects.equals(requesterUid, currentUser.getUid())) {
            throw new LinfengException("不能确认自己的申请");
        }

        FriendEntity relation = getRelation(currentUser.getUid(), requesterUid);
        if (relation == null || relation.getSessionId() == null) {
            throw new LinfengException("会话不存在或已失效");
        }
        String sessionId = String.valueOf(relation.getSessionId());
        if (StringUtils.isNotBlank(form.getSessionId()) && !StringUtils.equals(sessionId, form.getSessionId())) {
            throw new LinfengException("会话信息不匹配");
        }

        ChatMessageEntity requestMessage = findWechatRequestMessage(sessionId, requesterUid, currentUser.getUid(), requestId);
        cn.hutool.json.JSONObject requestPayload = parseWechatRequestPayload(requestMessage == null ? "" : requestMessage.getContent());
        if (requestPayload == null) {
            throw new LinfengException("申请消息不存在或格式异常");
        }
        Integer trustedRequesterUid = requestPayload.getInt("requesterUid");
        if (trustedRequesterUid == null || !Objects.equals(trustedRequesterUid, requesterUid)) {
            throw new LinfengException("发起人信息校验失败");
        }
        Integer trustedTargetUid = requestPayload.getInt("targetUid");
        if (trustedTargetUid != null && trustedTargetUid > 0 && !Objects.equals(trustedTargetUid, currentUser.getUid())) {
            throw new LinfengException("当前用户无权处理该申请");
        }
        int amount = toInt(requestPayload.get("amount"), 0);
        if (amount < 0) {
            throw new LinfengException("诚意积分不能为负数");
        }
        if (amount > 5000) {
            throw new LinfengException("诚意积分超过上限");
        }

        String doneKey = SOCIAL_INTENT_DONE_KEY + requestId;
        String terminalStatus = resolveWechatTerminalStatus(sessionId, requestId);
        if (SOCIAL_INTENT_STATUS_AGREED.equals(terminalStatus)) {
            Map<String, Object> doneResult = RedisUtils.getCacheObject(doneKey);
            if (doneResult != null && !doneResult.isEmpty()) {
                return doneResult;
            }
            throw new LinfengException("该申请已确认，请勿重复操作");
        }
        if (SOCIAL_INTENT_STATUS_REJECTED.equals(terminalStatus)) {
            throw new LinfengException("该申请已被拒绝");
        }

        AppUserEntity requester = appUserService.getById(requesterUid);
        if (requester == null) {
            throw new LinfengException("发起人不存在");
        }
        AppUserEntity receiver = appUserService.getById(currentUser.getUid());
        if (receiver == null) {
            throw new LinfengException("当前用户不存在");
        }

        String wechatNo = resolveWechatNo(receiver, form.getWechatNo());
        if (StringUtils.isBlank(wechatNo)) {
            throw new LinfengException("请先填写你的微信号");
        }

        int feeAmount = 0;
        int arrivalAmount = amount;
        BigDecimal commissionRate = parseCommissionRate(configService.getValue(Constant.REWARD_COMMISSION_RATE));
        if (amount > 0 && commissionRate.compareTo(BigDecimal.ZERO) > 0) {
            feeAmount = BigDecimal.valueOf(amount)
                    .multiply(commissionRate)
                    .divide(new BigDecimal("100"), 0, RoundingMode.DOWN)
                    .intValue();
            if (feeAmount >= amount) {
                feeAmount = amount - 1;
            }
            arrivalAmount = amount - feeAmount;
        }

        if (amount > 0) {
            if (requester.getIntegral() < amount) {
                throw new LinfengException("发起人积分不足，无法完成确认");
            }
            boolean requesterUpdated = appUserService.lambdaUpdate()
                    .set(AppUserEntity::getIntegral, requester.getIntegral() - amount)
                    .eq(AppUserEntity::getUid, requesterUid)
                    .update();
            if (!requesterUpdated) {
                throw new LinfengException("扣减发起人积分失败");
            }
            boolean receiverUpdated = appUserService.lambdaUpdate()
                    .set(AppUserEntity::getIntegral, receiver.getIntegral() + arrivalAmount)
                    .eq(AppUserEntity::getUid, receiver.getUid())
                    .update();
            if (!receiverUpdated) {
                throw new LinfengException("增加接收方积分失败");
            }

            RedisUtils.deleteObject(RedisKeys.getUserCacheKey(requesterUid));
            RedisUtils.deleteObject(RedisKeys.getUserCacheKey(receiver.getUid()));
            userLevelService.checkUserLevel(requesterUid);
            userLevelService.checkUserLevel(receiver.getUid());

            String payerMark = feeAmount > 0
                    ? "申请微信确认扣除积分（含平台服务费" + feeAmount + "积分）"
                    : "申请微信确认扣除积分";
            String receiverMark = feeAmount > 0
                    ? "申请微信确认增加积分（已扣除平台服务费" + feeAmount + "积分）"
                    : "申请微信确认增加积分";
            billService.expend(
                    requesterUid,
                    BillDetailEnum.TYPE_20.getDesc(),
                    BillDetailEnum.CATEGORY_2.getValue(),
                    BillDetailEnum.TYPE_20.getValue(),
                    amount,
                    requester.getIntegral(),
                    payerMark,
                    requestId,
                    receiver.getUid()
            );
            billService.income(
                    receiver.getUid(),
                    BillDetailEnum.TYPE_19.getDesc(),
                    BillDetailEnum.CATEGORY_2.getValue(),
                    BillDetailEnum.TYPE_19.getValue(),
                    arrivalAmount,
                    receiver.getIntegral(),
                    receiverMark,
                    requestId,
                    requesterUid
            );
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", SOCIAL_INTENT_WECHAT_RESULT);
        payload.put("requestId", requestId);
        payload.put("status", SOCIAL_INTENT_STATUS_AGREED);
        payload.put("wechatNo", wechatNo);
        payload.put("amount", amount);
        payload.put("feeAmount", feeAmount);
        payload.put("arrivalAmount", arrivalAmount);

        sendSocialIntentMessage(sessionId, receiver.getUid(), requesterUid, payload);

        Map<String, Object> result = new HashMap<>();
        result.put("requestId", requestId);
        result.put("wechatNo", wechatNo);
        result.put("amount", amount);
        result.put("feeAmount", feeAmount);
        result.put("arrivalAmount", arrivalAmount);
        result.put("sessionId", sessionId);
        result.put("status", SOCIAL_INTENT_STATUS_AGREED);
        RedisUtils.setCacheObject(doneKey, result, Duration.ofDays(30));
        RedisUtils.setCacheObject(SOCIAL_INTENT_STATE_KEY + requestId, SOCIAL_INTENT_STATUS_AGREED, Duration.ofDays(90));
        log.info("wechat intent confirmed, requestId={}, requesterUid={}, receiverUid={}, amount={}, fee={}",
                requestId, requesterUid, receiver.getUid(), amount, feeAmount);
        return result;
    }

    @DSTransactional
    public Map<String, Object> rejectWechat(AppUserEntity currentUser, RejectWechatIntentForm form) {
        String requestId = StringUtils.trimToEmpty(form.getRequestId());
        if (StringUtils.isBlank(requestId)) {
            throw new LinfengException("请求标识不能为空");
        }
        Integer requesterUid = form.getRequesterUid();
        if (requesterUid == null || requesterUid <= 0) {
            throw new LinfengException("发起人信息无效");
        }
        if (Objects.equals(requesterUid, currentUser.getUid())) {
            throw new LinfengException("不能处理自己的申请");
        }

        FriendEntity relation = getRelation(currentUser.getUid(), requesterUid);
        if (relation == null || relation.getSessionId() == null) {
            throw new LinfengException("会话不存在或已失效");
        }
        String sessionId = String.valueOf(relation.getSessionId());
        if (StringUtils.isNotBlank(form.getSessionId()) && !StringUtils.equals(sessionId, form.getSessionId())) {
            throw new LinfengException("会话信息不匹配");
        }

        ChatMessageEntity requestMessage = findWechatRequestMessage(sessionId, requesterUid, currentUser.getUid(), requestId);
        cn.hutool.json.JSONObject requestPayload = parseWechatRequestPayload(requestMessage == null ? "" : requestMessage.getContent());
        if (requestPayload == null) {
            throw new LinfengException("申请消息不存在或格式异常");
        }
        Integer trustedRequesterUid = requestPayload.getInt("requesterUid");
        if (trustedRequesterUid == null || !Objects.equals(trustedRequesterUid, requesterUid)) {
            throw new LinfengException("发起人信息校验失败");
        }
        Integer trustedTargetUid = requestPayload.getInt("targetUid");
        if (trustedTargetUid != null && trustedTargetUid > 0 && !Objects.equals(trustedTargetUid, currentUser.getUid())) {
            throw new LinfengException("当前用户无权处理该申请");
        }

        String terminalStatus = resolveWechatTerminalStatus(sessionId, requestId);
        if (SOCIAL_INTENT_STATUS_AGREED.equals(terminalStatus)) {
            throw new LinfengException("该申请已确认，不能拒绝");
        }
        if (SOCIAL_INTENT_STATUS_REJECTED.equals(terminalStatus)) {
            return buildRejectResult(requestId, sessionId, StringUtils.trimToEmpty(form.getReason()));
        }

        String reason = StringUtils.left(StringUtils.trimToEmpty(form.getReason()), 80);
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", SOCIAL_INTENT_WECHAT_STATUS);
        payload.put("requestId", requestId);
        payload.put("status", SOCIAL_INTENT_STATUS_REJECTED);
        if (StringUtils.isNotBlank(reason)) {
            payload.put("reason", StringUtils.left(reason, 40));
        }

        sendSocialIntentMessage(sessionId, currentUser.getUid(), requesterUid, payload);
        RedisUtils.setCacheObject(SOCIAL_INTENT_STATE_KEY + requestId, SOCIAL_INTENT_STATUS_REJECTED, Duration.ofDays(90));

        Map<String, Object> result = buildRejectResult(requestId, sessionId, reason);
        log.info("wechat intent rejected, requestId={}, requesterUid={}, receiverUid={}",
                requestId, requesterUid, currentUser.getUid());
        return result;
    }

    @DSTransactional
    public Map<String, Object> confirmMatchRequest(AppUserEntity currentUser, String requestId) {
        HongniangMatchRequestEntity request = getPendingMatchRequest(currentUser, requestId);
        request.setRequestStatus(HongniangMatchCaseServiceImpl.MATCH_REQUEST_STATUS_ACCEPTED);
        request.setUpdateTime(new java.util.Date());
        matchRequestDao.updateById(request);
        updateCaseLatestRequestStatus(request.getCaseId(), request.getRequestStatus());
        sendMatchStatusMessage(request, SOCIAL_INTENT_STATUS_AGREED, null);
        appendMatchProgress(request, "用户已接受牵线申请");
        return buildMatchRequestResult(request, SOCIAL_INTENT_STATUS_AGREED, null);
    }

    @DSTransactional
    public Map<String, Object> rejectMatchRequest(AppUserEntity currentUser, String requestId, String reason) {
        HongniangMatchRequestEntity request = getPendingMatchRequest(currentUser, requestId);
        String safeReason = StringUtils.left(StringUtils.trimToEmpty(reason), 80);
        request.setRequestStatus(HongniangMatchCaseServiceImpl.MATCH_REQUEST_STATUS_REJECTED);
        request.setUpdateTime(new java.util.Date());
        matchRequestDao.updateById(request);
        updateCaseLatestRequestStatus(request.getCaseId(), request.getRequestStatus());
        sendMatchStatusMessage(request, SOCIAL_INTENT_STATUS_REJECTED, safeReason);
        appendMatchProgress(request, StringUtils.isBlank(safeReason) ? "用户已拒绝牵线申请" : "用户已拒绝牵线申请：" + safeReason);
        return buildMatchRequestResult(request, SOCIAL_INTENT_STATUS_REJECTED, safeReason);
    }

    private String resolveWechatNo(AppUserEntity receiver, String inputWechat) {
        String fromInput = StringUtils.trimToEmpty(inputWechat);
        if (StringUtils.isNotBlank(fromInput)) {
            return fromInput;
        }
        String fromInfo = parseWechatFromInfo(receiver.getInfo());
        return StringUtils.trimToEmpty(fromInfo);
    }

    private String parseWechatFromInfo(String info) {
        if (StringUtils.isBlank(info)) {
            return "";
        }
        try {
            JSONObject json = JSONObject.parseObject(info);
            if (json == null) {
                return "";
            }
            String wechat = json.getString("wechat");
            if (StringUtils.isBlank(wechat)) {
                wechat = json.getString("wechatNo");
            }
            if (StringUtils.isBlank(wechat)) {
                wechat = json.getString("wechatId");
            }
            if (StringUtils.isBlank(wechat)) {
                wechat = json.getString("weixin");
            }
            if (StringUtils.isBlank(wechat)) {
                wechat = json.getString("wx");
            }
            return StringUtils.trimToEmpty(wechat);
        } catch (Exception ignored) {
            return "";
        }
    }

    private cn.hutool.json.JSONObject parseWechatRequestPayload(String content) {
        cn.hutool.json.JSONObject payload = parseSocialIntentPayload(content);
        if (payload == null) {
            return null;
        }
        if (!SOCIAL_INTENT_WECHAT_REQUEST.equals(payload.getStr("type"))) {
            return null;
        }
        return payload;
    }

    private cn.hutool.json.JSONObject parseSocialIntentPayload(String content) {
        if (StringUtils.isBlank(content) || !content.startsWith(SOCIAL_INTENT_PREFIX)) {
            return null;
        }
        try {
            return JSONUtil.parseObj(content.substring(SOCIAL_INTENT_PREFIX.length()));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveWechatTerminalStatus(String sessionId, String requestId) {
        String cachedStatus = normalizeIntentStatus(RedisUtils.getCacheObject(SOCIAL_INTENT_STATE_KEY + requestId));
        if (isTerminalStatus(cachedStatus)) {
            return cachedStatus;
        }
        List<ChatMessageEntity> relatedMessages = chatMessageService.lambdaQuery()
                .eq(ChatMessageEntity::getSessionId, sessionId)
                .eq(ChatMessageEntity::getMessageType, "text")
                .like(ChatMessageEntity::getContent, "\"requestId\":\"" + requestId + "\"")
                .orderByDesc(ChatMessageEntity::getId)
                .last("limit 30")
                .list();
        if (relatedMessages == null || relatedMessages.isEmpty()) {
            return SOCIAL_INTENT_STATUS_PENDING;
        }
        for (ChatMessageEntity relatedMessage : relatedMessages) {
            cn.hutool.json.JSONObject payload = parseSocialIntentPayload(relatedMessage.getContent());
            if (payload == null) {
                continue;
            }
            if (!StringUtils.equals(requestId, StringUtils.trimToEmpty(payload.getStr("requestId")))) {
                continue;
            }
            String type = StringUtils.trimToEmpty(payload.getStr("type"));
            if (SOCIAL_INTENT_WECHAT_RESULT.equals(type)) {
                return SOCIAL_INTENT_STATUS_AGREED;
            }
            if (SOCIAL_INTENT_WECHAT_STATUS.equals(type)) {
                String status = normalizeIntentStatus(payload.get("status"));
                if (isTerminalStatus(status)) {
                    return status;
                }
            }
        }
        return SOCIAL_INTENT_STATUS_PENDING;
    }

    private FriendEntity getRelation(Integer myUid, Integer friendUid) {
        return friendService.lambdaQuery()
                .eq(FriendEntity::getMyId, myUid)
                .eq(FriendEntity::getFriendId, friendUid)
                .one();
    }

    private ChatMessageEntity findWechatRequestMessage(String sessionId, Integer requesterUid, Integer targetUid, String requestId) {
        return chatMessageService.lambdaQuery()
                .eq(ChatMessageEntity::getSessionId, sessionId)
                .eq(ChatMessageEntity::getSenderId, String.valueOf(requesterUid))
                .eq(ChatMessageEntity::getReceiverId, String.valueOf(targetUid))
                .eq(ChatMessageEntity::getMessageType, "text")
                .like(ChatMessageEntity::getContent, "\"requestId\":\"" + requestId + "\"")
                .orderByDesc(ChatMessageEntity::getId)
                .last("limit 1")
                .one();
    }

    private Map<String, Object> buildRejectResult(String requestId, String sessionId, String reason) {
        Map<String, Object> result = new HashMap<>();
        result.put("requestId", requestId);
        result.put("sessionId", sessionId);
        result.put("status", SOCIAL_INTENT_STATUS_REJECTED);
        if (StringUtils.isNotBlank(reason)) {
            result.put("reason", reason);
        }
        return result;
    }

    private String normalizeIntentStatus(Object status) {
        String normalized = StringUtils.lowerCase(StringUtils.trimToEmpty(String.valueOf(status)));
        if (StringUtils.isBlank(normalized)) {
            return SOCIAL_INTENT_STATUS_PENDING;
        }
        if (SOCIAL_INTENT_STATUS_AGREED.equals(normalized)) {
            return SOCIAL_INTENT_STATUS_AGREED;
        }
        if (SOCIAL_INTENT_STATUS_REJECTED.equals(normalized)) {
            return SOCIAL_INTENT_STATUS_REJECTED;
        }
        return SOCIAL_INTENT_STATUS_PENDING;
    }

    private boolean isTerminalStatus(String status) {
        return SOCIAL_INTENT_STATUS_AGREED.equals(status) || SOCIAL_INTENT_STATUS_REJECTED.equals(status);
    }

    private ChatMessageEntity sendSocialIntentMessage(String sessionId, Integer senderUid, Integer receiverUid, Map<String, Object> payload) {
        return socialIntentMessageService.sendStructuredMessage(sessionId, senderUid, receiverUid, payload);
    }

    private int toInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private BigDecimal parseCommissionRate(String configValue) {
        if (StringUtils.isBlank(configValue)) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal rate = new BigDecimal(configValue.trim());
            if (rate.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.ZERO;
            }
            if (rate.compareTo(new BigDecimal("100")) > 0) {
                return new BigDecimal("100");
            }
            return rate;
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private HongniangMatchRequestEntity getPendingMatchRequest(AppUserEntity currentUser, String requestId) {
        String safeRequestId = StringUtils.trimToEmpty(requestId);
        if (StringUtils.isBlank(safeRequestId)) {
            throw new LinfengException("请求标识不能为空");
        }
        HongniangMatchRequestEntity request = matchRequestDao.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HongniangMatchRequestEntity>()
            .eq(HongniangMatchRequestEntity::getIntentRequestId, safeRequestId)
            .last("limit 1"));
        if (request == null) {
            throw new LinfengException("牵线申请不存在");
        }
        if (!Objects.equals(request.getToUserId(), currentUser.getUid())) {
            throw new LinfengException("当前用户无权处理该申请");
        }
        if (request.getExpireTime() != null && request.getExpireTime().before(new java.util.Date())) {
            request.setRequestStatus(HongniangMatchCaseServiceImpl.MATCH_REQUEST_STATUS_EXPIRED);
            request.setUpdateTime(new java.util.Date());
            matchRequestDao.updateById(request);
            updateCaseLatestRequestStatus(request.getCaseId(), request.getRequestStatus());
            throw new LinfengException("牵线申请已过期");
        }
        if (Objects.equals(request.getRequestStatus(), HongniangMatchCaseServiceImpl.MATCH_REQUEST_STATUS_ACCEPTED)) {
            throw new LinfengException("该申请已接受");
        }
        if (Objects.equals(request.getRequestStatus(), HongniangMatchCaseServiceImpl.MATCH_REQUEST_STATUS_REJECTED)) {
            throw new LinfengException("该申请已拒绝");
        }
        if (Objects.equals(request.getRequestStatus(), HongniangMatchCaseServiceImpl.MATCH_REQUEST_STATUS_EXPIRED)) {
            throw new LinfengException("牵线申请已过期");
        }
        return request;
    }

    private void updateCaseLatestRequestStatus(Integer caseId, Integer requestStatus) {
        HongniangMatchCaseEntity matchCase = matchCaseDao.selectById(caseId);
        if (matchCase == null) {
            return;
        }
        matchCase.setLatestRequestStatus(requestStatus);
        matchCase.setUpdateTime(new java.util.Date());
        matchCaseDao.updateById(matchCase);
    }

    private void sendMatchStatusMessage(HongniangMatchRequestEntity request, String status, String reason) {
        Long relationSessionId = friendService.getOrCreateSession(request.getFromUserId(), request.getToUserId());
        String sessionId = String.valueOf(relationSessionId);
        HongniangInfoEntity hongniang = hongniangService.getById(request.getHongniangId());
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", SOCIAL_INTENT_MATCH_STATUS);
        payload.put("requestId", request.getIntentRequestId());
        payload.put("caseId", request.getCaseId());
        payload.put("hongniangId", request.getHongniangId());
        payload.put("hongniangName", hongniang == null ? "" : hongniang.getHongniangName());
        payload.put("requesterUid", request.getFromUserId());
        payload.put("targetUid", request.getToUserId());
        payload.put("status", status);
        if (StringUtils.isNotBlank(reason)) {
            payload.put("reason", reason);
        }
        sendSocialIntentMessage(sessionId, request.getToUserId(), request.getFromUserId(), payload);
    }

    private void appendMatchProgress(HongniangMatchRequestEntity request, String content) {
        HongniangMatchCaseEntity matchCase = matchCaseDao.selectById(request.getCaseId());
        if (matchCase == null) {
            return;
        }
        HongniangMatchProgressEntity progress = new HongniangMatchProgressEntity();
        progress.setCaseId(request.getCaseId());
        progress.setProgressType(3);
        progress.setStageBefore(matchCase.getCurrentStage());
        progress.setStageAfter(matchCase.getCurrentStage());
        progress.setContent(content);
        progress.setOperatorId(Long.valueOf(request.getToUserId()));
        progress.setActualFollowTime(new java.util.Date());
        progress.setCreateTime(new java.util.Date());
        matchProgressDao.insert(progress);
    }

    private Map<String, Object> buildMatchRequestResult(HongniangMatchRequestEntity request, String status, String reason) {
        Map<String, Object> result = new HashMap<>();
        result.put("requestId", request.getIntentRequestId());
        result.put("caseId", request.getCaseId());
        result.put("status", status);
        if (StringUtils.isNotBlank(reason)) {
            result.put("reason", reason);
        }
        return result;
    }
}
