package org.aileme.shejiao.admin.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.aileme.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.admin.dao.HongniangMatchCaseDao;
import org.aileme.shejiao.admin.dao.HongniangMatchCaseGroupDao;
import org.aileme.shejiao.admin.dao.HongniangMatchProgressDao;
import org.aileme.shejiao.admin.dao.HongniangMatchRequestDao;
import org.aileme.shejiao.admin.dao.HongniangWechatGroupDao;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.FriendService;
import org.aileme.shejiao.api.service.HongniangMatchCaseService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.api.service.HongniangUserRelationService;
import org.aileme.shejiao.api.service.XiangqinActivityService;
import org.aileme.shejiao.app.biz.HongniangMpNoticeService;
import org.aileme.shejiao.app.service.impl.SocialIntentMessageService;
import org.aileme.shejiao.app.dao.SmartMatchSnapshotItemDao;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchCaseEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchCaseGroupEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchProgressEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchRequestEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangWechatGroupEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinActivityEntity;
import org.aileme.shejiao.domain.entity.app.SmartMatchSnapshotItemEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@DS("master")
@Service("hongniangMatchCaseService")
public class HongniangMatchCaseServiceImpl extends ServiceImpl<HongniangMatchCaseDao, HongniangMatchCaseEntity> implements HongniangMatchCaseService {

    public static final int STAGE_DRAFT = 0;
    public static final int STAGE_RECOMMENDING = 1;
    public static final int STAGE_CONTACTED = 2;
    public static final int STAGE_MET = 3;
    public static final int STAGE_DATING = 4;
    public static final int STAGE_MEET_PARENTS = 5;
    public static final int STAGE_MARRIED = 6;
    public static final int STAGE_CHILD_BIRTH = 7;
    public static final int STAGE_CLOSED = 8;
    public static final int MATCH_REQUEST_CHANNEL_APP = 1;
    public static final int MATCH_REQUEST_CHANNEL_SHARE = 2;
    public static final int MATCH_REQUEST_CHANNEL_MP = 3;
    public static final int MATCH_REQUEST_STATUS_PENDING = 0;
    public static final int MATCH_REQUEST_STATUS_SENT = 1;
    public static final int MATCH_REQUEST_STATUS_ACCEPTED = 2;
    public static final int MATCH_REQUEST_STATUS_REJECTED = 3;
    public static final int MATCH_REQUEST_STATUS_EXPIRED = 4;

    private final HongniangMatchProgressDao progressDao;
    private final HongniangMatchCaseGroupDao caseGroupDao;
    private final HongniangMatchRequestDao matchRequestDao;
    private final HongniangWechatGroupDao wechatGroupDao;
    private final AppUserService appUserService;
    private final HongniangService hongniangService;
    private final HongniangUserRelationService relationService;
    private final SmartMatchSnapshotItemDao smartMatchSnapshotItemDao;
    private final XiangqinActivityService xiangqinActivityService;
    private final FriendService friendService;
    private final SocialIntentMessageService socialIntentMessageService;
    private final HongniangMpNoticeService hongniangMpNoticeService;

