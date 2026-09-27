package org.aileme.shejiao.admin.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.admin.dao.HongniangGroupTaskExecutionDao;
import org.aileme.shejiao.admin.dao.HongniangGroupTouchTaskDao;
import org.aileme.shejiao.admin.dao.HongniangMatchCaseDao;
import org.aileme.shejiao.admin.dao.HongniangMatchRequestDao;
import org.aileme.shejiao.admin.dao.HongniangWechatGroupDao;
import org.aileme.shejiao.admin.dao.HongniangWechatGroupUserDao;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.HongniangMatchCaseService;
import org.aileme.shejiao.api.service.HongniangPrivateDomainService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.api.service.HongniangUserRelationService;
import org.aileme.shejiao.api.service.HongniangWechatGroupService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangGroupTaskExecutionEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangGroupTouchTaskEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchCaseEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchRequestEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangWechatGroupEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@DS("master")
@Service("hongniangPrivateDomainService")
public class HongniangPrivateDomainServiceImpl implements HongniangPrivateDomainService {

    private final HongniangUserRelationService relationService;
    private final HongniangMatchCaseService matchCaseService;
    private final HongniangWechatGroupService wechatGroupService;
    private final HongniangService hongniangService;
    private final AppUserService appUserService;
    private final HongniangMatchCaseDao matchCaseDao;
    private final HongniangMatchRequestDao matchRequestDao;
    private final HongniangWechatGroupDao wechatGroupDao;
    private final HongniangWechatGroupUserDao groupUserDao;
    private final HongniangGroupTouchTaskDao touchTaskDao;
    private final HongniangGroupTaskExecutionDao taskExecutionDao;

    public HongniangPrivateDomainServiceImpl(HongniangUserRelationService relationService,
                                             HongniangMatchCaseService matchCaseService,
                                             HongniangWechatGroupService wechatGroupService,
                                             HongniangService hongniangService,
                                             AppUserService appUserService,
                                             HongniangMatchCaseDao matchCaseDao,
                                             HongniangMatchRequestDao matchRequestDao,
                                             HongniangWechatGroupDao wechatGroupDao,
                                             HongniangWechatGroupUserDao groupUserDao,
                                             HongniangGroupTouchTaskDao touchTaskDao,
                                             HongniangGroupTaskExecutionDao taskExecutionDao) {
        this.relationService = relationService;
        this.matchCaseService = matchCaseService;
        this.wechatGroupService = wechatGroupService;
        this.hongniangService = hongniangService;
        this.appUserService = appUserService;
        this.matchCaseDao = matchCaseDao;
        this.matchRequestDao = matchRequestDao;
        this.wechatGroupDao = wechatGroupDao;
        this.groupUserDao = groupUserDao;
        this.touchTaskDao = touchTaskDao;
        this.taskExecutionDao = taskExecutionDao;
    }

