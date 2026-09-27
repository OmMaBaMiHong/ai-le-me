package org.aileme.shejiao.admin.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.admin.dao.HongniangGroupTouchLogDao;
import org.aileme.shejiao.admin.dao.HongniangGroupTouchTaskDao;
import org.aileme.shejiao.admin.dao.HongniangGroupTaskExecutionDao;
import org.aileme.shejiao.admin.dao.HongniangMatchCaseDao;
import org.aileme.shejiao.admin.dao.HongniangMatchCaseGroupDao;
import org.aileme.shejiao.admin.dao.HongniangWechatGroupDao;
import org.aileme.shejiao.admin.dao.HongniangWechatGroupUserDao;
import org.aileme.shejiao.admin.service.group.GroupTouchProvider;
import org.aileme.shejiao.admin.service.group.ProviderResult;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.HongniangMatchCaseService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.api.service.HongniangUserRelationService;
import org.aileme.shejiao.api.service.HongniangWechatGroupService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangGroupTouchLogEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangGroupTaskExecutionEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangGroupTouchTaskEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchCaseEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchCaseGroupEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangWechatGroupEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangWechatGroupUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@DS("master")
@Service("hongniangWechatGroupService")
public class HongniangWechatGroupServiceImpl extends ServiceImpl<HongniangWechatGroupDao, HongniangWechatGroupEntity> implements HongniangWechatGroupService {

    private final HongniangWechatGroupUserDao groupUserDao;
    private final HongniangMatchCaseGroupDao caseGroupDao;
    private final HongniangMatchCaseDao matchCaseDao;
    private final HongniangGroupTouchTaskDao touchTaskDao;
    private final HongniangGroupTouchLogDao touchLogDao;
    private final HongniangGroupTaskExecutionDao taskExecutionDao;
    private final HongniangService hongniangService;
    private final HongniangUserRelationService relationService;
    private final AppUserService appUserService;
    private final Map<Integer, GroupTouchProvider> providerMap;

