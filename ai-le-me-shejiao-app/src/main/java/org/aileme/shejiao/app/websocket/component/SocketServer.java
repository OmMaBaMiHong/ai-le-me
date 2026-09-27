/**
 * -----------------------------------
 * Copyright (c) 2021-2023
 * <p>
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.websocket.component;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.common.utils.DateUtil;
import org.aileme.shejiao.common.utils.SnowFlakeUtil;
import org.aileme.shejiao.domain.entity.app.ChatMessageEntity;
import org.aileme.shejiao.domain.entity.app.FriendEntity;
import org.aileme.shejiao.domain.entity.app.NoticeEntity;
import org.aileme.shejiao.api.service.ChatMessageService;
import org.aileme.shejiao.api.service.FriendService;
import org.aileme.shejiao.api.service.NoticeService;
import org.aileme.shejiao.common.utils.JwtUtils;
import org.aileme.shejiao.app.websocket.constant.MessageConstant;
import org.aileme.shejiao.app.websocket.entity.SocketMessage;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息通信核心业务模块
 *
 * @author linfeng
 * @date 2022/11/16 11:08
 */
@Slf4j
@Component
@ServerEndpoint("/app/socket/{token}")
public class SocketServer {

    public static Map<String, Session> userSessionMap = new ConcurrentHashMap<>();

    public static JwtUtils jwtUtils;

    public static ChatMessageService chatMessageService;

    public static NoticeService noticeService;

    public static FriendService friendService;

    @Autowired
    public void setChatMessageService(ChatMessageService chatMessageService) {
        SocketServer.chatMessageService = chatMessageService;
    }

    @Autowired
    public void setFriendService(FriendService friendService) {
        SocketServer.friendService = friendService;
    }

    @Autowired
    public void setJwtUtils(JwtUtils jwtUtils) {
        SocketServer.jwtUtils = jwtUtils;
    }