    @Override
    public Map<String, Object> overview(Integer hongniangId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<Integer> hongniangIds = resolveHongniangScope(hongniangId);

        int poolUserCount = Math.toIntExact(relationService.lambdaQuery()
            .in(HongniangUserRelationEntity::getHongniangId, hongniangIds)
            .count());
        int caseCount = Math.toIntExact(matchCaseDao.selectCount(new LambdaQueryWrapper<HongniangMatchCaseEntity>()
            .in(HongniangMatchCaseEntity::getHongniangId, hongniangIds)));
        int openCaseCount = Math.toIntExact(matchCaseDao.selectCount(new LambdaQueryWrapper<HongniangMatchCaseEntity>()
            .in(HongniangMatchCaseEntity::getHongniangId, hongniangIds)
            .ne(HongniangMatchCaseEntity::getCurrentStage, HongniangMatchCaseServiceImpl.STAGE_CLOSED)));
        int successCaseCount = hongniangIds.stream().mapToInt(matchCaseService::countSuccessCases).sum();
        int wechatGroupCount = Math.toIntExact(wechatGroupDao.selectCount(new LambdaQueryWrapper<HongniangWechatGroupEntity>()
            .in(HongniangWechatGroupEntity::getHongniangId, hongniangIds)));
        int activeTouchTaskCount = Math.toIntExact(touchTaskDao.selectCount(new LambdaQueryWrapper<HongniangGroupTouchTaskEntity>()
            .in(HongniangGroupTouchTaskEntity::getHongniangId, hongniangIds)
            .in(HongniangGroupTouchTaskEntity::getTaskStatus, List.of(0, 1))));
        int pendingMatchRequestCount = Math.toIntExact(matchRequestDao.selectCount(new LambdaQueryWrapper<HongniangMatchRequestEntity>()
            .in(HongniangMatchRequestEntity::getHongniangId, hongniangIds)
            .in(HongniangMatchRequestEntity::getRequestStatus, List.of(0, 1))));

        result.put("hongniangId", hongniangId);
        result.put("poolUserCount", poolUserCount);
        result.put("caseCount", caseCount);
        result.put("openCaseCount", openCaseCount);
        result.put("successCaseCount", successCaseCount);
        result.put("wechatGroupCount", wechatGroupCount);
        result.put("activeTouchTaskCount", activeTouchTaskCount);
        result.put("pendingMatchRequestCount", pendingMatchRequestCount);
        result.put("stageDistribution", buildStageDistribution(hongniangIds));
        result.put("requestStatusDistribution", buildRequestDistribution(hongniangIds));
        return result;
    }

