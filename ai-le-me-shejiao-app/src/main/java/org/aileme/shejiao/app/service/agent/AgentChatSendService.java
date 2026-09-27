package org.aileme.shejiao.app.service.agent;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.ChatMessageService;
import org.aileme.shejiao.api.service.FriendService;
import org.aileme.shejiao.app.websocket.component.SocketServer;
import org.aileme.shejiao.app.websocket.constant.MessageConstant;
import org.aileme.shejiao.app.websocket.entity.SocketMessage;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.DateUtil;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.ChatMessageEntity;
import org.aileme.shejiao.domain.param.app.AgentMessageAutoSendForm;
import org.aileme.shejiao.domain.vo.AgentMessageAutoSendVo;

@Service
public class AgentChatSendService {

    private static final int MAX_CHAT_MESSAGE_CONTENT_LENGTH = 255;

    @Autowired
    private FriendService friendService;

    @Autowired
    private ChatMessageService chatMessageService;

    public AgentMessageAutoSendVo sendText(AppUserEntity currentUser, AgentMessageAutoSendForm form) {
        Integer senderUid = currentUser == null ? null : currentUser.getUid();
        Integer targetUid = form == null ? null : form.getTargetUid();
        if (senderUid == null || senderUid <= 0) {
            throw new LinfengException("发送用户不存在");
        }
        if (targetUid == null || targetUid <= 0 || senderUid.equals(targetUid)) {
            throw new LinfengException("发送目标不存在");
        }
        String content = StringUtils.trimToEmpty(form.getContent());
        if (StringUtils.isBlank(content)) {
            throw new LinfengException("消息内容不能为空");
        }
        if (content.length() > MAX_CHAT_MESSAGE_CONTENT_LENGTH) {
            throw new LinfengException("消息内容过长，请精简后重试");
        }

        Long relationSessionId = friendService.getOrCreateSession(senderUid, targetUid);
        String sessionId = String.valueOf(relationSessionId);
        if (StringUtils.isNotBlank(form.getSessionId()) && !StringUtils.equals(form.getSessionId(), sessionId)) {
            throw new LinfengException("会话已变更，请刷新后重试");
        }

        ChatMessageEntity chatMessage = ChatMessageEntity.builder()
                .sessionId(sessionId)
                .senderId(String.valueOf(senderUid))
                .receiverId(String.valueOf(targetUid))
                .sendTime(DateUtil.nowDateTimeStr())
                .content(content)
                .messageType("text")
                .isWithdrawn(0)
                .updateTime(DateUtil.nowDateTime())
                .build();
        chatMessageService.saveMessage(chatMessage, true);

        SocketMessage<ChatMessageEntity> socketMessage = new SocketMessage<>(MessageConstant.PERSON_MESSAGE, chatMessage);
        SocketServer.sendToUser(String.valueOf(targetUid), socketMessage);
        SocketServer.sendToUser(String.valueOf(senderUid), socketMessage);

        return AgentMessageAutoSendVo.builder()
                .messageId(chatMessage.getId())
                .targetUid(targetUid)
                .sessionId(sessionId)
                .content(content)
                .source(StringUtils.trimToEmpty(form.getSource()))
                .taskStatus(AgentGovernanceConstants.TASK_STATUS_SUCCEEDED)
                .taskMessage("消息已发送")
                .approvalRequired(false)
                .build();
    }
}