    @Autowired
    public void setNoticeService(NoticeService noticeService) {
        SocketServer.noticeService = noticeService;
    }


    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        String id;
        try {
            id = jwtUtils.validateTokenAndExtUserId(token);
        } catch (Exception e) {
            this.sendMessage(new SocketMessage<>(MessageConstant.TOKEN_FAILED, null), session);
            return;
        }
        userSessionMap.put(id, session);
        log.info("用户:{}上线", id);
        this.sendToAll(new SocketMessage<>(MessageConstant.COUNT, userSessionMap.keySet().toArray()));
    }

    @OnClose
    public void onClose(Session session, @PathParam("token") String token) {

        String id = jwtUtils.validateTokenAndExtUserId(token);
        if(id==null){
            return;
        }
        userSessionMap.remove(id);
        log.info("用户:{}离线(@OnClose，连接正常关闭)", id);
        this.sendToAll(new SocketMessage<>(MessageConstant.COUNT, userSessionMap.keySet().toArray()));

    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("token") String token) {
        String id = jwtUtils.validateTokenAndExtUserId(token);
        if(id==null){
            return;
        }
        userSessionMap.remove(id);
        log.error("用户:{}离线(@OnError，连接异常)，错误信息: {}", id, error.getMessage(), error);
        this.sendToAll(new SocketMessage<>(MessageConstant.COUNT, userSessionMap.keySet().toArray()));

    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("token") String token) {
      try {
        JSONObject msg = JSONUtil.parseObj(message);
        JSONObject data = msg.getJSONObject("data");
        switch (msg.getStr("type")) {
            /*接收心跳**/
            case MessageConstant.PING:
                break;
            /*私聊消息**/
            case MessageConstant.PERSON_MESSAGE:
                ChatMessageEntity chatMessage = ChatMessageEntity.builder()
                        .sessionId(data.getStr("sessionId"))
                        .senderId(data.getStr("senderId"))
                        .receiverId(data.getStr("receiverId"))
                        .sendTime(data.getStr("sendTime"))
                        .content(data.getStr("content"))
                        .messageType(data.getStr("messageType"))
                        .isWithdrawn(0)
                        .updateTime(DateUtil.nowDateTime())
                        .build();
                chatMessageService.saveMessage(chatMessage);
                this.sendToUserById(chatMessage.getSenderId(), new SocketMessage<>(MessageConstant.PERSON_MESSAGE, chatMessage));
                this.sendToUserById(chatMessage.getReceiverId(), new SocketMessage<>(MessageConstant.PERSON_MESSAGE, chatMessage));
                break;
            /*私聊撤回消息**/
            case MessageConstant.PERSON_WITHDRAW:
                String messageId = data.getStr("messageId");
                String senderId = data.getStr("senderId");
                String receiverId = data.getStr("receiverId");
                chatMessageService.withdrawMessage(messageId);
                this.sendToUserById(senderId, new SocketMessage<>(MessageConstant.PERSON_WITHDRAW, data));
                this.sendToUserById(receiverId, new SocketMessage<>(MessageConstant.PERSON_WITHDRAW, data));
                break;
            /*申请好友消息**/
            case MessageConstant.PERSON_APPLY:
                NoticeEntity notice = NoticeEntity.builder()
                        .senderId(data.getInt("senderId"))
                        .receiverId(data.getInt("receiverId"))
                        .type(MessageConstant.PERSON_APPLY)
                        .information(msg.getStr("data"))
                        .isRead(false)
                        .createTime(DateUtil.nowDateTime())
                        .updateTime(DateUtil.nowDateTime())
                        .build();
                noticeService.save(notice);
                this.sendToUserById(data.getStr("receiverId"), new SocketMessage<>(MessageConstant.PERSON_APPLY, msg.getStr("data")));
                break;
            /*申请好友通过消息**/
            case MessageConstant.PERSON_APPLY_AGREE:
                NoticeEntity noticeInfo = noticeService.getById(data.getStr("id"));
                JSONObject jsonObject = JSONUtil.parseObj(noticeInfo.getInformation());
                Boolean isFriend = friendService.checkIsFriend(noticeInfo.getReceiverId().intValue(), noticeInfo.getSenderId().intValue());
                if (isFriend) {
                    noticeInfo.setIsRead(true);
                    noticeService.saveOrUpdate(noticeInfo);
                    this.sendToUserById(noticeInfo.getReceiverId().toString(), new SocketMessage<>(MessageConstant.NOTICE_REFRESH, "TA已经是你的好友啦"));
                    break;
                }
                Long sessionId = Math.abs(SnowFlakeUtil.getSnowFlakeId());
                FriendEntity friend1 = FriendEntity.builder()
                        .myId(noticeInfo.getSenderId())
                        .friendId(noticeInfo.getReceiverId())
                        .notation(jsonObject.getStr("notation"))
                        .sessionId(sessionId)
                        .lastMessage(MessageConstant.DEFAULT_LAST_MESSAGE)
                        .unread(MessageConstant.NOT_READ)
                        .isHidden(false)
                        .createTime(DateUtil.nowDateTime())
                        .updateTime(DateUtil.nowDateTime())
                        .build();
                FriendEntity friend2 = FriendEntity.builder()
                        .myId(noticeInfo.getReceiverId())
                        .friendId(noticeInfo.getSenderId())
                        .notation(jsonObject.getStr("senderName"))
                        .sessionId(sessionId)
                        .lastMessage(MessageConstant.DEFAULT_LAST_MESSAGE)
                        .unread(MessageConstant.NOT_READ)
                        .isHidden(false)
                        .createTime(DateUtil.nowDateTime())
                        .updateTime(DateUtil.nowDateTime())
                        .build();
                friendService.save(friend1);
                friendService.save(friend2);
                noticeInfo.setIsRead(true);
                noticeService.saveOrUpdate(noticeInfo);
                SocketMessage<FriendEntity> socketMessage = new SocketMessage<>(MessageConstant.PERSON_APPLY_AGREE, friend2);
                this.sendToUserById(noticeInfo.getSenderId().toString(), socketMessage);
                this.sendToUserById(noticeInfo.getReceiverId().toString(), socketMessage);
                break;

        }
      } catch (Exception e) {
          String id = "unknown";
          try {
              id = jwtUtils.validateTokenAndExtUserId(token);
          } catch (Exception ignored) {}
          log.error("用户:{} 消息处理异常，message={}，error={}", id, message, e.getMessage(), e);
      }
    }

    private void sendToAll(SocketMessage<?> message) {
        for (Session session : userSessionMap.values()) {
            this.sendMessage(message, session);
        }
    }

    private void sendToUserById(String id, SocketMessage<?> message) {
        for (String key : userSessionMap.keySet()) {
            if (Objects.equals(key, id)) {
                this.sendMessage(message, userSessionMap.get(key));
                return;
            }
        }
    }

    /**
     * 发送消息给指定用户（公共静态方法，供其他服务调用）
     * @param uid 用户ID
     * @param message 消息内容
     * @return 是否发送成功
     */
    public static boolean sendToUser(String uid, SocketMessage<?> message) {
        Session session = userSessionMap.get(uid);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.getBasicRemote().sendText(JSONUtil.toJsonStr(message));
                }
                return true;
            } catch (Exception e) {
                log.error("发送消息失败: uid={}, error={}", uid, e.getMessage());
            }
        }
        return false;
    }

    private void sendMessage(SocketMessage<?> message, Session toSession) {
        try {
            if (toSession.isOpen()) {
                synchronized (toSession) {
//                    toSession.getAsyncRemote().sendText(JSONUtil.toJsonStr(message));
                    toSession.getBasicRemote().sendText(JSONUtil.toJsonStr(message));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