    public HongniangWechatGroupServiceImpl(HongniangWechatGroupUserDao groupUserDao,
                                           HongniangMatchCaseGroupDao caseGroupDao,
                                           HongniangMatchCaseDao matchCaseDao,
                                           HongniangGroupTouchTaskDao touchTaskDao,
                                           HongniangGroupTouchLogDao touchLogDao,
                                           HongniangGroupTaskExecutionDao taskExecutionDao,
                                           HongniangService hongniangService,
                                           HongniangUserRelationService relationService,
                                           AppUserService appUserService,
                                           List<GroupTouchProvider> providers) {
        this.groupUserDao = groupUserDao;
        this.caseGroupDao = caseGroupDao;
        this.matchCaseDao = matchCaseDao;
        this.touchTaskDao = touchTaskDao;
        this.touchLogDao = touchLogDao;
        this.taskExecutionDao = taskExecutionDao;
        this.hongniangService = hongniangService;
        this.relationService = relationService;
        this.appUserService = appUserService;
        this.providerMap = providers.stream().collect(Collectors.toMap(GroupTouchProvider::providerType, Function.identity()));
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String hongniangName = trim((String) params.get("hongniangName"));
        String keyword = trim((String) params.get("keyword"));
        String city = trim((String) params.get("city"));
        Integer hongniangId = parseInteger(params.get("hongniangId"));
        Integer groupType = parseInteger(params.get("groupType"));
        Integer syncStatus = parseInteger(params.get("syncStatus"));
        String tagKeyword = trim((String) params.get("tagKeyword"));

        Set<Integer> matchedHongniangIds = hongniangId == null ? null : new LinkedHashSet<>(List.of(hongniangId));
        if (StringUtils.isNotBlank(hongniangName)) {
            Set<Integer> ids = hongniangService.lambdaQuery()
                .like(HongniangInfoEntity::getHongniangName, hongniangName)
                .list()
                .stream()
                .map(HongniangInfoEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            matchedHongniangIds = matchedHongniangIds == null ? ids : matchedHongniangIds.stream().filter(ids::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            if (matchedHongniangIds.isEmpty()) {
                return emptyPage(params);
            }
        }

        LambdaQueryWrapper<HongniangWechatGroupEntity> wrapper = new LambdaQueryWrapper<>();
        if (matchedHongniangIds != null) {
            wrapper.in(HongniangWechatGroupEntity::getHongniangId, matchedHongniangIds);
        }
        if (groupType != null) {
            wrapper.eq(HongniangWechatGroupEntity::getGroupType, groupType);
        }
        if (syncStatus != null) {
            wrapper.eq(HongniangWechatGroupEntity::getSyncStatus, syncStatus);
        }
        if (StringUtils.isNotBlank(city)) {
            wrapper.like(HongniangWechatGroupEntity::getCity, city);
        }
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(query -> query.like(HongniangWechatGroupEntity::getGroupName, keyword)
                .or().like(HongniangWechatGroupEntity::getOwnerName, keyword)
                .or().like(HongniangWechatGroupEntity::getOwnerWechat, keyword));
        }
        if (StringUtils.isNotBlank(tagKeyword)) {
            wrapper.like(HongniangWechatGroupEntity::getTagJson, tagKeyword);
        }
        wrapper.orderByDesc(HongniangWechatGroupEntity::getUpdateTime)
            .orderByDesc(HongniangWechatGroupEntity::getId);
        IPage<HongniangWechatGroupEntity> page = this.page(new Query<HongniangWechatGroupEntity>().getPage(params), wrapper);
        List<Map<String, Object>> rows = enrichGroups(page.getRecords());
        return new PageUtils(rows, (int) page.getTotal(), (int) page.getSize(), (int) page.getCurrent());
    }

    @Override
    public Map<String, Object> getDetail(Integer groupId) {
        HongniangWechatGroupEntity group = this.getById(groupId);
        if (group == null) {
            throw new LinfengException("微信群不存在");
        }
        Map<String, Object> detail = enrichGroup(group);
        List<HongniangWechatGroupUserEntity> bindings = groupUserDao.selectList(new LambdaQueryWrapper<HongniangWechatGroupUserEntity>()
            .eq(HongniangWechatGroupUserEntity::getGroupId, groupId)
            .orderByDesc(HongniangWechatGroupUserEntity::getCreateTime));
        Map<Integer, AppUserEntity> userMap = appUserService.listByIds(bindings.stream()
                .map(HongniangWechatGroupUserEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .collect(Collectors.toMap(AppUserEntity::getUid, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<Map<String, Object>> users = bindings.stream()
            .map(item -> {
                AppUserEntity user = userMap.get(item.getUserId());
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", item.getId());
                map.put("userId", item.getUserId());
                map.put("username", user == null ? null : user.getUsername());
                map.put("mobile", user == null ? null : user.getMobile());
                map.put("joinTime", item.getJoinTime());
                map.put("remark", item.getRemark());
                return map;
            })
            .collect(Collectors.toList());
        List<HongniangMatchCaseGroupEntity> caseBindings = caseGroupDao.selectList(new LambdaQueryWrapper<HongniangMatchCaseGroupEntity>()
            .eq(HongniangMatchCaseGroupEntity::getGroupId, groupId));
        Map<Integer, HongniangMatchCaseEntity> caseMap = matchCaseDao.selectBatchIds(caseBindings.stream()
                .map(HongniangMatchCaseGroupEntity::getCaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .collect(Collectors.toMap(HongniangMatchCaseEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<Map<String, Object>> cases = caseBindings.stream()
            .map(item -> {
                HongniangMatchCaseEntity matchCase = caseMap.get(item.getCaseId());
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("caseId", item.getCaseId());
                map.put("currentStage", matchCase == null ? null : matchCase.getCurrentStage());
                map.put("currentStageLabel", HongniangMatchCaseServiceImpl.stageLabel(matchCase == null ? null : matchCase.getCurrentStage()));
                return map;
            })
            .collect(Collectors.toList());
        List<HongniangGroupTouchTaskEntity> tasks = touchTaskDao.selectList(new LambdaQueryWrapper<HongniangGroupTouchTaskEntity>()
            .eq(HongniangGroupTouchTaskEntity::getGroupId, groupId)
            .orderByDesc(HongniangGroupTouchTaskEntity::getCreateTime)
            .last("limit 20"));
        detail.put("users", users);
        detail.put("cases", cases);
        detail.put("tasks", tasks);
        detail.put("executions", listTaskExecutions(null, groupId, 20));
        return detail;
    }

    @Override
    @DSTransactional
    public void saveGroup(HongniangWechatGroupEntity entity) {
        validateGroup(entity);
        entity.setSyncStatus(defaultSyncStatus(entity));
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        this.save(entity);
    }

    @Override
    @DSTransactional
    public void updateGroup(HongniangWechatGroupEntity entity) {
        HongniangWechatGroupEntity current = this.getById(entity.getId());
        if (current == null) {
            throw new LinfengException("微信群不存在");
        }
        validateGroup(entity);
        current.setHongniangId(entity.getHongniangId());
        current.setGroupName(entity.getGroupName());
        current.setGroupType(entity.getGroupType());
        current.setOwnerName(entity.getOwnerName());
        current.setOwnerWechat(entity.getOwnerWechat());
        current.setTagJson(entity.getTagJson());
        current.setCity(entity.getCity());
        current.setPurpose(entity.getPurpose());
        current.setQrCodeUrl(entity.getQrCodeUrl());
        current.setJoinLink(entity.getJoinLink());
        current.setExternalGroupId(entity.getExternalGroupId());
        current.setProviderType(entity.getProviderType());
        current.setRemark(entity.getRemark());
        current.setUpdateTime(new Date());
        this.updateById(current);
    }

    @Override
    @DSTransactional
    public void bindUsers(Integer groupId, List<Integer> userIds) {
        HongniangWechatGroupEntity group = this.getById(groupId);
        if (group == null) {
            throw new LinfengException("微信群不存在");
        }
        groupUserDao.delete(new LambdaQueryWrapper<HongniangWechatGroupUserEntity>()
            .eq(HongniangWechatGroupUserEntity::getGroupId, groupId));
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<HongniangUserRelationEntity> relations = relationService.lambdaQuery()
            .eq(HongniangUserRelationEntity::getHongniangId, group.getHongniangId())
            .in(HongniangUserRelationEntity::getUserId, userIds)
            .list();
        Map<Integer, HongniangUserRelationEntity> relationMap = relations.stream()
            .collect(Collectors.toMap(HongniangUserRelationEntity::getUserId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Date now = new Date();
        for (Integer userId : new LinkedHashSet<>(userIds)) {
            if (userId == null || userId <= 0) {
                continue;
            }
            HongniangUserRelationEntity relation = relationMap.get(userId);
            if (relation == null) {
                throw new LinfengException("仅支持绑定当前红娘用户池中的用户");
            }
            HongniangWechatGroupUserEntity entity = new HongniangWechatGroupUserEntity();
            entity.setGroupId(groupId);
            entity.setUserId(userId);
            entity.setRelationId(relation.getId());
            entity.setCreateTime(now);
            groupUserDao.insert(entity);
        }
    }

    @Override
    @DSTransactional
    public void bindCases(Integer groupId, List<Integer> caseIds) {
        HongniangWechatGroupEntity group = this.getById(groupId);
        if (group == null) {
            throw new LinfengException("微信群不存在");
        }
        caseGroupDao.delete(new LambdaQueryWrapper<HongniangMatchCaseGroupEntity>()
            .eq(HongniangMatchCaseGroupEntity::getGroupId, groupId));
        if (caseIds == null || caseIds.isEmpty()) {
            return;
        }
        List<HongniangMatchCaseEntity> cases = matchCaseDao.selectBatchIds(new LinkedHashSet<>(caseIds));
        Map<Integer, HongniangMatchCaseEntity> caseMap = cases.stream()
            .collect(Collectors.toMap(HongniangMatchCaseEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Date now = new Date();
        for (Integer caseId : new LinkedHashSet<>(caseIds)) {
            if (caseId == null || caseId <= 0) {
                continue;
            }
            HongniangMatchCaseEntity matchCase = caseMap.get(caseId);
            if (matchCase == null || !Objects.equals(matchCase.getHongniangId(), group.getHongniangId())) {
                throw new LinfengException("仅支持绑定当前红娘名下案件");
            }
            HongniangMatchCaseGroupEntity entity = new HongniangMatchCaseGroupEntity();
            entity.setCaseId(caseId);
            entity.setGroupId(groupId);
            entity.setCreateTime(now);
            caseGroupDao.insert(entity);
        }
    }

    @Override
    @DSTransactional
    public Map<String, Object> syncGroup(Integer groupId) {
        HongniangWechatGroupEntity group = this.getById(groupId);
        if (group == null) {
            throw new LinfengException("微信群不存在");
        }
        GroupTouchProvider provider = providerMap.get(group.getProviderType());
        if (provider == null) {
            throw new LinfengException("当前群接入方式不支持同步");
        }
        ProviderResult result = provider.syncGroup(group);
        applySyncResult(group, result);
        group.setSyncStatus(result.isSuccess() ? 1 : 2);
        group.setLastSyncTime(new Date());
        group.setUpdateTime(new Date());
        this.updateById(group);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.isSuccess());
        response.put("msg", result.getSummary());
        response.put("rawResponse", result.getRawResponse());
        return response;
    }

    @Override
    @DSTransactional
    public HongniangGroupTouchTaskEntity createTouchTask(HongniangGroupTouchTaskEntity task) {
        HongniangWechatGroupEntity group = this.getById(task.getGroupId());
        if (group == null) {
            throw new LinfengException("微信群不存在");
        }
        task.setHongniangId(group.getHongniangId());
        task.setProviderType(group.getProviderType());
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        if (group.getGroupType() == 1 || group.getProviderType() == 0) {
            task.setTaskStatus(4);
            task.setResultSummary("现有微信群仅做运营登记，不执行真实发送");
            touchTaskDao.insert(task);
            insertTouchLog(task.getId(), task.getGroupId(), 3, task.getResultSummary());
            insertExecution(task, 4, task.getContentPayload(), null, task.getResultSummary(), task.getResultSummary());
            return task;
        }
        GroupTouchProvider provider = providerMap.get(group.getProviderType());
        if (provider == null) {
            task.setTaskStatus(3);
            task.setResultSummary("当前接入方式未实现发送能力");
            touchTaskDao.insert(task);
            insertTouchLog(task.getId(), task.getGroupId(), 2, task.getResultSummary());
            insertExecution(task, 3, task.getContentPayload(), null, task.getResultSummary(), task.getResultSummary());
            return task;
        }
        if (task.getScheduleTime() != null && task.getScheduleTime().after(new Date())) {
            task.setTaskStatus(0);
            task.setResultSummary("任务已排队，等待计划时间执行");
            touchTaskDao.insert(task);
            insertExecution(task, 0, task.getContentPayload(), null, task.getResultSummary(), task.getResultSummary());
            return task;
        }
        task.setTaskStatus(1);
        touchTaskDao.insert(task);
        ProviderResult result = provider.submitTask(group, task);
        task.setProviderTaskId(result.getProviderTaskId());
        task.setResultSummary(result.getSummary());
        task.setTaskStatus(result.isSuccess() ? 2 : 3);
        task.setUpdateTime(new Date());
        touchTaskDao.updateById(task);
        insertTouchLog(task.getId(), task.getGroupId(), result.isSuccess() ? 1 : 2, result.getRawResponse());
        insertExecution(task, result.isSuccess() ? 2 : 3, task.getContentPayload(), result.getProviderTaskId(), result.getSummary(), result.getRawResponse());
        return task;
    }

    @Override
    public PageUtils listTouchTasks(Map<String, Object> params) {
        LambdaQueryWrapper<HongniangGroupTouchTaskEntity> wrapper = new LambdaQueryWrapper<>();
        Integer groupId = parseInteger(params.get("groupId"));
        Integer hongniangId = parseInteger(params.get("hongniangId"));
        Integer taskStatus = parseInteger(params.get("taskStatus"));
        if (groupId != null) {
            wrapper.eq(HongniangGroupTouchTaskEntity::getGroupId, groupId);
        }
        if (hongniangId != null) {
            wrapper.eq(HongniangGroupTouchTaskEntity::getHongniangId, hongniangId);
        }
        if (taskStatus != null) {
            wrapper.eq(HongniangGroupTouchTaskEntity::getTaskStatus, taskStatus);
        }
        wrapper.orderByDesc(HongniangGroupTouchTaskEntity::getCreateTime);
        IPage<HongniangGroupTouchTaskEntity> page = touchTaskDao.selectPage(new Query<HongniangGroupTouchTaskEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    public PageUtils listTouchLogs(Map<String, Object> params) {
        LambdaQueryWrapper<HongniangGroupTouchLogEntity> wrapper = new LambdaQueryWrapper<>();
        Integer taskId = parseInteger(params.get("taskId"));
        Integer groupId = parseInteger(params.get("groupId"));
        if (taskId != null) {
            wrapper.eq(HongniangGroupTouchLogEntity::getTaskId, taskId);
        }
        if (groupId != null) {
            wrapper.eq(HongniangGroupTouchLogEntity::getGroupId, groupId);
        }
        wrapper.orderByDesc(HongniangGroupTouchLogEntity::getCreateTime);
        IPage<HongniangGroupTouchLogEntity> page = touchLogDao.selectPage(new Query<HongniangGroupTouchLogEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    @DSTransactional
    public Map<String, Object> retryTouchTask(Integer taskId) {
        HongniangGroupTouchTaskEntity task = touchTaskDao.selectById(taskId);
        if (task == null) {
            throw new LinfengException("触达任务不存在");
        }
        HongniangWechatGroupEntity group = this.getById(task.getGroupId());
        if (group == null) {
            throw new LinfengException("微信群不存在");
        }
        GroupTouchProvider provider = providerMap.get(group.getProviderType());
        if (provider == null) {
            throw new LinfengException("当前接入方式未实现重试能力");
        }
        ProviderResult result = provider.submitTask(group, task);
        task.setTaskStatus(result.isSuccess() ? 2 : 3);
        task.setProviderTaskId(result.getProviderTaskId());
        task.setResultSummary(result.getSummary());
        task.setUpdateTime(new Date());
        touchTaskDao.updateById(task);
        insertTouchLog(task.getId(), task.getGroupId(), result.isSuccess() ? 1 : 2, result.getRawResponse());
        insertExecution(task, result.isSuccess() ? 2 : 3, task.getContentPayload(), result.getProviderTaskId(), result.getSummary(), result.getRawResponse());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.isSuccess());
        response.put("msg", result.getSummary());
        return response;
    }

    @Override
    public List<Map<String, Object>> listTaskExecutions(Integer taskId, Integer groupId, Integer limit) {
        LambdaQueryWrapper<HongniangGroupTaskExecutionEntity> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            wrapper.eq(HongniangGroupTaskExecutionEntity::getTaskId, taskId);
        }
        if (groupId != null) {
            wrapper.eq(HongniangGroupTaskExecutionEntity::getGroupId, groupId);
        }
        int actualLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        List<HongniangGroupTaskExecutionEntity> executions = taskExecutionDao.selectList(wrapper
            .orderByDesc(HongniangGroupTaskExecutionEntity::getCreateTime)
            .last("limit " + actualLimit));
        return executions.stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("taskId", item.getTaskId());
            map.put("groupId", item.getGroupId());
            map.put("providerType", item.getProviderType());
            map.put("executionStatus", item.getExecutionStatus());
            map.put("executionStatusLabel", executionStatusLabel(item.getExecutionStatus()));
            map.put("providerTaskId", item.getProviderTaskId());
            map.put("resultSummary", item.getResultSummary());
            map.put("providerResponse", item.getProviderResponse());
            map.put("executeTime", item.getExecuteTime());
            map.put("finishTime", item.getFinishTime());
            map.put("createTime", item.getCreateTime());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> searchBindableUsers(Integer groupId, Integer hongniangId, String keyword, Integer limit) {
        if (hongniangId == null || hongniangId <= 0) {
            return List.of();
        }
        Set<Integer> boundUserIds = new LinkedHashSet<>();
        if (groupId != null && groupId > 0) {
            boundUserIds.addAll(groupUserDao.selectList(new LambdaQueryWrapper<HongniangWechatGroupUserEntity>()
                    .eq(HongniangWechatGroupUserEntity::getGroupId, groupId))
                .stream()
                .map(HongniangWechatGroupUserEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
        List<HongniangUserRelationEntity> relations = relationService.lambdaQuery()
            .eq(HongniangUserRelationEntity::getHongniangId, hongniangId)
            .list();
        Map<Integer, Integer> noMap = relations.stream()
            .collect(Collectors.toMap(HongniangUserRelationEntity::getUserId, HongniangUserRelationEntity::getHongniangUserNo,
                (left, right) -> left, LinkedHashMap::new));
        List<AppUserEntity> users = appUserService.listByIds(relations.stream()
                .map(HongniangUserRelationEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .filter(user -> !boundUserIds.contains(user.getUid()))
            .filter(user -> StringUtils.isBlank(keyword)
                || StringUtils.containsIgnoreCase(StringUtils.defaultString(user.getUsername()), keyword)
                || StringUtils.containsIgnoreCase(StringUtils.defaultString(user.getMobile()), keyword)
                || StringUtils.equals(String.valueOf(user.getUid()), keyword)
                || StringUtils.equals(String.valueOf(noMap.get(user.getUid())), keyword))
            .sorted((left, right) -> Integer.compare(right.getUid(), left.getUid()))
            .limit(limit == null ? 20 : Math.min(limit, 50))
            .collect(Collectors.toList());
        return users.stream().map(user -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("uid", user.getUid());
            map.put("username", user.getUsername());
            map.put("mobile", user.getMobile());
            map.put("hongniangUserNo", noMap.get(user.getUid()));
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> searchBindableCases(Integer groupId, Integer hongniangId, String keyword, Integer limit) {
        if (hongniangId == null || hongniangId <= 0) {
            return List.of();
        }
        Set<Integer> boundCaseIds = new LinkedHashSet<>();
        if (groupId != null && groupId > 0) {
            boundCaseIds.addAll(caseGroupDao.selectList(new LambdaQueryWrapper<HongniangMatchCaseGroupEntity>()
                    .eq(HongniangMatchCaseGroupEntity::getGroupId, groupId))
                .stream()
                .map(HongniangMatchCaseGroupEntity::getCaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
        List<HongniangMatchCaseEntity> cases = matchCaseDao.selectList(new LambdaQueryWrapper<HongniangMatchCaseEntity>()
            .eq(HongniangMatchCaseEntity::getHongniangId, hongniangId)
            .orderByDesc(HongniangMatchCaseEntity::getUpdateTime)
            .last("limit " + (limit == null ? 20 : Math.min(limit, 50))));
        Map<Integer, AppUserEntity> userMap = appUserService.listByIds(cases.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getMaleUserId(), item.getFemaleUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .collect(Collectors.toMap(AppUserEntity::getUid, item -> item, (left, right) -> left, LinkedHashMap::new));
        return cases.stream()
            .filter(item -> !boundCaseIds.contains(item.getId()))
            .filter(item -> {
                if (StringUtils.isBlank(keyword)) {
                    return true;
                }
                AppUserEntity male = userMap.get(item.getMaleUserId());
                AppUserEntity female = userMap.get(item.getFemaleUserId());
                return (male != null && (StringUtils.containsIgnoreCase(male.getUsername(), keyword)
                    || StringUtils.containsIgnoreCase(StringUtils.defaultString(male.getMobile()), keyword)))
                    || (female != null && (StringUtils.containsIgnoreCase(female.getUsername(), keyword)
                    || StringUtils.containsIgnoreCase(StringUtils.defaultString(female.getMobile()), keyword)))
                    || StringUtils.equals(String.valueOf(item.getId()), keyword);
            })
            .map(item -> {
                AppUserEntity male = userMap.get(item.getMaleUserId());
                AppUserEntity female = userMap.get(item.getFemaleUserId());
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", item.getId());
                map.put("label", String.format("#%s %s / %s",
                    item.getId(),
                    male == null ? item.getMaleUserId() : male.getUsername(),
                    female == null ? item.getFemaleUserId() : female.getUsername()));
                map.put("currentStage", item.getCurrentStage());
                return map;
            })
            .collect(Collectors.toList());
    }

    private void validateGroup(HongniangWechatGroupEntity entity) {
        if (entity == null || entity.getHongniangId() == null || entity.getHongniangId() <= 0) {
            throw new LinfengException("红娘不能为空");
        }
        if (StringUtils.isBlank(entity.getGroupName())) {
            throw new LinfengException("群名称不能为空");
        }
        if (entity.getGroupType() == null) {
            entity.setGroupType(1);
        }
        if (entity.getProviderType() == null) {
            entity.setProviderType(entity.getGroupType() == 2 ? 1 : 0);
        }
        if (entity.getGroupType() == 2 && entity.getProviderType() == 0) {
            throw new LinfengException("企微客户群必须指定企业微信或 SCRM 接入方式");
        }
    }

    private Integer defaultSyncStatus(HongniangWechatGroupEntity entity) {
        return entity.getProviderType() != null && entity.getProviderType() > 0 ? 0 : 1;
    }

    private void applySyncResult(HongniangWechatGroupEntity group, ProviderResult result) {
        if (group == null || result == null || result.getData() == null || result.getData().isEmpty()) {
            return;
        }
        String groupName = trim(result.getData().get("groupName") == null ? null : String.valueOf(result.getData().get("groupName")));
        String ownerWechat = trim(result.getData().get("ownerWechat") == null ? null : String.valueOf(result.getData().get("ownerWechat")));
        if (StringUtils.isNotBlank(groupName)) {
            group.setGroupName(groupName);
        }
        if (StringUtils.isNotBlank(ownerWechat)) {
            group.setOwnerWechat(ownerWechat);
            if (StringUtils.isBlank(group.getOwnerName())) {
                group.setOwnerName(ownerWechat);
            }
        }
    }

    private void insertTouchLog(Integer taskId, Integer groupId, Integer sendStatus, String response) {
        HongniangGroupTouchLogEntity log = new HongniangGroupTouchLogEntity();
        log.setTaskId(taskId);
        log.setGroupId(groupId);
        log.setSendStatus(sendStatus);
        log.setProviderResponse(response);
        log.setSentTime(new Date());
        log.setCreateTime(new Date());
        touchLogDao.insert(log);
    }

    private void insertExecution(HongniangGroupTouchTaskEntity task,
                                 Integer executionStatus,
                                 String requestPayload,
                                 String providerTaskId,
                                 String resultSummary,
                                 String providerResponse) {
        HongniangGroupTaskExecutionEntity execution = new HongniangGroupTaskExecutionEntity();
        execution.setTaskId(task.getId());
        execution.setGroupId(task.getGroupId());
        execution.setProviderType(task.getProviderType());
        execution.setExecutionStatus(executionStatus);
        execution.setRequestPayload(requestPayload);
        execution.setProviderTaskId(providerTaskId);
        execution.setProviderResponse(providerResponse);
        execution.setResultSummary(resultSummary);
        execution.setExecuteTime(new Date());
        execution.setFinishTime(new Date());
        execution.setCreateTime(new Date());
        execution.setUpdateTime(new Date());
        taskExecutionDao.insert(execution);
    }

    private List<Map<String, Object>> enrichGroups(List<HongniangWechatGroupEntity> groups) {
        return groups.stream().map(this::enrichGroup).collect(Collectors.toList());
    }

    private Map<String, Object> enrichGroup(HongniangWechatGroupEntity group) {
        Map<Integer, HongniangInfoEntity> hongniangMap = loadHongniangs(java.util.stream.Stream.of(group.getHongniangId())
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
        int boundUserCount = Math.toIntExact(groupUserDao.selectCount(new LambdaQueryWrapper<HongniangWechatGroupUserEntity>()
            .eq(HongniangWechatGroupUserEntity::getGroupId, group.getId())));
        int boundCaseCount = Math.toIntExact(caseGroupDao.selectCount(new LambdaQueryWrapper<HongniangMatchCaseGroupEntity>()
            .eq(HongniangMatchCaseGroupEntity::getGroupId, group.getId())));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", group.getId());
        map.put("hongniangId", group.getHongniangId());
        map.put("hongniangName", resolveHongniangName(hongniangMap.get(group.getHongniangId())));
        map.put("groupName", group.getGroupName());
        map.put("groupType", group.getGroupType());
        map.put("providerType", group.getProviderType());
        map.put("ownerName", group.getOwnerName());
        map.put("ownerWechat", group.getOwnerWechat());
        map.put("tagJson", group.getTagJson());
        map.put("city", group.getCity());
        map.put("purpose", group.getPurpose());
        map.put("qrCodeUrl", group.getQrCodeUrl());
        map.put("joinLink", group.getJoinLink());
        map.put("externalGroupId", group.getExternalGroupId());
        map.put("syncStatus", group.getSyncStatus());
        map.put("syncStatusLabel", syncLabel(group.getSyncStatus()));
        map.put("lastSyncTime", group.getLastSyncTime());
        map.put("remark", group.getRemark());
        map.put("boundUserCount", boundUserCount);
        map.put("boundCaseCount", boundCaseCount);
        map.put("createTime", group.getCreateTime());
        map.put("updateTime", group.getUpdateTime());
        return map;
    }

    private Map<Integer, HongniangInfoEntity> loadHongniangs(Collection<Integer> ids) {
        LinkedHashSet<Integer> unique = ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (unique.isEmpty()) {
            return Map.of();
        }
        return hongniangService.listByIds(unique).stream()
            .collect(Collectors.toMap(HongniangInfoEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private PageUtils emptyPage(Map<String, Object> params) {
        int pageSize = parseInteger(params.get("pageSize")) == null ? 10 : parseInteger(params.get("pageSize"));
        int pageNum = parseInteger(params.get("pageNum")) == null ? 1 : parseInteger(params.get("pageNum"));
        return new PageUtils(List.of(), 0, pageSize, pageNum);
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

    private String resolveHongniangName(HongniangInfoEntity hongniang) {
        return hongniang == null ? null : hongniang.getHongniangName();
    }

    private String syncLabel(Integer syncStatus) {
        if (syncStatus == null) {
            return "-";
        }
        return switch (syncStatus) {
            case 0 -> "未同步";
            case 1 -> "已同步";
            case 2 -> "同步失败";
            default -> String.valueOf(syncStatus);
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
}
