package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.api.service.HongniangUserRelationService;
import org.aileme.shejiao.api.service.TopicService;
import org.aileme.shejiao.api.service.XiangqinActivityService;
import org.aileme.shejiao.api.service.XiangqinEnrollmentService;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.TopicEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinActivityEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinEnrollmentEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * App端-红娘Controller
 */
@RestController
@RequestMapping("/app/hongniang")
@Tag(name = "App端——红娘专区")
public class AppHongniangController {

    @Autowired
    private HongniangService hongniangService;

    @Autowired
    private HongniangUserRelationService relationService;

    @Autowired
    private XiangqinActivityService xiangqinActivityService;

    @Autowired
    private XiangqinEnrollmentService xiangqinEnrollmentService;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private TopicService topicService;

    @GetMapping("/list")
    @Operation(summary = "红娘列表")
    public R list(@RequestParam Map<String, Object> params) {
        // 只查询启用状态的红娘
        params.put("status", 1);
        
        PageUtils page = hongniangService.queryPage(params);
        
        // 如果有userId，查询关注状态
        if (params.containsKey("userId")) {
            Integer userId = Integer.valueOf(params.get("userId").toString());
            List<HongniangInfoEntity> list = (List<HongniangInfoEntity>) page.getList();
            for (HongniangInfoEntity hongniang : list) {
                boolean isFollowed = relationService.checkFollowed(userId, hongniang.getId());
                hongniang.setIsFollowed(isFollowed);
            }
        }
        
        return R.ok().put("result", page);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "红娘详情")
    public R detail(@PathVariable("id") Integer id, @RequestParam(required = false) Integer userId) {
        HongniangInfoEntity hongniang = hongniangService.getById(id);
        if (hongniang == null) {
            return R.error("红娘不存在");
        }
        
        // 查询关注状态
        if (userId != null) {
            boolean isFollowed = relationService.checkFollowed(userId, id);
            hongniang.setIsFollowed(isFollowed);
        }
        
        // 查询该红娘管理的用户数和组织的活动数
        Map<String, Object> stats = hongniangService.getHongniangStats(id);

        return R.ok().put("hongniang", buildHongniangDetail(hongniang)).put("stats", stats);
    }

