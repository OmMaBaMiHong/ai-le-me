/**
 * -----------------------------------
 *  Copyright (c) 2021-2023

 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.app.ChatMessageEntity;
import org.aileme.shejiao.domain.param.app.ClearChatMessageUnreadForm;
import org.aileme.shejiao.domain.param.app.GetChatListForm;

import java.util.Map;

/**
 *
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-11-16 14:05:06
 */
public interface ChatMessageService extends IService<ChatMessageEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveMessage(ChatMessageEntity chatMessage);

    default void saveMessage(ChatMessageEntity chatMessage, boolean skipAutonomy) {
        saveMessage(chatMessage);
    }

    void withdrawMessage(String messageId);

    IPage<ChatMessageEntity> getChatMessageList(GetChatListForm param);

    void refreshSessionLastMessage(String sessionId);

    void deleteChatMessage(Integer friendId, Integer myId);

    void deleteBySessionId(String sessionId);
}
