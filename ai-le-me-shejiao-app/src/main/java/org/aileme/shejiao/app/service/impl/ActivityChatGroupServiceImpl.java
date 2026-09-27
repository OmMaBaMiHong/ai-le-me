package org.aileme.shejiao.app.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.ActivityChatGroupService;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.api.service.XiangqinActivityService;
import org.aileme.shejiao.api.service.XiangqinEnrollmentService;
import org.aileme.shejiao.app.dao.ActivityChatGroupDao;
import org.aileme.shejiao.app.dao.ActivityChatMemberDao;
import org.aileme.shejiao.app.dao.ActivityChatMessageDao;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinActivityEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinEnrollmentEntity;
import org.aileme.shejiao.domain.entity.app.ActivityChatGroupEntity;
import org.aileme.shejiao.domain.entity.app.ActivityChatMemberEntity;
import org.aileme.shejiao.domain.entity.app.ActivityChatMessageEntity;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@DS("master")
@Service("activityChatGroupService")
public class ActivityChatGroupServiceImpl extends ServiceImpl<ActivityChatGroupDao, ActivityChatGroupEntity> implements ActivityChatGroupService {

    private static final int ROLE_ORGANIZER = 1;
    private static final int ROLE_MEMBER = 2;
    private static final int STATUS_ACTIVE = 1;

    @Resource
    private ActivityChatGroupDao activityChatGroupDao;

    @Resource
    private ActivityChatMemberDao activityChatMemberDao;

    @Resource
    private ActivityChatMessageDao activityChatMessageDao;

    @Resource
    private XiangqinActivityService xiangqinActivityService;

    @Resource
    private XiangqinEnrollmentService xiangqinEnrollmentService;

    @Resource
    private HongniangService hongniangService;

    @Resource
    private AppUserService appUserService;

    @Override
    public Map<String, Object> getGroupMeta(Integer activityId, AppUserEntity user) {
        if (activityId == null || activityId <= 0) {
            throw new LinfengException("活动不存在");
        }
        XiangqinActivityEntity activity = requireActivity(activityId);
        Integer organizerUid = resolveOrganizerUid(activity);
        boolean isOrganizer = organizerUid != null && user != null && Objects.equals(organizerUid, user.getUid());
        ActivityChatGroupEntity group = getByActivityId(activityId);
        if (group != null) {
            syncApprovedMembers(activityId);
            group = getById(group.getId());
        }
        boolean approvedApplicant = isApprovedApplicant(activityId, user == null ? null : user.getUid());
        boolean joined = false;
        List<ActivityChatMemberEntity> memberList = Collections.emptyList();
        if (group != null) {
            memberList = getActiveMembers(group.getId());
            joined = isOrganizer || memberList.stream().anyMatch(item -> Objects.equals(item.getUserId(), user.getUid()));
        }

        Map<String, Object> result = buildBaseMeta(activity, organizerUid, isOrganizer, approvedApplicant);
        result.put("groupExists", group != null);
        result.put("joined", joined);
        result.put("canCreate", isOrganizer);
        result.put("wechatGroupEnabled", false);
        result.put("wechatEntryMode", "pending");

        if (group == null) {
            result.put("groupId", null);
            result.put("memberCount", 0);
            result.put("previewMembers", Collections.emptyList());
            return result;
        }

        result.put("groupId", group.getId());
        result.put("groupTitle", group.getTitle());
        result.put("lastMessage", group.getLastMessage());
        result.put("lastMessageTime", group.getLastMessageTime());
        result.put("memberCount", memberList.size());
        result.put("previewMembers", buildPreviewMembers(memberList, 8));
        return result;
    }

    @Override
    @DSTransactional
    public Map<String, Object> createOrOpenGroup(Integer activityId, AppUserEntity user) {
        XiangqinActivityEntity activity = requireActivity(activityId);
        Integer organizerUid = resolveOrganizerUid(activity);
        if (organizerUid == null || user == null || !Objects.equals(organizerUid, user.getUid())) {
            throw new LinfengException("仅组局者可发起活动群聊");
        }
        ActivityChatGroupEntity group = getByActivityId(activityId);
        if (group == null) {
            Date now = new Date();
            group = new ActivityChatGroupEntity();
            group.setActivityId(activityId);
            group.setOrganizerUid(organizerUid);
            group.setTitle(buildGroupTitle(activity));
            group.setCoverImg(activity.getCoverImg());
            group.setStatus(STATUS_ACTIVE);
            group.setLastMessage("活动群已创建");
            group.setLastMessageTime(now);
            group.setCreateTime(now);
            group.setUpdateTime(now);
            activityChatGroupDao.insert(group);

            ensureMember(group.getId(), activityId, organizerUid, null, ROLE_ORGANIZER, now);
            saveSystemMessage(group, activity, "活动群已创建，已通过审核的报名用户会自动入群。", now);
        }

        syncApprovedMembers(activityId);
        return getGroupMeta(activityId, user);
    }