    @GetMapping("/statsDetail/{id}")
    @Operation(summary = "红娘统计明细")
    public R statsDetail(@PathVariable("id") Integer id) {
        HongniangInfoEntity hongniang = hongniangService.getById(id);
        if (hongniang == null) {
            return R.error("红娘不存在");
        }

        List<Integer> managedUserIds = relationService.getUserIdsByHongniangId(id);
        List<XiangqinActivityEntity> activities = xiangqinActivityService.getByHongniangId(id);
        if (activities == null) {
            activities = new ArrayList<>();
        }
        activities.sort(Comparator.comparing(
                XiangqinActivityEntity::getStartTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        ).thenComparing(
                XiangqinActivityEntity::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        Set<Integer> linkedUserIdSet = new LinkedHashSet<>();
        Map<Integer, Integer> userActivityMap = new LinkedHashMap<>();
        for (XiangqinActivityEntity activity : activities) {
            if (activity == null || activity.getId() == null) {
                continue;
            }
            List<XiangqinEnrollmentEntity> enrollments = xiangqinEnrollmentService.getByActivityId(activity.getId());
            if (enrollments == null) {
                continue;
            }
            for (XiangqinEnrollmentEntity enrollment : enrollments) {
                if (enrollment == null || enrollment.getUserId() == null || enrollment.getUserId() <= 0) {
                    continue;
                }
                Integer status = enrollment.getStatus();
                if (status != null && (status == 2 || status == 3)) {
                    continue;
                }
                linkedUserIdSet.add(enrollment.getUserId());
                userActivityMap.putIfAbsent(enrollment.getUserId(), activity.getId());
            }
        }

        Map<Integer, XiangqinActivityEntity> activityMap = new LinkedHashMap<>();
        for (XiangqinActivityEntity activity : activities) {
            if (activity != null && activity.getId() != null) {
                activityMap.put(activity.getId(), activity);
            }
        }

        return R.ok()
                .put("stats", hongniangService.getHongniangStats(id))
                .put("activities", buildActivityPreviewList(activities))
                .put("managedUsers", buildUserPreviewList(managedUserIds, null))
                .put("linkedUsers", buildUserPreviewList(new ArrayList<>(linkedUserIdSet), userActivityMap, activityMap));
    }

    @PostMapping("/follow")
    @Operation(summary = "关注红娘")
    public R follow(@RequestBody Map<String, Object> params) {
        Integer userId = Integer.valueOf(params.get("userId").toString());
        Integer hongniangId = Integer.valueOf(params.get("hongniangId").toString());
        
        // 检查是否已关注
        if (relationService.checkFollowed(userId, hongniangId)) {
            return R.error("已经关注过该红娘");
        }
        
        relationService.follow(userId, hongniangId);
        return R.ok().put("msg", "关注成功");
    }

    @PostMapping("/unfollow")
    @Operation(summary = "取消关注")
    public R unfollow(@RequestBody Map<String, Object> params) {
        Integer userId = Integer.valueOf(params.get("userId").toString());
        Integer hongniangId = Integer.valueOf(params.get("hongniangId").toString());
        
        relationService.unfollow(userId, hongniangId);
        return R.ok().put("msg", "已取消关注");
    }

    @GetMapping("/myHongniang")
    @Operation(summary = "我关注的红娘")
    public R myHongniang(@RequestParam Integer userId) {
        List<HongniangInfoEntity> list = relationService.getFollowedHongniangList(userId);
        return R.ok().put("list", list);
    }

    @GetMapping("/checkFollowed")
    @Operation(summary = "检查是否已关注")
    public R checkFollowed(@RequestParam Integer userId, @RequestParam Integer hongniangId) {
        boolean isFollowed = relationService.checkFollowed(userId, hongniangId);
        return R.ok().put("isFollowed", isFollowed);
    }

    private List<Map<String, Object>> buildActivityPreviewList(List<XiangqinActivityEntity> activities) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (XiangqinActivityEntity activity : activities) {
            if (activity == null || activity.getId() == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", activity.getId());
            item.put("title", activity.getTitle());
            item.put("coverImg", activity.getCoverImg());
            item.put("address", activity.getAddress());
            item.put("startTime", activity.getStartTime());
            item.put("status", activity.getStatus());
            int enrollCount = (activity.getMaleCount() == null ? 0 : activity.getMaleCount())
                    + (activity.getFemaleCount() == null ? 0 : activity.getFemaleCount());
            item.put("enrollCount", enrollCount);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildUserPreviewList(List<Integer> userIds, Map<Integer, Integer> userActivityMap) {
        return buildUserPreviewList(userIds, userActivityMap, new LinkedHashMap<>());
    }

    private List<Map<String, Object>> buildUserPreviewList(List<Integer> userIds,
                                                           Map<Integer, Integer> userActivityMap,
                                                           Map<Integer, XiangqinActivityEntity> activityMap) {
        List<Integer> distinctIds = userIds == null
                ? Collections.emptyList()
                : userIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, AppUserEntity> userMap = new LinkedHashMap<>();
        List<AppUserEntity> users = appUserService.getBatchUser(distinctIds);
        if (users != null) {
            for (AppUserEntity user : users) {
                if (user != null && user.getUid() != null) {
                    userMap.put(user.getUid(), user);
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Integer userId : distinctIds) {
            AppUserEntity user = userMap.get(userId);
            if (user == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("uid", user.getUid());
            item.put("avatar", user.getAvatar());
            item.put("username", resolveUserName(user));
            item.put("age", user.getAge());
            item.put("gender", user.getGender());
            item.put("city", resolveUserCity(user));
            item.put("job", user.getJob());
            Integer activityId = userActivityMap == null ? null : userActivityMap.get(userId);
            if (activityId != null && activityMap != null) {
                XiangqinActivityEntity activity = activityMap.get(activityId);
                if (activity != null) {
                    item.put("activityId", activity.getId());
                    item.put("activityTitle", activity.getTitle());
                }
            }
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> buildHongniangDetail(HongniangInfoEntity hongniang) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", hongniang.getId());
        result.put("userId", hongniang.getUserId());
        result.put("hongniangName", hongniang.getHongniangName());
        result.put("avatar", hongniang.getAvatar());
        result.put("phone", hongniang.getPhone());
        result.put("wechat", hongniang.getWechat());
        result.put("companyName", hongniang.getCompanyName());
        result.put("certificationStatus", hongniang.getCertificationStatus());
        result.put("certificationImg", hongniang.getCertificationImg());
        result.put("intro", hongniang.getIntro());
        result.put("serviceArea", hongniang.getServiceArea());
        result.put("level", hongniang.getLevel());
        result.put("totalUsers", hongniang.getTotalUsers());
        result.put("totalActivities", hongniang.getTotalActivities());
        result.put("successCount", hongniang.getSuccessCount());
        result.put("status", hongniang.getStatus());
        result.put("tenantId", hongniang.getTenantId());
        result.put("createTime", hongniang.getCreateTime());
        result.put("updateTime", hongniang.getUpdateTime());
        result.put("isFollowed", hongniang.getIsFollowed());
        result.put("topics", buildHongniangTopicPreviewList(hongniang));

        if (hongniang.getUserId() == null || hongniang.getUserId() <= 0) {
            return result;
        }
        AppUserEntity user = appUserService.getById(hongniang.getUserId());
        if (user == null) {
            return result;
        }
        if (isBlank((String) result.get("avatar")) && !isBlank(user.getAvatar())) {
            result.put("avatar", user.getAvatar());
        }
        if (isBlank((String) result.get("intro")) && !isBlank(user.getSelfIntroduction())) {
            result.put("intro", user.getSelfIntroduction());
        }
        result.put("tagStr", user.getTagStr());
        result.put("interest", user.getInterest());
        result.put("userGender", user.getGender());
        result.put("userAge", user.getAge());
        return result;
    }

    private List<Map<String, Object>> buildHongniangTopicPreviewList(HongniangInfoEntity hongniang) {
        if (hongniang == null) {
            return Collections.emptyList();
        }
        Integer hongniangId = hongniang.getId();
        if (hongniangId == null || hongniangId <= 0) {
            return Collections.emptyList();
        }
        List<TopicEntity> topics = topicService.lambdaQuery()
                .eq(TopicEntity::getStatus, Constant.NORMAL)
                .eq(TopicEntity::getChannelType, 2)
                .eq(TopicEntity::getHongniangId, hongniangId)
                .orderByDesc(TopicEntity::getTopType)
                .orderByDesc(TopicEntity::getUserNum)
                .orderByDesc(TopicEntity::getId)
                .list();
        if (topics == null || topics.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (TopicEntity topic : topics) {
            if (topic == null || topic.getId() == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", topic.getId());
            item.put("topicName", topic.getTopicName());
            item.put("coverImage", topic.getCoverImage());
            item.put("bgImage", topic.getBgImage());
            item.put("userCount", topic.getUserNum() == null ? 0 : topic.getUserNum());
            item.put("postCount", 0);
            item.put("channelType", topic.getChannelType());
            item.put("hongniangId", topic.getHongniangId());
            result.add(item);
        }
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveUserName(AppUserEntity user) {
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        if (user.getMobile() != null && user.getMobile().length() >= 7) {
            return user.getMobile().replaceAll("(\\d{3})\\d{4}(\\d+)", "$1****$2");
        }
        return "平台用户";
    }

    private String resolveUserCity(AppUserEntity user) {
        if (user.getAbodeCity() != null && !user.getAbodeCity().isBlank()) {
            return user.getAbodeCity();
        }
        if (user.getLocationCity() != null && !user.getLocationCity().isBlank()) {
            return user.getLocationCity();
        }
        if (user.getCity() != null && !user.getCity().isBlank()) {
            return user.getCity();
        }
        if (user.getHomeCity() != null && !user.getHomeCity().isBlank()) {
            return user.getHomeCity();
        }
        return "";
    }
}
