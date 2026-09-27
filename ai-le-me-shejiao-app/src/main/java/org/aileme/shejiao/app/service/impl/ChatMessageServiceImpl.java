package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.aileme.common.redis.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.api.service.ChatMessageService;
import org.aileme.shejiao.app.dao.ChatMessageDao;
import org.aileme.shejiao.app.dao.FriendDao;
import org.aileme.shejiao.app.service.agent.AgentAutonomyService;
import org.aileme.shejiao.app.websocket.constant.MessageConstant;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.ChatMessageEntity;
import org.aileme.shejiao.domain.param.app.GetChatListForm;

import java.util.Map;

/**
 * 聊天消息服务实现类
 * @author JL.Yu
 */
@DS("master")
@Service("chatMessageService")
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageDao, ChatMessageEntity> implements ChatMessageService {
    private static final String SOCIAL_INTENT_PREFIX = "__SOCIAL_INTENT__:";
    private static final String SOCIAL_INTENT_WECHAT_REQUEST = "wechat_request";
    private static final String SOCIAL_INTENT_WECHAT_RESULT = "wechat_result";
    private static final String SOCIAL_INTENT_WECHAT_STATUS = "wechat_status";
    private static final String SOCIAL_INTENT_GIFT_TIP = "gift_tip";

    @Autowired
    private FriendDao friendDao;

    @Resource
    private ChatMessageDao chatMessageDao;

    @Autowired(required = false)
    private AgentAutonomyService agentAutonomyService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ChatMessageEntity> page = this.page(
                new Query<ChatMessageEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    @DSTransactional
    public void saveMessage(ChatMessageEntity chatMessage) {
        saveMessage(chatMessage, false);
    }

    @Override
    @DSTransactional
    public void saveMessage(ChatMessageEntity chatMessage, boolean skipAutonomy) {
        this.save(chatMessage);
        // 更新好友表的最新消息和未读数
        String lastMessage = resolveLastMessagePreview(chatMessage);
        friendDao.updateInfo(chatMessage.getSessionId(), lastMessage);
        if (!skipAutonomy && agentAutonomyService != null) {
            agentAutonomyService.ingestChatMessageEvent(chatMessage);
        }
    }

    public String resolveLastMessagePreview(ChatMessageEntity chatMessage) {
        switch (chatMessage.getMessageType()) {
            case "image":
                return "【图片】";
            case "file":
                return "【文件】";
            case "video":
                return "【视频】";
            default:
                String content = chatMessage.getContent();
                if (content != null && content.startsWith(SOCIAL_INTENT_PREFIX)) {
                    try {
                        JSONObject payload = JSONUtil.parseObj(content.substring(SOCIAL_INTENT_PREFIX.length()));
                        String type = payload.getStr("type");
                        if (SOCIAL_INTENT_WECHAT_REQUEST.equals(type)) {
                            return "【申请微信】";
                        }
                        if (SOCIAL_INTENT_WECHAT_RESULT.equals(type)) {
                            if ("rejected".equalsIgnoreCase(payload.getStr("status"))) {
                                return "【微信申请已拒绝】";
                            }
                            return "【微信号已发送】";
                        }
                        if (SOCIAL_INTENT_WECHAT_STATUS.equals(type)) {
                            String status = payload.getStr("status");
                            if ("rejected".equalsIgnoreCase(status)) {
                                return "【微信申请已拒绝】";
                            }
                            if ("agreed".equalsIgnoreCase(status)) {
                                return "【微信申请已同意】";
                            }
                            return "【申请微信】";
                        }
                        if (SOCIAL_INTENT_GIFT_TIP.equals(type)) {
                            String giftStatus = payload.getStr("giftStatus");
                            if ("rejected".equalsIgnoreCase(giftStatus)) {
                                return "【礼物已婉拒】";
                            }
                            if ("accepted".equalsIgnoreCase(giftStatus)) {
                                return "【礼物已接受】";
                            }
                            return "【礼物申请】";
                        }
                        return "【社交确认】";
                    } catch (Exception ignored) {
                        return "【社交消息】";
                    }
                }
                return content;
        }
    }

    @Override
    public void refreshSessionLastMessage(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }
        ChatMessageEntity latestMessage = this.getOne(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, sessionId)
                .eq(ChatMessageEntity::getIsWithdrawn, 0)
                .orderByDesc(ChatMessageEntity::getId)
                .last("limit 1"));
        String lastMessage = latestMessage == null ? "" : resolveLastMessagePreview(latestMessage);
        friendDao.updateWithdrawInfo(sessionId, lastMessage);
    }

    @Override
    @DSTransactional
    public void withdrawMessage(String messageId) {
        ChatMessageEntity message = this.getById(messageId);
        chatMessageDao.withdrawMessage(messageId);
        RedisUtils.deleteObject(MessageConstant.IM_CHAT+message.getSessionId());

        // 更新好友表最新消息展示
        if (message != null) {
            friendDao.updateWithdrawInfo(message.getSessionId(), "撤回了一条消息");
        }

    }

    @Override
    public IPage<ChatMessageEntity> getChatMessageList(GetChatListForm param) {
        Page<ChatMessageEntity> page = new Page<>(param.getPageNum(), param.getPageSize());
        LambdaQueryWrapper<ChatMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageEntity::getSessionId, param.getSessionId());
        // 加载历史消息时，只查比 lastMessageId 更早的记录
        if (param.getLastMessageId() != null && !"0".equals(param.getLastMessageId()) && !"".equals(param.getLastMessageId())) {
            wrapper.lt(ChatMessageEntity::getId, Integer.parseInt(param.getLastMessageId()));
        }
        // 按 id 降序，返回最新的消息在前面（前端会 reverse）
        wrapper.orderByDesc(ChatMessageEntity::getId);
        return this.page(page, wrapper);
    }

    @Override
    public void deleteChatMessage(Integer friendId, Integer myId) {
        LambdaQueryWrapper<ChatMessageEntity> wrapper1=new LambdaQueryWrapper<>();
        wrapper1.eq(ChatMessageEntity::getSenderId,friendId);
        wrapper1.eq(ChatMessageEntity::getReceiverId,myId);
        this.remove(wrapper1);
        LambdaQueryWrapper<ChatMessageEntity> wrapper2=new LambdaQueryWrapper<>();
        wrapper2.eq(ChatMessageEntity::getSenderId,myId);
        wrapper2.eq(ChatMessageEntity::getReceiverId,friendId);
        this.remove(wrapper2);
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        LambdaQueryWrapper<ChatMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageEntity::getSessionId, sessionId);
        this.remove(wrapper);
    }

}