    @Override
    public List<Map<String, Object>> getUserSessionList(AppUserEntity user) {
        if (user == null || user.getUid() == null || user.getUid() <= 0) {
            return Collections.emptyList();
        }
        List<ActivityChatMemberEntity> memberList = activityChatMemberDao.selectList(new LambdaQueryWrapper<ActivityChatMemberEntity>()
                .eq(ActivityChatMemberEntity::getUserId, user.getUid())
                .eq(ActivityChatMemberEntity::getStatus, STATUS_ACTIVE)
                .orderByDesc(ActivityChatMemberEntity::getUpdateTime)
                .orderByDesc(ActivityChatMemberEntity::getId));
        if (memberList == null || memberList.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> groupIdSet = new LinkedHashSet<>();
        Set<Integer> activityIdSet = new LinkedHashSet<>();
        for (ActivityChatMemberEntity member : memberList) {
            if (member.getGroupId() != null && member.getGroupId() > 0) {
                groupIdSet.add(member.getGroupId());
            }
            if (member.getActivityId() != null && member.getActivityId() > 0) {
                activityIdSet.add(member.getActivityId());
            }
        }
        if (groupIdSet.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, XiangqinActivityEntity> activityMap = new LinkedHashMap<>();
        for (Integer activityId : activityIdSet) {
            XiangqinActivityEntity activity = xiangqinActivityService.getById(activityId);
            if (activity != null) {
                activityMap.put(activityId, activity);
            }
        }

        List<ActivityChatGroupEntity> groupList = activityChatGroupDao.selectBatchIds(groupIdSet);
        List<Map<String, Object>> result = new ArrayList<>();
        if (groupList == null || groupList.isEmpty()) {
            return result;
        }
        for (ActivityChatGroupEntity group : groupList) {
            if (group == null || !Objects.equals(group.getStatus(), STATUS_ACTIVE)) {
                continue;
            }
            XiangqinActivityEntity activity = activityMap.get(group.getActivityId());
            List<ActivityChatMemberEntity> groupMembers = getActiveMembers(group.getId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "group");
            map.put("sessionId", "group-" + group.getId());
            map.put("groupId", group.getId());
            map.put("activityId", group.getActivityId());
            map.put("name", resolveGroupSessionName(group, activity));
            map.put("activityTitle", activity == null ? "" : activity.getTitle());
            map.put("avatar", resolveGroupSessionAvatar(group, activity));
            map.put("lastMessage", resolveGroupSessionLastMessage(group));
            map.put("unread", 0);
            map.put("updateTime", group.getLastMessageTime() != null ? group.getLastMessageTime() : group.getUpdateTime());
            map.put("memberCount", groupMembers.size());
            map.put("previewMembers", buildPreviewMembers(groupMembers, 4));
            result.add(map);
        }
        result.sort((left, right) -> {
            Date leftTime = left.get("updateTime") instanceof Date ? (Date) left.get("updateTime") : null;
            Date rightTime = right.get("updateTime") instanceof Date ? (Date) right.get("updateTime") : null;
            long leftMillis = leftTime == null ? 0L : leftTime.getTime();
            long rightMillis = rightTime == null ? 0L : rightTime.getTime();
            return Long.compare(rightMillis, leftMillis);
        });
        return result;
    }

    @Override
    public List<Map<String, Object>> getMessageList(Integer groupId, AppUserEntity user, Long lastMessageId, Integer pageSize) {
        ActivityChatGroupEntity group = requireGroup(groupId);
        verifyGroupAccess(group, user);
        int limit = pageSize == null || pageSize <= 0 ? 30 : Math.min(pageSize, 60);
        LambdaQueryWrapper<ActivityChatMessageEntity> wrapper = new LambdaQueryWrapper<ActivityChatMessageEntity>()
                .eq(ActivityChatMessageEntity::getGroupId, groupId);
        if (lastMessageId != null && lastMessageId > 0) {
            wrapper.lt(ActivityChatMessageEntity::getId, lastMessageId);
        }
        wrapper.orderByDesc(ActivityChatMessageEntity::getId).last("limit " + limit);
        List<ActivityChatMessageEntity> list = activityChatMessageDao.selectList(wrapper);
        list.sort(Comparator.comparing(ActivityChatMessageEntity::getId));

        Set<Integer> senderIdSet = new LinkedHashSet<>();
        for (ActivityChatMessageEntity item : list) {
            if (item.getSenderId() != null && item.getSenderId() > 0) {
                senderIdSet.add(item.getSenderId());
            }
        }
        Map<Integer, AppUserEntity> userMap = new LinkedHashMap<>();
        for (Integer uid : senderIdSet) {
            AppUserEntity sender = appUserService.getById(uid);
            if (sender != null) {
                userMap.put(uid, sender);
            }
        }

        List<ActivityChatMemberEntity> memberList = getActiveMembers(groupId);
        Set<Integer> organizerUidSet = new LinkedHashSet<>();
        for (ActivityChatMemberEntity item : memberList) {
            if (Objects.equals(item.getRole(), ROLE_ORGANIZER)) {
                organizerUidSet.add(item.getUserId());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ActivityChatMessageEntity item : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("groupId", item.getGroupId());
            map.put("activityId", item.getActivityId());
            map.put("senderId", item.getSenderId());
            map.put("messageType", item.getMessageType());
            map.put("content", item.getContent());
            map.put("createTime", item.getCreateTime());
            AppUserEntity sender = item.getSenderId() == null ? null : userMap.get(item.getSenderId());
            map.put("senderName", sender == null ? "系统通知" : resolveUsername(sender));
            map.put("senderAvatar", sender == null ? "" : sender.getAvatar());
            map.put("isOrganizer", sender != null && organizerUidSet.contains(sender.getUid()));
            result.add(map);
        }
        return result;
    }

    @Override
    @DSTransactional
    public Map<String, Object> sendMessage(Integer groupId, AppUserEntity user, String content, String messageType) {
        ActivityChatGroupEntity group = requireGroup(groupId);
        verifyGroupAccess(group, user);
        String safeContent = content == null ? "" : content.trim();
        if (safeContent.isEmpty()) {
            throw new LinfengException("消息内容不能为空");
        }
        if (safeContent.length() > 500) {
            throw new LinfengException("消息内容过长");
        }
        Date now = new Date();
        ActivityChatMessageEntity message = new ActivityChatMessageEntity();
        message.setGroupId(groupId);
        message.setActivityId(group.getActivityId());
        message.setSenderId(user.getUid());
        message.setMessageType(messageType == null || messageType.trim().isEmpty() ? "text" : messageType.trim());
        message.setContent(safeContent);
        message.setCreateTime(now);
        message.setUpdateTime(now);
        activityChatMessageDao.insert(message);

        group.setLastMessage(resolveLastMessage(safeContent, message.getMessageType()));
        group.setLastMessageTime(now);
        group.setUpdateTime(now);
        activityChatGroupDao.updateById(group);

        AppUserEntity sender = appUserService.getById(user.getUid());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", message.getId());
        result.put("groupId", message.getGroupId());
        result.put("activityId", message.getActivityId());
        result.put("senderId", message.getSenderId());
        result.put("messageType", message.getMessageType());
        result.put("content", message.getContent());
        result.put("createTime", message.getCreateTime());
        result.put("senderName", resolveUsername(sender));
        result.put("senderAvatar", sender == null ? "" : sender.getAvatar());
        result.put("isOrganizer", Objects.equals(group.getOrganizerUid(), user.getUid()));
        return result;
    }

    @Override
    @DSTransactional
    public void syncApprovedMembers(Integer activityId) {
        ActivityChatGroupEntity group = getByActivityId(activityId);
        if (group == null) {
            return;
        }
        XiangqinActivityEntity activity = requireActivity(activityId);
        Integer organizerUid = resolveOrganizerUid(activity);
        Date now = new Date();

        if (organizerUid != null && organizerUid > 0) {
            ensureMember(group.getId(), activityId, organizerUid, null, ROLE_ORGANIZER, now);
        }

        List<ActivityChatMemberEntity> existingMembers = getActiveMembers(group.getId());
        Set<Integer> existingUserIds = new LinkedHashSet<>();
        for (ActivityChatMemberEntity member : existingMembers) {
            existingUserIds.add(member.getUserId());
        }

        List<XiangqinEnrollmentEntity> enrollmentList = xiangqinEnrollmentService.getByActivityId(activityId);
        List<String> joinedUserNames = new ArrayList<>();
        for (XiangqinEnrollmentEntity enrollment : enrollmentList) {
            Integer userId = enrollment.getUserId();
            if (userId == null || userId <= 0) {
                continue;
            }
            if (!Objects.equals(enrollment.getStatus(), 1)) {
                continue;
            }
            if (existingUserIds.contains(userId)) {
                continue;
            }
            ensureMember(group.getId(), activityId, userId, enrollment.getId(), ROLE_MEMBER, now);
            existingUserIds.add(userId);
            AppUserEntity sender = appUserService.getById(userId);
            joinedUserNames.add(resolveUsername(sender));
        }

        if (!joinedUserNames.isEmpty()) {
            String joinedText = String.join("、", joinedUserNames);
            saveSystemMessage(group, activity, joinedText + " 已加入活动群。", now);
        }
    }

    private XiangqinActivityEntity requireActivity(Integer activityId) {
        XiangqinActivityEntity activity = xiangqinActivityService.getById(activityId);
        if (activity == null) {
            throw new LinfengException("活动不存在");
        }
        return activity;
    }

    private ActivityChatGroupEntity requireGroup(Integer groupId) {
        ActivityChatGroupEntity group = getById(groupId);
        if (group == null || !Objects.equals(group.getStatus(), STATUS_ACTIVE)) {
            throw new LinfengException("活动群不存在");
        }
        return group;
    }

    private ActivityChatGroupEntity getByActivityId(Integer activityId) {
        return this.lambdaQuery()
                .eq(ActivityChatGroupEntity::getActivityId, activityId)
                .eq(ActivityChatGroupEntity::getStatus, STATUS_ACTIVE)
                .last("limit 1")
                .one();
    }

    private Integer resolveOrganizerUid(XiangqinActivityEntity activity) {
        if (activity == null || activity.getHongniangId() == null || activity.getHongniangId() <= 0) {
            return null;
        }
        HongniangInfoEntity hongniang = hongniangService.getById(activity.getHongniangId());
        return hongniang == null ? null : hongniang.getUserId();
    }

    private boolean isApprovedApplicant(Integer activityId, Integer userId) {
        if (activityId == null || userId == null || userId <= 0) {
            return false;
        }
        List<XiangqinEnrollmentEntity> list = xiangqinEnrollmentService.getByActivityId(activityId);
        return list.stream().anyMatch(item ->
                Objects.equals(item.getUserId(), userId) && Objects.equals(item.getStatus(), 1));
    }

    private Map<String, Object> buildBaseMeta(XiangqinActivityEntity activity, Integer organizerUid, boolean isOrganizer, boolean approvedApplicant) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activityId", activity.getId());
        result.put("activityTitle", activity.getTitle());
        result.put("activityCover", activity.getCoverImg());
        result.put("isOrganizer", isOrganizer);
        result.put("approvedApplicant", approvedApplicant);
        result.put("organizerUid", organizerUid);
        AppUserEntity organizer = organizerUid == null ? null : appUserService.getById(organizerUid);
        result.put("organizerName", resolveUsername(organizer));
        result.put("organizerAvatar", organizer == null ? "" : organizer.getAvatar());
        return result;
    }

    private List<ActivityChatMemberEntity> getActiveMembers(Integer groupId) {
        return activityChatMemberDao.selectList(new LambdaQueryWrapper<ActivityChatMemberEntity>()
                .eq(ActivityChatMemberEntity::getGroupId, groupId)
                .eq(ActivityChatMemberEntity::getStatus, STATUS_ACTIVE)
                .orderByAsc(ActivityChatMemberEntity::getRole)
                .orderByAsc(ActivityChatMemberEntity::getJoinTime)
                .orderByAsc(ActivityChatMemberEntity::getId));
    }

    private List<Map<String, Object>> buildPreviewMembers(List<ActivityChatMemberEntity> memberList, int limit) {
        List<Map<String, Object>> previewList = new ArrayList<>();
        List<ActivityChatMemberEntity> safeList = memberList == null ? Collections.emptyList() : memberList;
        for (int i = 0; i < safeList.size() && i < limit; i++) {
            ActivityChatMemberEntity member = safeList.get(i);
            AppUserEntity profile = appUserService.getById(member.getUserId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("userId", member.getUserId());
            map.put("name", resolveUsername(profile));
            map.put("avatar", profile == null ? "" : profile.getAvatar());
            map.put("role", member.getRole());
            previewList.add(map);
        }
        return previewList;
    }

    private void verifyGroupAccess(ActivityChatGroupEntity group, AppUserEntity user) {
        if (group == null || user == null || user.getUid() == null || user.getUid() <= 0) {
            throw new LinfengException("请先登录");
        }
        if (Objects.equals(group.getOrganizerUid(), user.getUid())) {
            return;
        }
        long count = activityChatMemberDao.selectCount(new LambdaQueryWrapper<ActivityChatMemberEntity>()
                .eq(ActivityChatMemberEntity::getGroupId, group.getId())
                .eq(ActivityChatMemberEntity::getUserId, user.getUid())
                .eq(ActivityChatMemberEntity::getStatus, STATUS_ACTIVE));
        if (count <= 0) {
            throw new LinfengException("你还不在活动群中");
        }
    }

    private void ensureMember(Integer groupId, Integer activityId, Integer userId, Integer enrollmentId, Integer role, Date now) {
        ActivityChatMemberEntity existing = activityChatMemberDao.selectOne(new LambdaQueryWrapper<ActivityChatMemberEntity>()
                .eq(ActivityChatMemberEntity::getGroupId, groupId)
                .eq(ActivityChatMemberEntity::getUserId, userId)
                .last("limit 1"));
        if (existing != null) {
            boolean changed = false;
            if (!Objects.equals(existing.getStatus(), STATUS_ACTIVE)) {
                existing.setStatus(STATUS_ACTIVE);
                changed = true;
            }
            if (existing.getRole() == null || existing.getRole() > role) {
                existing.setRole(role);
                changed = true;
            }
            if (enrollmentId != null && !Objects.equals(existing.getEnrollmentId(), enrollmentId)) {
                existing.setEnrollmentId(enrollmentId);
                changed = true;
            }
            if (existing.getJoinTime() == null) {
                existing.setJoinTime(now);
                changed = true;
            }
            if (changed) {
                existing.setUpdateTime(now);
                activityChatMemberDao.updateById(existing);
            }
            return;
        }
        ActivityChatMemberEntity member = new ActivityChatMemberEntity();
        member.setGroupId(groupId);
        member.setActivityId(activityId);
        member.setUserId(userId);
        member.setEnrollmentId(enrollmentId);
        member.setRole(role);
        member.setStatus(STATUS_ACTIVE);
        member.setJoinTime(now);
        member.setCreateTime(now);
        member.setUpdateTime(now);
        activityChatMemberDao.insert(member);
    }

    private void saveSystemMessage(ActivityChatGroupEntity group, XiangqinActivityEntity activity, String content, Date now) {
        ActivityChatMessageEntity message = new ActivityChatMessageEntity();
        message.setGroupId(group.getId());
        message.setActivityId(activity.getId());
        message.setSenderId(0);
        message.setMessageType("system");
        message.setContent(content);
        message.setCreateTime(now);
        message.setUpdateTime(now);
        activityChatMessageDao.insert(message);

        group.setLastMessage(content);
        group.setLastMessageTime(now);
        group.setUpdateTime(now);
        activityChatGroupDao.updateById(group);
    }

    private String buildGroupTitle(XiangqinActivityEntity activity) {
        String title = activity == null ? "" : String.valueOf(activity.getTitle() == null ? "" : activity.getTitle()).trim();
        if (title.isEmpty()) {
            return "活动群聊";
        }
        return "《" + title + "》活动群";
    }

    private String resolveGroupSessionName(ActivityChatGroupEntity group, XiangqinActivityEntity activity) {
        if (group != null && group.getTitle() != null && !group.getTitle().trim().isEmpty()) {
            return group.getTitle().trim();
        }
        if (activity != null && activity.getTitle() != null && !activity.getTitle().trim().isEmpty()) {
            return activity.getTitle().trim() + " · 活动群";
        }
        return "活动群聊";
    }

    private String resolveGroupSessionAvatar(ActivityChatGroupEntity group, XiangqinActivityEntity activity) {
        if (group != null && group.getCoverImg() != null && !group.getCoverImg().trim().isEmpty()) {
            return group.getCoverImg().trim();
        }
        if (activity != null && activity.getCoverImg() != null && !activity.getCoverImg().trim().isEmpty()) {
            return activity.getCoverImg().trim();
        }
        return "";
    }

    private String resolveGroupSessionLastMessage(ActivityChatGroupEntity group) {
        if (group != null && group.getLastMessage() != null && !group.getLastMessage().trim().isEmpty()) {
            return group.getLastMessage().trim();
        }
        return "点击进入活动群聊";
    }

    private String resolveUsername(AppUserEntity user) {
        if (user == null) {
            return "活动成员";
        }
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            return user.getUsername().trim();
        }
        if (user.getMobile() != null && !user.getMobile().trim().isEmpty()) {
            return user.getMobile().trim();
        }
        return "活动成员";
    }

    private String resolveLastMessage(String content, String messageType) {
        if ("image".equalsIgnoreCase(messageType)) {
            return "【图片】";
        }
        if ("system".equalsIgnoreCase(messageType)) {
            return content;
        }
        return content;
    }
}
