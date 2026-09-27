package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.ActivityChatGroupEntity;

import java.util.List;
import java.util.Map;

public interface ActivityChatGroupService extends IService<ActivityChatGroupEntity> {

    Map<String, Object> getGroupMeta(Integer activityId, AppUserEntity user);

    Map<String, Object> createOrOpenGroup(Integer activityId, AppUserEntity user);

    List<Map<String, Object>> getUserSessionList(AppUserEntity user);

    List<Map<String, Object>> getMessageList(Integer groupId, AppUserEntity user, Long lastMessageId, Integer pageSize);

    Map<String, Object> sendMessage(Integer groupId, AppUserEntity user, String content, String messageType);

    void syncApprovedMembers(Integer activityId);
}
