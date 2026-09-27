package org.aileme.shejiao.app.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.ChatMessageService;
import org.aileme.shejiao.app.websocket.component.SocketServer;
import org.aileme.shejiao.app.websocket.constant.MessageConstant;
import org.aileme.shejiao.app.websocket.entity.SocketMessage;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.DateUtil;
import org.aileme.shejiao.domain.entity.app.ChatMessageEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@DS("master")
@Service
public class SocialIntentMessageService {

    public static final String SOCIAL_INTENT_PREFIX = "__SOCIAL_INTENT__:";
    private static final int MAX_CHAT_MESSAGE_CONTENT_LENGTH = 255;

    @Autowired
    private ChatMessageService chatMessageService;

    public ChatMessageEntity sendStructuredMessage(String sessionId,
                                                   Integer senderUid,
                                                   Integer receiverUid,
                                                   Map<String, Object> payload) {
        String safeSessionId = StringUtils.trimToEmpty(sessionId);
        if (StringUtils.isBlank(safeSessionId)) {
            throw new LinfengException("会话不存在或已失效");
        }
        if (senderUid == null || senderUid <= 0 || receiverUid == null || receiverUid <= 0) {
            throw new LinfengException("发送目标不存在");
        }
        String content = SOCIAL_INTENT_PREFIX + JSONUtil.toJsonStr(payload);
        if (content.length() > MAX_CHAT_MESSAGE_CONTENT_LENGTH) {
            throw new LinfengException("社交消息过长，无法写入聊天记录");
        }

        ChatMessageEntity chatMessage = ChatMessageEntity.builder()
                .sessionId(safeSessionId)
                .senderId(String.valueOf(senderUid))
                .receiverId(String.valueOf(receiverUid))
                .sendTime(DateUtil.nowDateTimeStr())
                .content(content)
                .messageType("text")
                .isWithdrawn(0)
                .updateTime(DateUtil.nowDateTime())
                .build();
        chatMessageService.saveMessage(chatMessage, true);

        SocketMessage<ChatMessageEntity> socketMessage = new SocketMessage<>(MessageConstant.PERSON_MESSAGE, chatMessage);
        SocketServer.sendToUser(String.valueOf(receiverUid), socketMessage);
        SocketServer.sendToUser(String.valueOf(senderUid), socketMessage);
        log.info("social intent message sent, sessionId={}, senderUid={}, receiverUid={}, type={}",
                safeSessionId, senderUid, receiverUid, payload == null ? "" : payload.get("type"));
        return chatMessage;
    }

    public void syncGiftTipMessageState(String sessionId,
                                        String requestId,
                                        String giftStatus,
                                        String taskStatus) {
        String safeSessionId = StringUtils.trimToEmpty(sessionId);
        String safeRequestId = StringUtils.trimToEmpty(requestId);
        if (StringUtils.isBlank(safeSessionId) || StringUtils.isBlank(safeRequestId)) {
            return;
        }
        List<ChatMessageEntity> candidates = chatMessageService.list(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, safeSessionId)
                .eq(ChatMessageEntity::getMessageType, "text")
                .like(ChatMessageEntity::getContent, safeRequestId));
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        List<ChatMessageEntity> changedMessages = new ArrayList<>();
        for (ChatMessageEntity item : candidates) {
            String content = item == null ? "" : item.getContent();
            if (StringUtils.isBlank(content) || !content.startsWith(SOCIAL_INTENT_PREFIX)) {
                continue;
            }
            try {
                cn.hutool.json.JSONObject payload = JSONUtil.parseObj(content.substring(SOCIAL_INTENT_PREFIX.length()));
                if (!"gift_tip".equalsIgnoreCase(payload.getStr("type"))) {
                    continue;
                }
                if (!StringUtils.equals(safeRequestId, payload.getStr("requestId"))) {
                    continue;
                }
                boolean changed = false;
                if (StringUtils.isNotBlank(giftStatus) && !StringUtils.equals(payload.getStr("giftStatus"), giftStatus)) {
                    payload.set("giftStatus", giftStatus);
                    changed = true;
                }
                if (StringUtils.isNotBlank(taskStatus) && !StringUtils.equals(payload.getStr("taskStatus"), taskStatus)) {
                    payload.set("taskStatus", taskStatus);
                    changed = true;
                }
                if (!changed) {
                    continue;
                }
                item.setContent(SOCIAL_INTENT_PREFIX + payload.toString());
                item.setUpdateTime(new Date());
                changedMessages.add(item);
            } catch (Exception ex) {
                log.warn("sync gift tip message failed to parse payload. sessionId={}, requestId={}, messageId={}",
                        safeSessionId, safeRequestId, item.getId());
            }
        }
        if (changedMessages.isEmpty()) {
            return;
        }
        chatMessageService.updateBatchById(changedMessages);
        chatMessageService.refreshSessionLastMessage(safeSessionId);
    }
}
