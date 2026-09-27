/**
 * -----------------------------------
 *  Copyright (c) 2021-2023

 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.api.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.FriendEntity;
import org.aileme.shejiao.domain.param.app.ClearChatMessageUnreadForm;
import org.aileme.shejiao.domain.param.app.DeleteFriendForm;

import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-11-16 14:05:06
 */
public interface FriendService extends IService<FriendEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<JSONObject> getFriendList(Integer uid);

    void clearUnread(ClearChatMessageUnreadForm param);

    Boolean checkIsFriend(Integer uid, Integer uid1);

    void doFirendsEach(AppUserEntity user, long longValue);

    void removeFriends(Integer uid, Integer id);

    void deleteFriend(Integer friendId, Integer myId);

    void applyFriend(Object data);

    void agreePersonApply(Integer id);

    void deleteBySessionId(String sessionId);

    /**
     * 获取或创建两个用户之间的会话
     * 如果已有好友关系则返回已有 sessionId，否则创建双向好友记录
     */
    Long getOrCreateSession(Integer myId, Integer friendId);

    /**
     * 将已有隐藏会话转成正式好友关系
     */
    Long activateFriendship(Integer myId, Integer friendId);
}