    @Override
    public Map<String, Object> kanban(Map<String, Object> params) {
        Integer hongniangId = parseInteger(params.get("hongniangId"));
        Set<Integer> hongniangIds = resolveHongniangScope(hongniangId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overview", overview(hongniangId));
        result.put("urgentCases", buildUrgentCases(hongniangIds));
        result.put("pendingRequests", buildPendingRequests(hongniangIds));
        result.put("recentGroups", buildRecentGroups(hongniangIds));
        result.put("recentExecutions", buildRecentExecutions(hongniangIds));
        return result;
    }

    @Override
    public Map<String, Object> workbenchOverview(Integer hongniangId) {
        requireHongniang(hongniangId);
        Map<String, Object> result = new LinkedHashMap<>(overview(hongniangId));
        result.put("todayFollowList", buildUrgentCases(Set.of(hongniangId)));
        result.put("pendingRequests", buildPendingRequests(Set.of(hongniangId)));
        result.put("recentGroups", buildRecentGroups(Set.of(hongniangId)));
        return result;
    }

    @Override
    public PageUtils workbenchUsers(Integer hongniangId, Map<String, Object> params) {
        requireHongniang(hongniangId);
        String keyword = trim((String) params.get("keyword"));
        LambdaQueryWrapper<HongniangUserRelationEntity> wrapper = new LambdaQueryWrapper<HongniangUserRelationEntity>()
            .eq(HongniangUserRelationEntity::getHongniangId, hongniangId)
            .orderByDesc(HongniangUserRelationEntity::getCreateTime)
            .orderByDesc(HongniangUserRelationEntity::getId);
        IPage<HongniangUserRelationEntity> page = relationService.page(new Query<HongniangUserRelationEntity>().getPage(params), wrapper);
        List<HongniangUserRelationEntity> filteredRelations = page.getRecords();
        if (StringUtils.isNotBlank(keyword)) {
            Map<Integer, AppUserEntity> userMap = loadUsers(filteredRelations.stream()
                .map(HongniangUserRelationEntity::getUserId)
                .collect(Collectors.toList()));
            filteredRelations = filteredRelations.stream()
                .filter(item -> matchesUserKeyword(userMap.get(item.getUserId()), keyword, item.getHongniangUserNo()))
                .collect(Collectors.toList());
        }
        List<Map<String, Object>> rows = buildWorkbenchUsers(filteredRelations);
        return new PageUtils(rows, (int) page.getTotal(), (int) page.getSize(), (int) page.getCurrent());
    }

    @Override
    public PageUtils workbenchCases(Integer hongniangId, Map<String, Object> params) {
        requireHongniang(hongniangId);
        String keyword = trim((String) params.get("keyword"));
        Integer currentStage = parseInteger(params.get("currentStage"));
        Integer requestStatus = parseInteger(params.get("requestStatus"));
        Set<Integer> matchedUserIds = resolveUserIdsByKeyword(keyword);
        LambdaQueryWrapper<HongniangMatchCaseEntity> wrapper = new LambdaQueryWrapper<HongniangMatchCaseEntity>()
            .eq(HongniangMatchCaseEntity::getHongniangId, hongniangId)
            .orderByAsc(HongniangMatchCaseEntity::getNextFollowTime)
            .orderByDesc(HongniangMatchCaseEntity::getUpdateTime)
            .orderByDesc(HongniangMatchCaseEntity::getId);
        if (currentStage != null) {
            wrapper.eq(HongniangMatchCaseEntity::getCurrentStage, currentStage);
        }
        if (requestStatus != null) {
            wrapper.eq(HongniangMatchCaseEntity::getLatestRequestStatus, requestStatus);
        }
        if (matchedUserIds != null) {
            if (matchedUserIds.isEmpty()) {
                return emptyPage(params);
            }
            wrapper.and(query -> query.in(HongniangMatchCaseEntity::getMaleUserId, matchedUserIds)
                .or().in(HongniangMatchCaseEntity::getFemaleUserId, matchedUserIds));
        }
        IPage<HongniangMatchCaseEntity> page = matchCaseDao.selectPage(new Query<HongniangMatchCaseEntity>().getPage(params), wrapper);
        List<Map<String, Object>> rows = page.getRecords().stream()
            .map(this::buildWorkbenchCaseCard)
            .collect(Collectors.toList());
        return new PageUtils(rows, (int) page.getTotal(), (int) page.getSize(), (int) page.getCurrent());
    }

    @Override
    public Map<String, Object> workbenchCaseDetail(Integer hongniangId, Integer caseId) {
        requireHongniang(hongniangId);
        HongniangMatchCaseEntity matchCase = matchCaseDao.selectById(caseId);
        if (matchCase == null || !Objects.equals(matchCase.getHongniangId(), hongniangId)) {
            throw new LinfengException("案件不存在或无权查看");
        }
        Map<String, Object> detail = matchCaseService.getDetail(caseId);
        detail.put("canOperate", true);
        return detail;
    }

    @Override
    public PageUtils workbenchGroups(Integer hongniangId, Map<String, Object> params) {
        requireHongniang(hongniangId);
        Map<String, Object> query = new LinkedHashMap<>(params);
        query.put("hongniangId", hongniangId);
        return wechatGroupService.queryPage(query);
    }

    @Override
    public Map<String, Object> workbenchGroupDetail(Integer hongniangId, Integer groupId) {
        requireHongniang(hongniangId);
        HongniangWechatGroupEntity group = wechatGroupDao.selectById(groupId);
        if (group == null || !Objects.equals(group.getHongniangId(), hongniangId)) {
            throw new LinfengException("微信群不存在或无权查看");
        }
        Map<String, Object> detail = wechatGroupService.getDetail(groupId);
        detail.put("executions", wechatGroupService.listTaskExecutions(null, groupId, 20));
        return detail;
    }

    private Set<Integer> resolveHongniangScope(Integer hongniangId) {
        if (hongniangId != null && hongniangId > 0) {
            return Set.of(hongniangId);
        }
        List<Integer> ids = hongniangService.list().stream()
            .map(HongniangInfoEntity::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Set.of(-1);
        }
        return new LinkedHashSet<>(ids);
    }

    private void requireHongniang(Integer hongniangId) {
        if (hongniangId == null || hongniangId <= 0 || hongniangService.getById(hongniangId) == null) {
            throw new LinfengException("红娘不存在");
        }
    }

    private List<Map<String, Object>> buildStageDistribution(Set<Integer> hongniangIds) {
        List<HongniangMatchCaseEntity> cases = matchCaseDao.selectList(new LambdaQueryWrapper<HongniangMatchCaseEntity>()
            .in(HongniangMatchCaseEntity::getHongniangId, hongniangIds));
        Map<Integer, Long> counts = cases.stream()
            .collect(Collectors.groupingBy(item -> item.getCurrentStage() == null ? -1 : item.getCurrentStage(), LinkedHashMap::new, Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int stage = 0; stage <= HongniangMatchCaseServiceImpl.STAGE_CLOSED; stage++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stage", stage);
            item.put("label", HongniangMatchCaseServiceImpl.stageLabel(stage));
            item.put("count", counts.getOrDefault(stage, 0L));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildRequestDistribution(Set<Integer> hongniangIds) {
        List<HongniangMatchRequestEntity> requests = matchRequestDao.selectList(new LambdaQueryWrapper<HongniangMatchRequestEntity>()
            .in(HongniangMatchRequestEntity::getHongniangId, hongniangIds));
        Map<Integer, Long> counts = requests.stream()
            .collect(Collectors.groupingBy(item -> item.getRequestStatus() == null ? -1 : item.getRequestStatus(), LinkedHashMap::new, Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int status = 0; status <= 4; status++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", status);
            item.put("label", requestStatusLabel(status));
            item.put("count", counts.getOrDefault(status, 0L));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildUrgentCases(Set<Integer> hongniangIds) {
        List<HongniangMatchCaseEntity> cases = matchCaseDao.selectList(new LambdaQueryWrapper<HongniangMatchCaseEntity>()
            .in(HongniangMatchCaseEntity::getHongniangId, hongniangIds)
            .ne(HongniangMatchCaseEntity::getCurrentStage, HongniangMatchCaseServiceImpl.STAGE_CLOSED)
            .orderByAsc(HongniangMatchCaseEntity::getNextFollowTime)
            .orderByDesc(HongniangMatchCaseEntity::getUpdateTime)
            .last("limit 8"));
        return cases.stream().map(this::buildWorkbenchCaseCard).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildPendingRequests(Set<Integer> hongniangIds) {
        List<HongniangMatchRequestEntity> requests = matchRequestDao.selectList(new LambdaQueryWrapper<HongniangMatchRequestEntity>()
            .in(HongniangMatchRequestEntity::getHongniangId, hongniangIds)
            .in(HongniangMatchRequestEntity::getRequestStatus, List.of(0, 1))
            .orderByDesc(HongniangMatchRequestEntity::getCreateTime)
            .last("limit 8"));
        Map<Integer, AppUserEntity> userMap = loadUsers(requests.stream()
            .flatMap(item -> java.util.stream.Stream.of(item.getFromUserId(), item.getToUserId()))
            .collect(Collectors.toList()));
        return requests.stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("caseId", item.getCaseId());
            map.put("requestChannel", item.getRequestChannel());
            map.put("requestChannelLabel", item.getRequestChannel() != null && item.getRequestChannel() == 2 ? "红娘代分享微信" : "App 私信申请");
            map.put("requestStatus", item.getRequestStatus());
            map.put("requestStatusLabel", requestStatusLabel(item.getRequestStatus()));
            map.put("fromUserName", resolveUserName(userMap.get(item.getFromUserId()), item.getFromUserId()));
            map.put("toUserName", resolveUserName(userMap.get(item.getToUserId()), item.getToUserId()));
            map.put("requestMessage", item.getRequestMessage());
            map.put("expireTime", item.getExpireTime());
            map.put("createTime", item.getCreateTime());
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildRecentGroups(Set<Integer> hongniangIds) {
        List<HongniangWechatGroupEntity> groups = wechatGroupDao.selectList(new LambdaQueryWrapper<HongniangWechatGroupEntity>()
            .in(HongniangWechatGroupEntity::getHongniangId, hongniangIds)
            .orderByDesc(HongniangWechatGroupEntity::getUpdateTime)
            .last("limit 8"));
        return groups.stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("groupName", item.getGroupName());
            map.put("groupType", item.getGroupType());
            map.put("providerType", item.getProviderType());
            map.put("syncStatus", item.getSyncStatus());
            map.put("syncStatusLabel", syncStatusLabel(item.getSyncStatus()));
            map.put("boundUserCount", groupUserDao.selectCount(new LambdaQueryWrapper<org.aileme.shejiao.domain.entity.admin.HongniangWechatGroupUserEntity>()
                .eq(org.aileme.shejiao.domain.entity.admin.HongniangWechatGroupUserEntity::getGroupId, item.getId())));
            map.put("lastSyncTime", item.getLastSyncTime());
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildRecentExecutions(Set<Integer> hongniangIds) {
        List<Integer> groupIds = wechatGroupDao.selectList(new LambdaQueryWrapper<HongniangWechatGroupEntity>()
                .in(HongniangWechatGroupEntity::getHongniangId, hongniangIds)
                .select(HongniangWechatGroupEntity::getId))
            .stream()
            .map(HongniangWechatGroupEntity::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (groupIds.isEmpty()) {
            return List.of();
        }
        List<HongniangGroupTaskExecutionEntity> executions = taskExecutionDao.selectList(new LambdaQueryWrapper<HongniangGroupTaskExecutionEntity>()
            .in(HongniangGroupTaskExecutionEntity::getGroupId, groupIds)
            .orderByDesc(HongniangGroupTaskExecutionEntity::getCreateTime)
            .last("limit 8"));
        return executions.stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("taskId", item.getTaskId());
            map.put("groupId", item.getGroupId());
            map.put("executionStatus", item.getExecutionStatus());
            map.put("executionStatusLabel", executionStatusLabel(item.getExecutionStatus()));
            map.put("resultSummary", item.getResultSummary());
            map.put("executeTime", item.getExecuteTime());
            map.put("finishTime", item.getFinishTime());
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildWorkbenchUsers(List<HongniangUserRelationEntity> relations) {
        Map<Integer, AppUserEntity> userMap = loadUsers(relations.stream()
            .map(HongniangUserRelationEntity::getUserId)
            .collect(Collectors.toList()));
        Set<Integer> userIds = relations.stream()
            .map(HongniangUserRelationEntity::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<HongniangMatchCaseEntity> cases = userIds.isEmpty() ? List.of() : matchCaseDao.selectList(new LambdaQueryWrapper<HongniangMatchCaseEntity>()
            .and(wrapper -> wrapper.in(HongniangMatchCaseEntity::getMaleUserId, userIds)
                .or().in(HongniangMatchCaseEntity::getFemaleUserId, userIds)));
        Map<Integer, Long> caseCountMap = new LinkedHashMap<>();
        for (HongniangMatchCaseEntity item : cases) {
            if (item.getMaleUserId() != null) {
                caseCountMap.merge(item.getMaleUserId(), 1L, Long::sum);
            }
            if (item.getFemaleUserId() != null) {
                caseCountMap.merge(item.getFemaleUserId(), 1L, Long::sum);
            }
        }
        return relations.stream().map(item -> {
            AppUserEntity user = userMap.get(item.getUserId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("userId", item.getUserId());
            map.put("hongniangUserNo", item.getHongniangUserNo());
            map.put("username", user == null ? null : user.getUsername());
            map.put("mobile", user == null ? null : user.getMobile());
            map.put("avatar", user == null ? null : user.getAvatar());
            map.put("gender", user == null ? null : user.getGender());
            map.put("age", user == null ? null : user.getAge());
            map.put("relationTime", item.getCreateTime());
            map.put("caseCount", caseCountMap.getOrDefault(item.getUserId(), 0L));
            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> buildWorkbenchCaseCard(HongniangMatchCaseEntity item) {
        Map<Integer, AppUserEntity> userMap = loadUsers(List.of(item.getMaleUserId(), item.getFemaleUserId()));
        AppUserEntity male = userMap.get(item.getMaleUserId());
        AppUserEntity female = userMap.get(item.getFemaleUserId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("maleUserId", item.getMaleUserId());
        map.put("femaleUserId", item.getFemaleUserId());
        map.put("maleUsername", resolveUserName(male, item.getMaleUserId()));
        map.put("femaleUsername", resolveUserName(female, item.getFemaleUserId()));
        map.put("maleAvatar", male == null ? null : male.getAvatar());
        map.put("femaleAvatar", female == null ? null : female.getAvatar());
        map.put("maleMobile", male == null ? null : male.getMobile());
        map.put("femaleMobile", female == null ? null : female.getMobile());
        map.put("currentStage", item.getCurrentStage());
        map.put("currentStageLabel", HongniangMatchCaseServiceImpl.stageLabel(item.getCurrentStage()));
        map.put("sourceType", item.getSourceType());
        map.put("sourceLabel", HongniangMatchCaseServiceImpl.sourceLabel(item.getSourceType()));
        map.put("nextFollowTime", item.getNextFollowTime());
        map.put("lastFollowTime", item.getLastFollowTime());
        map.put("latestRequestStatus", item.getLatestRequestStatus());
        map.put("latestRequestStatusLabel", requestStatusLabel(item.getLatestRequestStatus()));
        map.put("remark", item.getRemark());
        map.put("updateTime", item.getUpdateTime());
        return map;
    }

    private Map<Integer, AppUserEntity> loadUsers(Collection<Integer> ids) {
        LinkedHashSet<Integer> uniqueIds = ids == null ? new LinkedHashSet<>() : ids.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }
        return appUserService.listByIds(uniqueIds).stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(AppUserEntity::getUid, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private boolean matchesUserKeyword(AppUserEntity user, String keyword, Integer hongniangUserNo) {
        if (StringUtils.isBlank(keyword)) {
            return true;
        }
        if (user == null) {
            return hongniangUserNo != null && StringUtils.equals(String.valueOf(hongniangUserNo), keyword.trim());
        }
        String normalized = keyword.trim();
        return StringUtils.containsIgnoreCase(StringUtils.defaultString(user.getUsername()), normalized)
            || StringUtils.containsIgnoreCase(StringUtils.defaultString(user.getMobile()), normalized)
            || StringUtils.equals(String.valueOf(user.getUid()), normalized)
            || (hongniangUserNo != null && StringUtils.equals(String.valueOf(hongniangUserNo), normalized));
    }

    private Set<Integer> resolveUserIdsByKeyword(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        return appUserService.lambdaQuery()
            .and(wrapper -> {
                if (StringUtils.isNumeric(keyword)) {
                    Integer value = Integer.parseInt(keyword);
                    wrapper.eq(AppUserEntity::getUid, value)
                        .or().like(AppUserEntity::getMobile, keyword)
                        .or().like(AppUserEntity::getUsername, keyword);
                } else {
                    wrapper.like(AppUserEntity::getUsername, keyword)
                        .or().like(AppUserEntity::getMobile, keyword);
                }
            })
            .list()
            .stream()
            .map(AppUserEntity::getUid)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String requestStatusLabel(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 0 -> "待发送";
            case 1 -> "已发送";
            case 2 -> "已接受";
            case 3 -> "已拒绝";
            case 4 -> "已过期";
            default -> String.valueOf(status);
        };
    }

    private String executionStatusLabel(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 0 -> "待执行";
            case 1 -> "执行中";
            case 2 -> "成功";
            case 3 -> "失败";
            case 4 -> "仅登记";
            default -> String.valueOf(status);
        };
    }

    private String syncStatusLabel(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 0 -> "未同步";
            case 1 -> "已同步";
            case 2 -> "同步失败";
            default -> String.valueOf(status);
        };
    }

    private String resolveUserName(AppUserEntity user, Integer userId) {
        return user == null ? String.valueOf(userId) : user.getUsername();
    }

    private Integer parseInteger(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String trim(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private PageUtils emptyPage(Map<String, Object> params) {
        int pageSize = parseInteger(params.get("pageSize")) == null ? 10 : parseInteger(params.get("pageSize"));
        int pageNum = parseInteger(params.get("pageNum")) == null ? 1 : parseInteger(params.get("pageNum"));
        return new PageUtils(List.of(), 0, pageSize, pageNum);
    }
}