    public HongniangMatchCaseServiceImpl(HongniangMatchProgressDao progressDao,
                                         HongniangMatchCaseGroupDao caseGroupDao,
                                         HongniangMatchRequestDao matchRequestDao,
                                         HongniangWechatGroupDao wechatGroupDao,
                                         AppUserService appUserService,
                                         HongniangService hongniangService,
                                         HongniangUserRelationService relationService,
                                         SmartMatchSnapshotItemDao smartMatchSnapshotItemDao,
                                         XiangqinActivityService xiangqinActivityService,
                                         FriendService friendService,
                                         SocialIntentMessageService socialIntentMessageService,
                                         HongniangMpNoticeService hongniangMpNoticeService) {
        this.progressDao = progressDao;
        this.caseGroupDao = caseGroupDao;
        this.matchRequestDao = matchRequestDao;
        this.wechatGroupDao = wechatGroupDao;
        this.appUserService = appUserService;
        this.hongniangService = hongniangService;
        this.relationService = relationService;
        this.smartMatchSnapshotItemDao = smartMatchSnapshotItemDao;
        this.xiangqinActivityService = xiangqinActivityService;
        this.friendService = friendService;
        this.socialIntentMessageService = socialIntentMessageService;
        this.hongniangMpNoticeService = hongniangMpNoticeService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String hongniangName = trim((String) params.get("hongniangName"));
        String maleKeyword = trim((String) params.get("maleKeyword"));
        String femaleKeyword = trim((String) params.get("femaleKeyword"));
        Integer currentStage = parseInteger(params.get("currentStage"));
        Integer sourceType = parseInteger(params.get("sourceType"));
        String nextFollowDate = trim((String) params.get("nextFollowDate"));
        Integer hongniangId = parseInteger(params.get("hongniangId"));

        Set<Integer> matchedHongniangIds = hongniangId == null ? null : new LinkedHashSet<>(List.of(hongniangId));
        if (StringUtils.isNotBlank(hongniangName)) {
            Set<Integer> ids = hongniangService.lambdaQuery()
                .like(HongniangInfoEntity::getHongniangName, hongniangName)
                .list()
                .stream()
                .map(HongniangInfoEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            matchedHongniangIds = mergeIds(matchedHongniangIds, ids);
            if (matchedHongniangIds.isEmpty()) {
                return emptyPage(params);
            }
        }

        Set<Integer> maleUserIds = resolveUserIdsByKeyword(maleKeyword);
        if (maleKeyword != null && maleUserIds.isEmpty()) {
            return emptyPage(params);
        }
        Set<Integer> femaleUserIds = resolveUserIdsByKeyword(femaleKeyword);
        if (femaleKeyword != null && femaleUserIds.isEmpty()) {
            return emptyPage(params);
        }

        LambdaQueryWrapper<HongniangMatchCaseEntity> wrapper = new LambdaQueryWrapper<>();
        if (matchedHongniangIds != null) {
            wrapper.in(HongniangMatchCaseEntity::getHongniangId, matchedHongniangIds);
        }
        if (maleUserIds != null) {
            wrapper.in(HongniangMatchCaseEntity::getMaleUserId, maleUserIds);
        }
        if (femaleUserIds != null) {
            wrapper.in(HongniangMatchCaseEntity::getFemaleUserId, femaleUserIds);
        }
        if (currentStage != null) {
            wrapper.eq(HongniangMatchCaseEntity::getCurrentStage, currentStage);
        }
        if (sourceType != null) {
            wrapper.eq(HongniangMatchCaseEntity::getSourceType, sourceType);
        }
        if (StringUtils.isNotBlank(nextFollowDate)) {
            wrapper.apply("date(next_follow_time) = {0}", nextFollowDate);
        }
        wrapper.orderByAsc(HongniangMatchCaseEntity::getNextFollowTime)
            .orderByDesc(HongniangMatchCaseEntity::getUpdateTime)
            .orderByDesc(HongniangMatchCaseEntity::getId);

        IPage<HongniangMatchCaseEntity> page = this.page(new Query<HongniangMatchCaseEntity>().getPage(params), wrapper);
        List<Map<String, Object>> rows = enrichCaseRows(page.getRecords());
        return new PageUtils(rows, (int) page.getTotal(), (int) page.getSize(), (int) page.getCurrent());
    }

    @Override
    public Map<String, Object> getDetail(Integer caseId) {
        HongniangMatchCaseEntity matchCase = this.getById(caseId);
        if (matchCase == null) {
            throw new LinfengException("案件不存在");
        }
        Map<String, Object> detail = enrichCaseRow(matchCase);
        List<HongniangMatchProgressEntity> progressList = progressDao.selectList(new LambdaQueryWrapper<HongniangMatchProgressEntity>()
            .eq(HongniangMatchProgressEntity::getCaseId, caseId)
            .orderByDesc(HongniangMatchProgressEntity::getCreateTime)
            .orderByDesc(HongniangMatchProgressEntity::getId));
        List<HongniangMatchCaseGroupEntity> bindings = caseGroupDao.selectList(new LambdaQueryWrapper<HongniangMatchCaseGroupEntity>()
            .eq(HongniangMatchCaseGroupEntity::getCaseId, caseId));
        Map<Integer, HongniangWechatGroupEntity> groupMap = wechatGroupDao.selectBatchIds(bindings.stream()
                .map(HongniangMatchCaseGroupEntity::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .collect(Collectors.toMap(HongniangWechatGroupEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<Map<String, Object>> groups = bindings.stream()
            .map(item -> {
                HongniangWechatGroupEntity group = groupMap.get(item.getGroupId());
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("groupId", item.getGroupId());
                map.put("groupName", group == null ? null : group.getGroupName());
                map.put("groupType", group == null ? null : group.getGroupType());
                map.put("providerType", group == null ? null : group.getProviderType());
                return map;
            })
            .collect(Collectors.toList());
        List<HongniangMatchRequestEntity> requestEntities = matchRequestDao.selectList(new LambdaQueryWrapper<HongniangMatchRequestEntity>()
            .eq(HongniangMatchRequestEntity::getCaseId, caseId)
            .orderByDesc(HongniangMatchRequestEntity::getCreateTime)
            .orderByDesc(HongniangMatchRequestEntity::getId));
        detail.put("progressList", progressList);
        detail.put("groups", groups);
        detail.put("requestList", requestEntities.stream().map(this::buildRequestRow).collect(Collectors.toList()));
        return detail;
    }

    @Override
    @DSTransactional
    public void createCase(HongniangMatchCaseEntity entity, Long operatorId) {
        validateBaseCase(entity);
        entity.setCurrentStage(defaultStage(entity.getCurrentStage()));
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        if (hasOpenDuplicate(entity.getHongniangId(), entity.getMaleUserId(), entity.getFemaleUserId(), null)) {
            throw new LinfengException("当前红娘下已存在同一对男女的未关闭案件");
        }
        this.save(entity);
        insertProgress(entity.getId(), 1, null, entity.getCurrentStage(), "创建牵线案件", entity.getNextFollowTime(), null, null, operatorId);
        hongniangService.updateStatistics(entity.getHongniangId());
    }

    @Override
    @DSTransactional
    public void updateCase(HongniangMatchCaseEntity entity, Long operatorId) {
        HongniangMatchCaseEntity current = this.getById(entity.getId());
        if (current == null) {
            throw new LinfengException("案件不存在");
        }
        validateBaseCase(entity);
        if (hasOpenDuplicate(entity.getHongniangId(), entity.getMaleUserId(), entity.getFemaleUserId(), entity.getId())) {
            throw new LinfengException("当前红娘下已存在同一对男女的未关闭案件");
        }
        current.setHongniangId(entity.getHongniangId());
        current.setMaleUserId(entity.getMaleUserId());
        current.setFemaleUserId(entity.getFemaleUserId());
        current.setSourceType(defaultSourceType(entity.getSourceType()));
        current.setSourceRefId(entity.getSourceRefId());
        current.setNextFollowTime(entity.getNextFollowTime());
        current.setRemark(entity.getRemark());
        current.setUpdateTime(new Date());
        this.updateById(current);
        if (!Objects.equals(current.getHongniangId(), entity.getHongniangId())) {
            hongniangService.updateStatistics(entity.getHongniangId());
        }
        hongniangService.updateStatistics(current.getHongniangId());
    }

    @Override
    @DSTransactional
    public void advanceStage(Integer caseId, Integer targetStage, String content, Date actualFollowTime, Date nextFollowTime,
                             String attachments, Long operatorId) {
        HongniangMatchCaseEntity current = this.getById(caseId);
        if (current == null) {
            throw new LinfengException("案件不存在");
        }
        if (targetStage == null || targetStage < STAGE_DRAFT || targetStage > STAGE_CHILD_BIRTH) {
            throw new LinfengException("目标阶段不合法");
        }
        Integer oldStage = current.getCurrentStage();
        current.setCurrentStage(targetStage);
        current.setLastFollowTime(actualFollowTime == null ? new Date() : actualFollowTime);
        current.setNextFollowTime(nextFollowTime);
        current.setUpdateTime(new Date());
        this.updateById(current);
        insertProgress(caseId, 2, oldStage, targetStage,
            StringUtils.defaultIfBlank(content, "推进到" + stageLabel(targetStage)),
            nextFollowTime, actualFollowTime, attachments, operatorId);
        hongniangService.updateStatistics(current.getHongniangId());
    }

    @Override
    @DSTransactional
    public void addProgress(Integer caseId, Integer progressType, String content, Date plannedFollowTime, Date actualFollowTime,
                            String attachments, Long operatorId) {
        HongniangMatchCaseEntity current = this.getById(caseId);
        if (current == null) {
            throw new LinfengException("案件不存在");
        }
        int actualType = progressType == null ? 3 : progressType;
        insertProgress(caseId, actualType, current.getCurrentStage(), current.getCurrentStage(), content,
            plannedFollowTime, actualFollowTime, attachments, operatorId);
        if (actualFollowTime != null) {
            current.setLastFollowTime(actualFollowTime);
        }
        if (plannedFollowTime != null) {
            current.setNextFollowTime(plannedFollowTime);
        }
        current.setUpdateTime(new Date());
        this.updateById(current);
    }

    @Override
    @DSTransactional
    public void closeCase(Integer caseId, String closeReason, String content, Long operatorId) {
        if (StringUtils.isBlank(closeReason)) {
            throw new LinfengException("关闭原因不能为空");
        }
        HongniangMatchCaseEntity current = this.getById(caseId);
        if (current == null) {
            throw new LinfengException("案件不存在");
        }
        Integer oldStage = current.getCurrentStage();
        current.setCurrentStage(STAGE_CLOSED);
        current.setCloseReason(closeReason.trim());
        current.setLastFollowTime(new Date());
        current.setUpdateTime(new Date());
        this.updateById(current);
        insertProgress(caseId, 4, oldStage, STAGE_CLOSED,
            StringUtils.defaultIfBlank(content, closeReason.trim()), null, new Date(), null, operatorId);
        hongniangService.updateStatistics(current.getHongniangId());
    }

    @Override
    @DSTransactional
    public void bindGroups(Integer caseId, List<Integer> groupIds) {
        HongniangMatchCaseEntity current = this.getById(caseId);
        if (current == null) {
            throw new LinfengException("案件不存在");
        }
        caseGroupDao.delete(new LambdaQueryWrapper<HongniangMatchCaseGroupEntity>()
            .eq(HongniangMatchCaseGroupEntity::getCaseId, caseId));
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        LinkedHashSet<Integer> uniqueIds = groupIds.stream()
            .filter(Objects::nonNull)
            .filter(id -> id > 0)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Date now = new Date();
        for (Integer groupId : uniqueIds) {
            HongniangMatchCaseGroupEntity item = new HongniangMatchCaseGroupEntity();
            item.setCaseId(caseId);
            item.setGroupId(groupId);
            item.setCreateTime(now);
            caseGroupDao.insert(item);
        }
    }

    @Override
    public List<Map<String, Object>> searchPoolUsers(Integer hongniangId, String keyword, Integer gender, Integer limit, Integer excludeUserId) {
        if (hongniangId == null || hongniangId <= 0) {
            return List.of();
        }
        int actualLimit = normalizeLimit(limit);
        LinkedHashSet<Integer> candidateUserIds = relationService.lambdaQuery()
            .eq(org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity::getHongniangId, hongniangId)
            .list()
            .stream()
            .filter(Objects::nonNull)
            .filter(item -> excludeUserId == null || !Objects.equals(item.getUserId(), excludeUserId))
            .map(org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (candidateUserIds.isEmpty()) {
            return List.of();
        }
        List<org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity> relations = relationService.lambdaQuery()
            .eq(org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity::getHongniangId, hongniangId)
            .in(org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity::getUserId, candidateUserIds)
            .list();
        Map<Integer, Integer> noMap = relations.stream()
            .collect(Collectors.toMap(org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity::getUserId,
                org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity::getHongniangUserNo,
                (left, right) -> left, LinkedHashMap::new));
        List<AppUserEntity> users = appUserService.listByIds(candidateUserIds).stream()
            .filter(Objects::nonNull)
            .filter(user -> gender == null || Objects.equals(user.getGender(), gender))
            .filter(user -> matchesUserKeyword(user, keyword, noMap.get(user.getUid())))
            .sorted((left, right) -> Integer.compare(right.getUid(), left.getUid()))
            .limit(actualLimit)
            .collect(Collectors.toList());
        return users.stream()
            .map(user -> buildPoolUserOption(user, noMap.get(user.getUid())))
            .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> listSourceCandidates(Integer hongniangId, Integer sourceType, String keyword, Integer limit) {
        if (hongniangId == null || hongniangId <= 0 || sourceType == null) {
            return List.of();
        }
        int actualLimit = normalizeLimit(limit);
        if (sourceType == 2) {
            Set<Integer> managedUserIds = new LinkedHashSet<>(relationService.getUserIdsByHongniangId(hongniangId));
            if (managedUserIds.isEmpty()) {
                return List.of();
            }
            List<SmartMatchSnapshotItemEntity> items = smartMatchSnapshotItemDao.selectList(new LambdaQueryWrapper<SmartMatchSnapshotItemEntity>()
                .and(wrapper -> wrapper.in(SmartMatchSnapshotItemEntity::getOwnerUserId, managedUserIds)
                    .or().in(SmartMatchSnapshotItemEntity::getTargetUserId, managedUserIds))
                .orderByDesc(SmartMatchSnapshotItemEntity::getId)
                .last("limit " + actualLimit));
            Map<Integer, AppUserEntity> userMap = loadUsers(items.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getOwnerUserId(), item.getTargetUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
            return items.stream()
                .filter(item -> matchesSnapshotKeyword(item, userMap, keyword))
                .limit(actualLimit)
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    AppUserEntity owner = userMap.get(item.getOwnerUserId());
                    AppUserEntity target = userMap.get(item.getTargetUserId());
                    map.put("id", item.getId());
                    map.put("label", String.format("%s -> %s | 匹配分:%s",
                        owner == null ? item.getOwnerUserId() : owner.getUsername(),
                        target == null ? item.getTargetUserId() : target.getUsername(),
                        item.getMatchScore() == null ? 0 : item.getMatchScore()));
                    map.put("ownerUserId", item.getOwnerUserId());
                    map.put("targetUserId", item.getTargetUserId());
                    map.put("matchScore", item.getMatchScore());
                    return map;
                })
                .collect(Collectors.toList());
        }
        if (sourceType == 3) {
            List<XiangqinActivityEntity> activities = xiangqinActivityService.getByHongniangId(hongniangId);
            return activities.stream()
                .filter(item -> StringUtils.isBlank(keyword) || StringUtils.containsIgnoreCase(item.getTitle(), keyword))
                .limit(actualLimit)
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", item.getId());
                    map.put("label", item.getTitle());
                    map.put("title", item.getTitle());
                    map.put("startTime", item.getStartTime());
                    return map;
                })
                .collect(Collectors.toList());
        }
        if (sourceType == 4) {
            List<HongniangWechatGroupEntity> groups = wechatGroupDao.selectList(new LambdaQueryWrapper<HongniangWechatGroupEntity>()
                .eq(HongniangWechatGroupEntity::getHongniangId, hongniangId)
                .orderByDesc(HongniangWechatGroupEntity::getId)
                .last("limit " + actualLimit));
            return groups.stream()
                .filter(item -> StringUtils.isBlank(keyword) || StringUtils.containsIgnoreCase(item.getGroupName(), keyword))
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", item.getId().longValue());
                    map.put("label", item.getGroupName());
                    map.put("groupName", item.getGroupName());
                    return map;
                })
                .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    @DSTransactional
    public Map<String, Object> createMatchRequest(Integer caseId,
                                                  Integer fromUserId,
                                                  Integer toUserId,
                                                  Integer requestChannel,
                                                  String requestMessage,
                                                  String wechatShareSnapshot,
                                                  Date expireTime,
                                                  Long operatorId) {
        HongniangMatchCaseEntity matchCase = this.getById(caseId);
        if (matchCase == null) {
            throw new LinfengException("案件不存在");
        }
        Integer safeChannel = requestChannel == null ? MATCH_REQUEST_CHANNEL_APP : requestChannel;
        validateMatchRequestUsers(matchCase, fromUserId, toUserId);
        if (hasOpenRequest(caseId, fromUserId, toUserId, safeChannel)) {
            throw new LinfengException("相同方向的牵线申请仍在处理中，请勿重复发起");
        }
        AppUserEntity fromUser = appUserService.getById(fromUserId);
        AppUserEntity toUser = appUserService.getById(toUserId);
        if (fromUser == null || toUser == null) {
            throw new LinfengException("用户不存在");
        }
        HongniangInfoEntity hongniang = hongniangService.getById(matchCase.getHongniangId());
        String requestId = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        Date safeExpireTime = expireTime == null ? new Date(now.getTime() + 7L * 24 * 60 * 60 * 1000) : expireTime;

        HongniangMatchRequestEntity request = new HongniangMatchRequestEntity();
        request.setCaseId(caseId);
        request.setHongniangId(matchCase.getHongniangId());
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setRequestChannel(safeChannel);
        request.setRequestStatus(MATCH_REQUEST_STATUS_PENDING);
        request.setRequestMessage(StringUtils.left(StringUtils.trimToEmpty(requestMessage), 500));
        request.setIntentRequestId(requestId);
        request.setWechatShareSnapshot(StringUtils.left(StringUtils.trimToEmpty(wechatShareSnapshot), 255));
        request.setExpireTime(safeExpireTime);
        request.setCreateTime(now);
        request.setUpdateTime(now);
        matchRequestDao.insert(request);

        String summary;
        String sessionId = null;
        if (safeChannel == MATCH_REQUEST_CHANNEL_APP) {
            Long relationSessionId = friendService.getOrCreateSession(fromUserId, toUserId);
            sessionId = String.valueOf(relationSessionId);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "hongniang_match_request");
            payload.put("requestId", requestId);
            payload.put("caseId", caseId);
            payload.put("hongniangId", matchCase.getHongniangId());
            payload.put("hongniangName", hongniang == null ? "" : hongniang.getHongniangName());
            payload.put("requesterUid", fromUserId);
            payload.put("requesterName", fromUser.getUsername());
            payload.put("targetUid", toUserId);
            payload.put("targetName", toUser.getUsername());
            payload.put("status", "pending");
            payload.put("requestChannel", safeChannel);
            payload.put("message", request.getRequestMessage());
            payload.put("expireTime", safeExpireTime.getTime());
            socialIntentMessageService.sendStructuredMessage(sessionId, fromUserId, toUserId, payload);
            request.setRequestStatus(MATCH_REQUEST_STATUS_SENT);
            request.setUpdateTime(new Date());
            matchRequestDao.updateById(request);
            summary = "牵线申请已发送到 App 私信";
        } else if (safeChannel == MATCH_REQUEST_CHANNEL_MP) {
            hongniangMpNoticeService.sendMatchRequestNotice(toUser, fromUser, hongniang, request);
            request.setRequestStatus(MATCH_REQUEST_STATUS_SENT);
            request.setUpdateTime(new Date());
            matchRequestDao.updateById(request);
            summary = "牵线申请已发送到公众号";
        } else {
            request.setRequestStatus(MATCH_REQUEST_STATUS_SENT);
            request.setUpdateTime(new Date());
            matchRequestDao.updateById(request);
            summary = "已登记为红娘代分享微信";
        }

        matchCase.setLatestRequestStatus(request.getRequestStatus());
        matchCase.setUpdateTime(new Date());
        this.updateById(matchCase);
        insertProgress(caseId, 3, matchCase.getCurrentStage(), matchCase.getCurrentStage(),
            String.format("发起牵线申请：%s -> %s（%s）",
                fromUser.getUsername(),
                toUser.getUsername(),
                requestChannelLabel(safeChannel)),
            null, null, null, operatorId);

        Map<String, Object> response = buildRequestRow(request);
        response.put("summary", summary);
        response.put("sessionId", sessionId);
        return response;
    }

    @Override
    public int countSuccessCases(Integer hongniangId) {
        if (hongniangId == null || hongniangId <= 0) {
            return 0;
        }
        return Math.toIntExact(this.lambdaQuery()
            .eq(HongniangMatchCaseEntity::getHongniangId, hongniangId)
            .in(HongniangMatchCaseEntity::getCurrentStage, List.of(STAGE_MARRIED, STAGE_CHILD_BIRTH))
            .count());
    }

    private void validateBaseCase(HongniangMatchCaseEntity entity) {
        if (entity == null || entity.getHongniangId() == null || entity.getHongniangId() <= 0) {
            throw new LinfengException("红娘不能为空");
        }
        if (entity.getMaleUserId() == null || entity.getMaleUserId() <= 0 || entity.getFemaleUserId() == null || entity.getFemaleUserId() <= 0) {
            throw new LinfengException("男女用户不能为空");
        }
        if (Objects.equals(entity.getMaleUserId(), entity.getFemaleUserId())) {
            throw new LinfengException("男方和女方不能是同一用户");
        }
        AppUserEntity male = appUserService.getById(entity.getMaleUserId());
        AppUserEntity female = appUserService.getById(entity.getFemaleUserId());
        if (male == null || female == null) {
            throw new LinfengException("关联用户不存在");
        }
        if (!Objects.equals(male.getGender(), 1)) {
            throw new LinfengException("男方用户性别必须为男");
        }
        if (!Objects.equals(female.getGender(), 2)) {
            throw new LinfengException("女方用户性别必须为女");
        }
        if (!relationService.hasPermission(entity.getHongniangId(), entity.getMaleUserId())
            || !relationService.hasPermission(entity.getHongniangId(), entity.getFemaleUserId())) {
            throw new LinfengException("案件双方必须都在当前红娘的用户池中");
        }
        entity.setSourceType(defaultSourceType(entity.getSourceType()));
    }

    private boolean hasOpenDuplicate(Integer hongniangId, Integer maleUserId, Integer femaleUserId, Integer excludeId) {
        LambdaQueryWrapper<HongniangMatchCaseEntity> wrapper = new LambdaQueryWrapper<HongniangMatchCaseEntity>()
            .eq(HongniangMatchCaseEntity::getHongniangId, hongniangId)
            .eq(HongniangMatchCaseEntity::getMaleUserId, maleUserId)
            .eq(HongniangMatchCaseEntity::getFemaleUserId, femaleUserId)
            .ne(HongniangMatchCaseEntity::getCurrentStage, STAGE_CLOSED);
        if (excludeId != null) {
            wrapper.ne(HongniangMatchCaseEntity::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    private void insertProgress(Integer caseId, Integer progressType, Integer stageBefore, Integer stageAfter, String content,
                                Date plannedFollowTime, Date actualFollowTime, String attachments, Long operatorId) {
        HongniangMatchProgressEntity progress = new HongniangMatchProgressEntity();
        progress.setCaseId(caseId);
        progress.setProgressType(progressType);
        progress.setStageBefore(stageBefore);
        progress.setStageAfter(stageAfter);
        progress.setContent(StringUtils.defaultIfBlank(content, ""));
        progress.setPlannedFollowTime(plannedFollowTime);
        progress.setActualFollowTime(actualFollowTime);
        progress.setAttachments(attachments);
        progress.setOperatorId(operatorId == null ? LoginHelper.getUserId() : operatorId);
        progress.setCreateTime(new Date());
        progressDao.insert(progress);
    }

    private List<Map<String, Object>> enrichCaseRows(List<HongniangMatchCaseEntity> records) {
        return records.stream().map(this::enrichCaseRow).collect(Collectors.toList());
    }

    private Map<String, Object> enrichCaseRow(HongniangMatchCaseEntity item) {
        Map<Integer, HongniangInfoEntity> hongniangMap = loadHongniangs(java.util.stream.Stream.of(item.getHongniangId())
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
        Map<Integer, AppUserEntity> userMap = loadUsers(java.util.stream.Stream.of(item.getMaleUserId(), item.getFemaleUserId())
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
        int groupCount = Math.toIntExact(caseGroupDao.selectCount(new LambdaQueryWrapper<HongniangMatchCaseGroupEntity>()
            .eq(HongniangMatchCaseGroupEntity::getCaseId, item.getId())));
        Map<String, Object> map = new LinkedHashMap<>();
        HongniangInfoEntity hongniang = hongniangMap.get(item.getHongniangId());
        AppUserEntity male = userMap.get(item.getMaleUserId());
        AppUserEntity female = userMap.get(item.getFemaleUserId());
        map.put("id", item.getId());
        map.put("hongniangId", item.getHongniangId());
        map.put("hongniangName", hongniang == null ? null : hongniang.getHongniangName());
        map.put("maleUserId", item.getMaleUserId());
        map.put("maleUsername", male == null ? null : male.getUsername());
        map.put("maleMobile", male == null ? null : male.getMobile());
        map.put("femaleUserId", item.getFemaleUserId());
        map.put("femaleUsername", female == null ? null : female.getUsername());
        map.put("femaleMobile", female == null ? null : female.getMobile());
        map.put("currentStage", item.getCurrentStage());
        map.put("currentStageLabel", stageLabel(item.getCurrentStage()));
        map.put("sourceType", item.getSourceType());
        map.put("sourceRefId", item.getSourceRefId());
        map.put("sourceDisplay", resolveSourceDisplay(item));
        map.put("latestRequestStatus", item.getLatestRequestStatus());
        map.put("latestRequestStatusLabel", requestStatusLabel(item.getLatestRequestStatus()));
        map.put("nextFollowTime", item.getNextFollowTime());
        map.put("lastFollowTime", item.getLastFollowTime());
        map.put("closeReason", item.getCloseReason());
        map.put("remark", item.getRemark());
        map.put("groupCount", groupCount);
        map.put("createTime", item.getCreateTime());
        map.put("updateTime", item.getUpdateTime());
        return map;
    }

    private Map<String, Object> buildRequestRow(HongniangMatchRequestEntity item) {
        Map<Integer, AppUserEntity> userMap = loadUsers(List.of(item.getFromUserId(), item.getToUserId()));
        AppUserEntity fromUser = userMap.get(item.getFromUserId());
        AppUserEntity toUser = userMap.get(item.getToUserId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("caseId", item.getCaseId());
        map.put("hongniangId", item.getHongniangId());
        map.put("fromUserId", item.getFromUserId());
        map.put("fromUserName", fromUser == null ? null : fromUser.getUsername());
        map.put("toUserId", item.getToUserId());
        map.put("toUserName", toUser == null ? null : toUser.getUsername());
        map.put("requestChannel", item.getRequestChannel());
        map.put("requestChannelLabel", requestChannelLabel(item.getRequestChannel()));
        map.put("requestStatus", item.getRequestStatus());
        map.put("requestStatusLabel", requestStatusLabel(item.getRequestStatus()));
        map.put("requestMessage", item.getRequestMessage());
        map.put("intentRequestId", item.getIntentRequestId());
        map.put("wechatShareSnapshot", item.getWechatShareSnapshot());
        map.put("expireTime", item.getExpireTime());
        map.put("createTime", item.getCreateTime());
        map.put("updateTime", item.getUpdateTime());
        return map;
    }

    private String resolveSourceDisplay(HongniangMatchCaseEntity item) {
        if (item.getSourceType() == null || item.getSourceRefId() == null) {
            return sourceLabel(item.getSourceType());
        }
        if (item.getSourceType() == 2) {
            SmartMatchSnapshotItemEntity snapshotItem = smartMatchSnapshotItemDao.selectById(item.getSourceRefId());
            if (snapshotItem != null) {
                return sourceLabel(item.getSourceType()) + " #" + snapshotItem.getId();
            }
        } else if (item.getSourceType() == 3) {
            XiangqinActivityEntity activity = xiangqinActivityService.getById(item.getSourceRefId());
            if (activity != null) {
                return sourceLabel(item.getSourceType()) + " - " + activity.getTitle();
            }
        } else if (item.getSourceType() == 4) {
            HongniangWechatGroupEntity group = wechatGroupDao.selectById(item.getSourceRefId());
            if (group != null) {
                return sourceLabel(item.getSourceType()) + " - " + group.getGroupName();
            }
        }
        return sourceLabel(item.getSourceType()) + " #" + item.getSourceRefId();
    }

    private Set<Integer> resolveUserIdsByKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        LinkedHashSet<Integer> matchedUserIds = appUserService.lambdaQuery()
            .and(wrapper -> {
                if (StringUtils.isNumeric(keyword)) {
                    Integer value = Integer.valueOf(keyword);
                    wrapper.eq(AppUserEntity::getUid, value)
                        .or().like(AppUserEntity::getMobile, keyword)
                        .or().like(AppUserEntity::getUsername, keyword);
                } else {
                    wrapper.like(AppUserEntity::getMobile, keyword)
                        .or().like(AppUserEntity::getUsername, keyword);
                }
            })
            .list()
            .stream()
            .map(AppUserEntity::getUid)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (StringUtils.isNumeric(keyword)) {
            relationService.lambdaQuery()
                .list()
                .stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getUserId() != null && item.getHongniangUserNo() != null)
                .filter(item -> StringUtils.contains(String.valueOf(item.getHongniangUserNo()), keyword))
                .map(org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity::getUserId)
                .forEach(matchedUserIds::add);
        }
        return matchedUserIds;
    }

    private boolean matchesUserKeyword(AppUserEntity user, String keyword, Integer hongniangUserNo) {
        if (StringUtils.isBlank(keyword)) {
            return true;
        }
        String normalized = keyword.trim();
        return StringUtils.containsIgnoreCase(StringUtils.defaultString(user.getUsername()), normalized)
            || StringUtils.containsIgnoreCase(StringUtils.defaultString(user.getMobile()), normalized)
            || StringUtils.equals(String.valueOf(user.getUid()), normalized)
            || (hongniangUserNo != null && StringUtils.contains(String.valueOf(hongniangUserNo), normalized));
    }

    private Map<String, Object> buildPoolUserOption(AppUserEntity user, Integer hongniangUserNo) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uid", user.getUid());
        map.put("username", user.getUsername());
        map.put("mobile", user.getMobile());
        map.put("gender", user.getGender());
        map.put("age", user.getAge());
        map.put("hongniangUserNo", hongniangUserNo);
        return map;
    }

    private boolean matchesSnapshotKeyword(SmartMatchSnapshotItemEntity item, Map<Integer, AppUserEntity> userMap, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return true;
        }
        String normalized = keyword.trim();
        AppUserEntity owner = userMap.get(item.getOwnerUserId());
        AppUserEntity target = userMap.get(item.getTargetUserId());
        return (owner != null && (StringUtils.containsIgnoreCase(owner.getUsername(), normalized)
            || StringUtils.containsIgnoreCase(owner.getMobile(), normalized)
            || StringUtils.equals(String.valueOf(owner.getUid()), normalized)))
            || (target != null && (StringUtils.containsIgnoreCase(target.getUsername(), normalized)
            || StringUtils.containsIgnoreCase(target.getMobile(), normalized)
            || StringUtils.equals(String.valueOf(target.getUid()), normalized)));
    }

    private void validateMatchRequestUsers(HongniangMatchCaseEntity matchCase, Integer fromUserId, Integer toUserId) {
        if (fromUserId == null || fromUserId <= 0 || toUserId == null || toUserId <= 0) {
            throw new LinfengException("牵线申请双方不能为空");
        }
        if (Objects.equals(fromUserId, toUserId)) {
            throw new LinfengException("牵线申请双方不能相同");
        }
        Set<Integer> caseUsers = Set.of(matchCase.getMaleUserId(), matchCase.getFemaleUserId());
        if (!caseUsers.contains(fromUserId) || !caseUsers.contains(toUserId)) {
            throw new LinfengException("申请双方必须来自当前案件");
        }
    }

    private boolean hasOpenRequest(Integer caseId, Integer fromUserId, Integer toUserId, Integer requestChannel) {
        return matchRequestDao.selectCount(new LambdaQueryWrapper<HongniangMatchRequestEntity>()
            .eq(HongniangMatchRequestEntity::getCaseId, caseId)
            .eq(HongniangMatchRequestEntity::getFromUserId, fromUserId)
            .eq(HongniangMatchRequestEntity::getToUserId, toUserId)
            .eq(HongniangMatchRequestEntity::getRequestChannel, requestChannel)
            .in(HongniangMatchRequestEntity::getRequestStatus, List.of(MATCH_REQUEST_STATUS_PENDING, MATCH_REQUEST_STATUS_SENT))) > 0;
    }

    private Map<Integer, AppUserEntity> loadUsers(Collection<Integer> userIds) {
        LinkedHashSet<Integer> ids = userIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return appUserService.listByIds(ids).stream()
            .collect(Collectors.toMap(AppUserEntity::getUid, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Integer, HongniangInfoEntity> loadHongniangs(Collection<Integer> hongniangIds) {
        LinkedHashSet<Integer> ids = hongniangIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return hongniangService.listByIds(ids).stream()
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

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, 50);
    }

    private int defaultStage(Integer stage) {
        return stage == null ? STAGE_DRAFT : stage;
    }

    private int defaultSourceType(Integer sourceType) {
        return sourceType == null ? 1 : sourceType;
    }

    private String requestChannelLabel(Integer requestChannel) {
        if (requestChannel == null || requestChannel == MATCH_REQUEST_CHANNEL_APP) {
            return "App私信申请";
        }
        if (requestChannel == MATCH_REQUEST_CHANNEL_SHARE) {
            return "红娘代分享微信";
        }
        if (requestChannel == MATCH_REQUEST_CHANNEL_MP) {
            return "公众号通知";
        }
        return "未知渠道";
    }

    private Set<Integer> mergeIds(Set<Integer> base, Set<Integer> extra) {
        if (base == null) {
            return extra == null ? null : new LinkedHashSet<>(extra);
        }
        if (extra == null) {
            return base;
        }
        base.retainAll(extra);
        return base;
    }

    public static String stageLabel(Integer stage) {
        if (stage == null) {
            return "-";
        }
        return switch (stage) {
            case STAGE_DRAFT -> "待建档";
            case STAGE_RECOMMENDING -> "推荐中";
            case STAGE_CONTACTED -> "已建联";
            case STAGE_MET -> "已见面";
            case STAGE_DATING -> "交往中";
            case STAGE_MEET_PARENTS -> "见家长";
            case STAGE_MARRIED -> "已结婚";
            case STAGE_CHILD_BIRTH -> "已生子";
            case STAGE_CLOSED -> "已关闭";
            default -> String.valueOf(stage);
        };
    }

    public static String sourceLabel(Integer sourceType) {
        if (sourceType == null) {
            return "-";
        }
        return switch (sourceType) {
            case 1 -> "手工建档";
            case 2 -> "智能推荐快照";
            case 3 -> "相亲活动";
            case 4 -> "微信群组局";
            default -> String.valueOf(sourceType);
        };
    }

    public static String requestStatusLabel(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case MATCH_REQUEST_STATUS_PENDING -> "待发送";
            case MATCH_REQUEST_STATUS_SENT -> "已发送";
            case MATCH_REQUEST_STATUS_ACCEPTED -> "已接受";
            case MATCH_REQUEST_STATUS_REJECTED -> "已拒绝";
            case MATCH_REQUEST_STATUS_EXPIRED -> "已过期";
            default -> String.valueOf(status);
        };
    }
}
