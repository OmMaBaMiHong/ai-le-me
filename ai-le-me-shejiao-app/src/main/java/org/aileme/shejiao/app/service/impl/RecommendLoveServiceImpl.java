package org.aileme.shejiao.app.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aileme.common.redis.utils.RedisUtils;
import org.redisson.api.RList;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.FollowService;
import org.aileme.shejiao.api.service.RecommendLoveService;
import org.aileme.shejiao.app.dao.RecommendLoveDao;
import org.aileme.shejiao.app.dao.SmartMatchSnapshotDao;
import org.aileme.shejiao.app.dao.SmartMatchSnapshotItemDao;
import org.aileme.shejiao.app.service.agent.AgentRuntimeBridgeService;
import org.aileme.shejiao.app.websocket.component.SocketServer;
import org.aileme.shejiao.app.websocket.constant.MessageConstant;
import org.aileme.shejiao.app.websocket.entity.SocketMessage;
import org.aileme.shejiao.common.utils.DateUtil;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.FollowEntity;
import org.aileme.shejiao.domain.entity.app.RecommendLoveEntity;
import org.aileme.shejiao.domain.entity.app.SmartMatchSnapshotEntity;
import org.aileme.shejiao.domain.entity.app.SmartMatchSnapshotItemEntity;
import org.aileme.shejiao.domain.vo.AppReccomentUserResponse;
import org.aileme.shejiao.domain.vo.SmartMatchCandidateVo;
import org.aileme.shejiao.domain.vo.SmartMatchRecommendVo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@DS("master")
@Service("recommendLoveService")
public class RecommendLoveServiceImpl extends ServiceImpl<RecommendLoveDao, RecommendLoveEntity> implements RecommendLoveService {

    private static final int SMART_MATCH_FREE_LIMIT = 3;
    private static final int SMART_MATCH_VIP_LIMIT = 10;
    private static final int SMART_MATCH_RANK_LIMIT = 20;
    private static final int SMART_MATCH_PREGENERATE_LIMIT = 40;
    private static final Duration SMART_MATCH_DAILY_TTL = Duration.ofDays(2);
    private static final DateTimeFormatter SMART_MATCH_DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Set<String> SMART_MATCH_RUNTIME_UPGRADING = ConcurrentHashMap.newKeySet();

    @Autowired
    private FollowService followService;

    @Autowired
    private AppUserService appUserService;

    @Autowired(required = false)
    private AgentRuntimeBridgeService agentRuntimeBridgeService;

    @Autowired
    private SmartMatchSnapshotDao smartMatchSnapshotDao;

    @Autowired
    private SmartMatchSnapshotItemDao smartMatchSnapshotItemDao;

    @Override
    public List<AppReccomentUserResponse> reccomentLoves(AppUserEntity user) {
        RecommendLoveEntity setting = loadSetting(user);
        CandidatePool pool = buildCandidatePool(user, setting, 1, 12);
        return pool.getCandidates().stream()
                .map(item -> toResponse(
                        item,
                        pool.getLikedUids().contains(item.getUid()),
                        pool.getFanUids().contains(item.getUid())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void getLove(Integer recommendUid, AppUserEntity user) {
        log.info("用户 {} 喜欢了推荐用户 {}", user.getUid(), recommendUid);

        boolean isFollow = followService.isFollowOrNot(user.getUid(), recommendUid);
        if (!isFollow) {
            FollowEntity followEntity = new FollowEntity();
            followEntity.setUid(user.getUid());
            followEntity.setFollowUid(recommendUid);
            followEntity.setStatus(0);
            followEntity.setCreateTime(DateUtil.nowDateTime());
            followService.save(followEntity);
        }

        Map<String, Object> notifyData = new HashMap<>();
        notifyData.put("fromUid", user.getUid());
        notifyData.put("fromUsername", user.getUsername());
        notifyData.put("fromAvatar", user.getAvatar());
        notifyData.put("type", "love");
        notifyData.put("message", user.getUsername() + "对你表达了好感~");

        SocketMessage<Map<String, Object>> socketMessage = new SocketMessage<>(MessageConstant.RECOMMEND_LOVE, notifyData);
        boolean sent = SocketServer.sendToUser(String.valueOf(recommendUid), socketMessage);
        log.info("发送喜欢通知给用户 {}, 结果: {}", recommendUid, sent ? "成功" : "失败(用户不在线)");
        invalidateSmartMatchSnapshots(user.getUid());
    }

    @Override
    public void lossLove(Integer recommendUid, AppUserEntity user) {
        log.info("用户 {} 不喜欢推荐用户 {}", user.getUid(), recommendUid);

        FollowEntity existFollow = followService.lambdaQuery()
                .eq(FollowEntity::getUid, user.getUid())
                .eq(FollowEntity::getFollowUid, recommendUid)
                .one();

        if (existFollow != null) {
            existFollow.setStatus(1);
            followService.updateById(existFollow);
        } else {
            FollowEntity followEntity = new FollowEntity();
            followEntity.setUid(user.getUid());
            followEntity.setFollowUid(recommendUid);
            followEntity.setStatus(1);
            followEntity.setCreateTime(DateUtil.nowDateTime());
            followService.save(followEntity);
        }
        invalidateSmartMatchSnapshots(user.getUid());
    }

    @Override
    public Result saveLove(RecommendLoveEntity entity, AppUserEntity user) {
        RecommendLoveEntity exist = lambdaQuery().eq(RecommendLoveEntity::getUid, user.getUid()).one();
        entity.setUid(user.getUid());
        entity.setRecommendUid(null);
        entity.setHasRecNums(exist == null ? 0 : defaultInteger(exist.getHasRecNums()));
        entity.setRecNum(exist == null ? 0 : defaultInteger(exist.getRecNum()));
        entity.setVip(user.getVip());
        if (exist != null) {
            entity.setId(exist.getId());
            entity.setCreateTime(exist.getCreateTime());
        } else if (entity.getCreateTime() == null) {
            entity.setCreateTime(DateUtil.nowDateTime());
        }
        saveOrUpdate(entity);
        invalidateSmartMatchSnapshots(user.getUid());
        return new Result().ok(entity);
    }

    @Override
    public void sendNots(Integer recommendUid, String nots, AppUserEntity user) {
        // 简化实现
    }

    @Override
    public List<AppReccomentUserResponse> sameCityLoves(AppUserEntity user, String city, Integer page, Integer size) {
        String targetCity = resolveCity(user, city);
        if (targetCity == null || targetCity.isEmpty()) {
            log.info("用户 {} 无法确定城市，返回空列表", user.getUid());
            return new ArrayList<>();
        }

        Set<Integer> likedUids = new HashSet<>(listFollowUids(user.getUid(), 0));
        Set<Integer> fanUids = new HashSet<>(listFansUids(user.getUid()));
        Set<Integer> excludeUids = new HashSet<>(listFollowUids(user.getUid(), 1));
        excludeUids.add(user.getUid());

        QueryWrapper<AppUserEntity> wrapper = new QueryWrapper<>();
        wrapper.notIn(!excludeUids.isEmpty(), "uid", excludeUids)
                .eq("status", 0)
                .and(w -> w.eq("location_city", targetCity)
                        .or().eq("city", targetCity)
                        .or().eq("abode_city", targetCity)
                        .or().like("location_city", targetCity)
                        .or().like("city", targetCity)
                        .or().like("abode_city", targetCity));

        if (user.getGender() != null && user.getGender() > 0) {
            wrapper.eq("gender", user.getGender() == 1 ? 2 : 1);
        }

        wrapper.orderByDesc("update_time");

        Page<AppUserEntity> pageParam = new Page<>(page, size);
        IPage<AppUserEntity> userPage = appUserService.page(pageParam, wrapper);

        return userPage.getRecords().stream()
                .sorted(Comparator
                        .comparingInt((AppUserEntity item) -> sameCityCandidateScore(item, targetCity))
                        .reversed()
                        .thenComparing(AppUserEntity::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AppUserEntity::getVip, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AppUserEntity::getUid, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> toResponse(
                        item,
                        likedUids.contains(item.getUid()),
                        fanUids.contains(item.getUid())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public SmartMatchRecommendVo smartMatchLoves(AppUserEntity user, Integer page, Integer size) {
        int dailyLimit = resolveSmartMatchDailyLimit(user);
        int visibleLimit = Math.max(1, Math.min(size == null ? dailyLimit : size, dailyLimit));
        int rankLimit = Math.max(visibleLimit * 4, Math.min(SMART_MATCH_RANK_LIMIT, dailyLimit * 4));
        RecommendLoveEntity setting = loadSetting(user);
        List<String> selectedCities = resolvePreferredCities(user, setting);
        String featureHash = buildSmartMatchFeatureHash(user, setting, selectedCities);
        PreGeneratedSmartMatch snapshotBundle = loadSmartMatchSnapshot(user.getUid(), featureHash);

        Set<Integer> likedUids = new HashSet<>(listFollowUids(user.getUid(), 0));
        Set<Integer> dislikedUids = new HashSet<>(listFollowUids(user.getUid(), 1));
        Set<Integer> actedUids = new HashSet<>(likedUids);
        actedUids.addAll(dislikedUids);
        String deliveryDay = LocalDate.now().format(SMART_MATCH_DAY_FORMATTER);

        if (snapshotBundle == null) {
            snapshotBundle = buildAndStoreSmartMatchSnapshot(user, setting, featureHash, rankLimit, false);
        }
        if (snapshotBundle != null && !Boolean.TRUE.equals(snapshotBundle.getUsedRuntime())) {
            triggerAsyncRuntimeSnapshotUpgrade(user, setting, featureHash, rankLimit, snapshotBundle);
        }

        if (snapshotBundle == null || snapshotBundle.getCandidates().isEmpty()) {
            syncSmartMatchQuotaSnapshot(user, setting, 0, dailyLimit, 0);
            return SmartMatchRecommendVo.builder()
                    .provider("local_rule")
                    .insight("暂时没筛到足够合适的人，建议先放宽城市或学历条件。")
                    .settingSummary(buildSettingSummary(setting, selectedCities, 0, dailyLimit))
                    .selectedCities(selectedCities)
                    .usedRuntime(false)
                    .data(Collections.emptyList())
                    .build();
        }

        DailySmartMatchWindow dailyWindow = applyDailySmartMatchWindow(
                user.getUid(),
                snapshotBundle.getCandidates(),
                actedUids,
                dailyLimit,
                visibleLimit,
                deliveryDay
        );
        String insight = dailyWindow.getVisible().isEmpty() && dailyWindow.getDeliveredCount() >= dailyLimit
                ? "今天的推荐名额已经看完了，明天会继续补新的对象。"
                : defaultIfBlank(snapshotBundle.getInsight(), "优先把更容易开口、基础条件更贴近的人排在前面。");
        syncSmartMatchQuotaSnapshot(user, setting, dailyWindow.getDeliveredCount(), dailyLimit, dailyWindow.getNewlyDeliveredCount());

        return SmartMatchRecommendVo.builder()
                .provider(defaultIfBlank(snapshotBundle.getProvider(), "local_rule"))
                .insight(insight)
                .settingSummary(buildSettingSummary(setting, snapshotBundle.getSelectedCities(), dailyWindow.getDeliveredCount(), dailyLimit))
                .selectedCities(snapshotBundle.getSelectedCities())
                .usedRuntime(snapshotBundle.getUsedRuntime())
                .data(dailyWindow.getVisible())
                .build();
    }

    @Override
    public void preGenerateSmartMatch(AppUserEntity user) {
        if (user == null || user.getUid() == null) {
            return;
        }
        RecommendLoveEntity setting = loadSetting(user);
        List<String> selectedCities = resolvePreferredCities(user, setting);
        String featureHash = buildSmartMatchFeatureHash(user, setting, selectedCities);
        int rankLimit = Math.max(resolveSmartMatchDailyLimit(user) * 4, SMART_MATCH_PREGENERATE_LIMIT);
        buildAndStoreSmartMatchSnapshot(user, setting, featureHash, rankLimit, true);
    }

    @Override
    public String replenishDailySmartMatches(Integer batchSize) {
        int limit = Math.max(5, Math.min(batchSize == null ? 30 : batchSize, 200));
        LinkedHashSet<Integer> userIds = collectSmartMatchReplenishUserIds(limit * 4);
        if (userIds.isEmpty()) {
            return "无待补量智能红娘用户";
        }
        List<Integer> limitedUserIds = new ArrayList<>(userIds).subList(0, Math.min(limit, userIds.size()));
        List<AppUserEntity> users = appUserService.getBatchUser(limitedUserIds);
        if (users == null || users.isEmpty()) {
            return "无待补量智能红娘用户";
        }

        int processed = 0;
        int replenished = 0;
        int skipped = 0;
        int failed = 0;
        for (AppUserEntity user : users) {
            if (user == null || user.getUid() == null) {
                continue;
            }
            processed++;
            try {
                if (tryReplenishDailySmartMatch(user)) {
                    replenished++;
                } else {
                    skipped++;
                }
            } catch (Exception ex) {
                failed++;
                log.warn("智能红娘补量失败, uid={}, err={}", user.getUid(), ex.getMessage());
            }
        }
        return String.format("智能红娘补量检查%d人，补新%d人，跳过%d人，失败%d人", processed, replenished, skipped, failed);
    }

    private PreGeneratedSmartMatch buildAndStoreSmartMatchSnapshot(AppUserEntity user,
                                                                   RecommendLoveEntity setting,
                                                                   String featureHash,
                                                                   int rankLimit,
                                                                   boolean allowRuntimeEnhancement) {
        CandidatePool pool = buildCandidatePool(user, setting, 1, Math.max(rankLimit, SMART_MATCH_PREGENERATE_LIMIT));
        if (pool.getCandidates().isEmpty()) {
            return null;
        }
        List<SmartMatchCandidateVo> localRanked = buildLocalSmartMatches(user, setting, pool, SMART_MATCH_PREGENERATE_LIMIT);
        JSONObject runtimeResponse = null;
        if (allowRuntimeEnhancement
                && agentRuntimeBridgeService != null
                && agentRuntimeBridgeService.isCompanionEnabled()) {
            runtimeResponse = agentRuntimeBridgeService.preGenerateSmartMatches(
                    user,
                    setting,
                    pool.getCandidates(),
                    pool.getLikedUids(),
                    pool.getFanUids(),
                    SMART_MATCH_PREGENERATE_LIMIT
            );
        }
        List<SmartMatchCandidateVo> runtimeRanked = buildRuntimeSmartMatches(runtimeResponse, pool, SMART_MATCH_PREGENERATE_LIMIT);
        List<SmartMatchCandidateVo> merged = mergeSmartMatches(runtimeRanked, localRanked, SMART_MATCH_PREGENERATE_LIMIT);
        boolean usedRuntime = !runtimeRanked.isEmpty();
        String insight = usedRuntime
                ? defaultIfBlank(runtimeResponse.getString("insight"), defaultIfBlank(pool.getFallbackInsight(), "优先把更容易开口、基础条件更贴近的人排在前面。"))
                : defaultIfBlank(pool.getFallbackInsight(), "先按城市、年龄、学历和资料完整度筛一轮，再把更容易推进关系的人排在前面。");
        String provider = usedRuntime
                ? defaultIfBlank(runtimeResponse.getString("provider"), "agent_runtime")
                : "local_rule";
        PreGeneratedSmartMatch snapshot = new PreGeneratedSmartMatch(
                merged,
                provider,
                insight,
                pool.getSelectedCities(),
                usedRuntime
        );
        saveSmartMatchSnapshot(user.getUid(), featureHash, snapshot);
        return snapshot;
    }

    private void triggerAsyncRuntimeSnapshotUpgrade(AppUserEntity user,
                                                    RecommendLoveEntity setting,
                                                    String featureHash,
                                                    int rankLimit,
                                                    PreGeneratedSmartMatch snapshotBundle) {
        if (user == null
                || user.getUid() == null
                || !hasText(featureHash)
                || snapshotBundle == null
                || Boolean.TRUE.equals(snapshotBundle.getUsedRuntime())
                || agentRuntimeBridgeService == null
                || !agentRuntimeBridgeService.isCompanionEnabled()) {
            return;
        }
        String upgradeKey = user.getUid() + ":" + featureHash;
        if (!SMART_MATCH_RUNTIME_UPGRADING.add(upgradeKey)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                PreGeneratedSmartMatch upgraded = buildAndStoreSmartMatchSnapshot(user, setting, featureHash, rankLimit, true);
                if (upgraded != null && Boolean.TRUE.equals(upgraded.getUsedRuntime())) {
                    log.info("智能红娘快照异步升级完成, uid={}, featureHash={}", user.getUid(), featureHash);
                }
            } catch (Exception ex) {
                log.warn("智能红娘快照异步升级失败, uid={}, featureHash={}, err={}", user.getUid(), featureHash, ex.getMessage());
            } finally {
                SMART_MATCH_RUNTIME_UPGRADING.remove(upgradeKey);
            }
        });
    }

    private PreGeneratedSmartMatch loadSmartMatchSnapshot(Integer userId, String featureHash) {
        if (userId == null || !hasText(featureHash)) {
            return null;
        }
        SmartMatchSnapshotEntity snapshot = smartMatchSnapshotDao.selectOne(new QueryWrapper<SmartMatchSnapshotEntity>()
                .lambda()
                .eq(SmartMatchSnapshotEntity::getUserId, userId)
                .eq(SmartMatchSnapshotEntity::getFeatureHash, featureHash)
                .eq(SmartMatchSnapshotEntity::getStatus, 1)
                .orderByDesc(SmartMatchSnapshotEntity::getUpdateTime)
                .last("LIMIT 1"));
        if (snapshot == null) {
            return null;
        }
        List<SmartMatchSnapshotItemEntity> items = smartMatchSnapshotItemDao.selectList(new QueryWrapper<SmartMatchSnapshotItemEntity>()
                .lambda()
                .eq(SmartMatchSnapshotItemEntity::getSnapshotId, snapshot.getId())
                .orderByAsc(SmartMatchSnapshotItemEntity::getRankNo));
        if (items == null || items.isEmpty()) {
            return null;
        }
        List<SmartMatchCandidateVo> candidates = items.stream()
                .map(SmartMatchSnapshotItemEntity::getCandidateJson)
                .filter(this::hasText)
                .map(item -> JSON.parseObject(item, SmartMatchCandidateVo.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return null;
        }
        return new PreGeneratedSmartMatch(
                candidates,
                defaultIfBlank(snapshot.getProvider(), "local_rule"),
                defaultIfBlank(snapshot.getInsight(), ""),
                parseJsonStringList(snapshot.getSelectedCitiesJson()),
                Objects.equals(snapshot.getUsedRuntime(), 1)
        );
    }

    private void saveSmartMatchSnapshot(Integer userId, String featureHash, PreGeneratedSmartMatch snapshot) {
        if (userId == null || !hasText(featureHash) || snapshot == null || snapshot.getCandidates().isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<SmartMatchSnapshotEntity> existingSnapshots = smartMatchSnapshotDao.selectList(new QueryWrapper<SmartMatchSnapshotEntity>()
                .lambda()
                .eq(SmartMatchSnapshotEntity::getUserId, userId)
                .orderByDesc(SmartMatchSnapshotEntity::getUpdateTime)
                .orderByDesc(SmartMatchSnapshotEntity::getId));
        SmartMatchSnapshotEntity entity = existingSnapshots == null || existingSnapshots.isEmpty()
                ? new SmartMatchSnapshotEntity()
                : existingSnapshots.get(0);
        entity.setUserId(userId);
        entity.setFeatureHash(featureHash);
        entity.setProvider(defaultIfBlank(snapshot.getProvider(), "local_rule"));
        entity.setInsight(defaultIfBlank(snapshot.getInsight(), ""));
        entity.setSelectedCitiesJson(JSON.toJSONString(snapshot.getSelectedCities()));
        entity.setUsedRuntime(Boolean.TRUE.equals(snapshot.getUsedRuntime()) ? 1 : 0);
        entity.setCandidateCount(snapshot.getCandidates().size());
        entity.setVersionNo(1);
        entity.setStatus(1);
        entity.setGeneratedAt(now);
        entity.setExpireAt(null);
        entity.setUpdateTime(now);
        if (entity.getId() == null) {
            entity.setCreateTime(now);
            smartMatchSnapshotDao.insert(entity);
        } else {
            smartMatchSnapshotDao.updateById(entity);
        }

        smartMatchSnapshotItemDao.delete(new QueryWrapper<SmartMatchSnapshotItemEntity>()
                .lambda()
                .eq(SmartMatchSnapshotItemEntity::getSnapshotId, entity.getId()));

        int rankNo = 1;
        for (SmartMatchCandidateVo candidate : snapshot.getCandidates()) {
            if (candidate == null || candidate.getUid() == null) {
                continue;
            }
            SmartMatchSnapshotItemEntity item = new SmartMatchSnapshotItemEntity();
            item.setSnapshotId(entity.getId());
            item.setOwnerUserId(userId);
            item.setTargetUserId(candidate.getUid());
            item.setRankNo(rankNo++);
            item.setMatchScore(candidate.getMatchScore());
            item.setProvider(defaultIfBlank(candidate.getProvider(), entity.getProvider()));
            item.setCandidateJson(JSON.toJSONString(candidate));
            item.setCreateTime(now);
            item.setUpdateTime(now);
            smartMatchSnapshotItemDao.insert(item);
        }

        if (existingSnapshots != null && existingSnapshots.size() > 1) {
            List<Long> removeSnapshotIds = existingSnapshots.stream()
                    .map(SmartMatchSnapshotEntity::getId)
                    .filter(Objects::nonNull)
                    .filter(id -> !Objects.equals(id, entity.getId()))
                    .collect(Collectors.toList());
            if (!removeSnapshotIds.isEmpty()) {
                smartMatchSnapshotItemDao.delete(new QueryWrapper<SmartMatchSnapshotItemEntity>()
                        .lambda()
                        .in(SmartMatchSnapshotItemEntity::getSnapshotId, removeSnapshotIds));
                removeSnapshotIds.forEach(smartMatchSnapshotDao::deleteById);
            }
        }
    }

    private LinkedHashSet<Integer> collectSmartMatchReplenishUserIds(int limit) {
        int fetchLimit = Math.max(20, Math.min(limit, 500));
        LinkedHashSet<Integer> userIds = new LinkedHashSet<>();
        List<SmartMatchSnapshotEntity> snapshots = smartMatchSnapshotDao.selectList(new QueryWrapper<SmartMatchSnapshotEntity>()
                .lambda()
                .eq(SmartMatchSnapshotEntity::getStatus, 1)
                .orderByDesc(SmartMatchSnapshotEntity::getUpdateTime)
                .last("LIMIT " + fetchLimit));
        if (snapshots != null) {
            snapshots.stream()
                    .map(SmartMatchSnapshotEntity::getUserId)
                    .filter(Objects::nonNull)
                    .forEach(userIds::add);
        }
        List<RecommendLoveEntity> settings = lambdaQuery()
                .isNotNull(RecommendLoveEntity::getUid)
                .orderByDesc(RecommendLoveEntity::getCreateTime)
                .last("LIMIT " + fetchLimit)
                .list();
        if (settings != null) {
            settings.stream()
                    .map(RecommendLoveEntity::getUid)
                    .filter(Objects::nonNull)
                    .forEach(userIds::add);
        }
        return userIds;
    }

    private boolean tryReplenishDailySmartMatch(AppUserEntity user) {
        if (user == null || user.getUid() == null || !Objects.equals(user.getStatus(), 0)) {
            return false;
        }
        RecommendLoveEntity setting = loadSetting(user);
        int dailyLimit = resolveSmartMatchDailyLimit(user);
        String deliveryDay = LocalDate.now().format(SMART_MATCH_DAY_FORMATTER);
        List<Integer> deliveredToday = readDeliveredSmartMatchUids(user.getUid(), deliveryDay);
        int deliveredCount = Math.min(deliveredToday.size(), dailyLimit);
        syncSmartMatchQuotaSnapshot(user, setting, deliveredCount, dailyLimit, 0);
        int remainingQuota = Math.max(dailyLimit - deliveredCount, 0);
        if (remainingQuota <= 0) {
            return false;
        }

        List<String> selectedCities = resolvePreferredCities(user, setting);
        String featureHash = buildSmartMatchFeatureHash(user, setting, selectedCities);
        PreGeneratedSmartMatch snapshotBundle = loadSmartMatchSnapshot(user.getUid(), featureHash);
        Set<Integer> actedUids = new HashSet<>(listFollowUids(user.getUid(), 0));
        actedUids.addAll(listFollowUids(user.getUid(), 1));
        int availableCandidates = countAvailableSmartMatchCandidates(snapshotBundle, actedUids, deliveredToday);
        boolean runtimeUpgrade = snapshotBundle != null
                && !Boolean.TRUE.equals(snapshotBundle.getUsedRuntime())
                && agentRuntimeBridgeService != null
                && agentRuntimeBridgeService.isCompanionEnabled();
        if (snapshotBundle != null && !runtimeUpgrade && availableCandidates >= remainingQuota) {
            return false;
        }

        int rankLimit = Math.max(dailyLimit * 4, SMART_MATCH_PREGENERATE_LIMIT);
        PreGeneratedSmartMatch refreshed = buildAndStoreSmartMatchSnapshot(user, setting, featureHash, rankLimit, true);
        int refreshedAvailable = countAvailableSmartMatchCandidates(refreshed, actedUids, deliveredToday);
        syncSmartMatchQuotaSnapshot(user, setting, deliveredCount, dailyLimit, 0);
        return refreshed != null
                && refreshedAvailable > 0
                && (runtimeUpgrade || refreshedAvailable >= Math.min(remainingQuota, 3) || refreshedAvailable > availableCandidates);
    }

    private int countAvailableSmartMatchCandidates(PreGeneratedSmartMatch snapshotBundle,
                                                   Set<Integer> actedUids,
                                                   List<Integer> deliveredToday) {
        if (snapshotBundle == null || snapshotBundle.getCandidates() == null || snapshotBundle.getCandidates().isEmpty()) {
            return 0;
        }
        Set<Integer> deliveredSet = deliveredToday == null
                ? Collections.emptySet()
                : deliveredToday.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        return (int) snapshotBundle.getCandidates().stream()
                .filter(Objects::nonNull)
                .map(SmartMatchCandidateVo::getUid)
                .filter(Objects::nonNull)
                .filter(uid -> !deliveredSet.contains(uid))
                .filter(uid -> actedUids == null || !actedUids.contains(uid))
                .distinct()
                .count();
    }

    private void invalidateSmartMatchSnapshots(Integer userId) {
        if (userId == null) {
            return;
        }
        List<SmartMatchSnapshotEntity> snapshots = smartMatchSnapshotDao.selectList(new QueryWrapper<SmartMatchSnapshotEntity>()
                .lambda()
                .eq(SmartMatchSnapshotEntity::getUserId, userId));
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        List<Long> snapshotIds = snapshots.stream()
                .map(SmartMatchSnapshotEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!snapshotIds.isEmpty()) {
            smartMatchSnapshotItemDao.delete(new QueryWrapper<SmartMatchSnapshotItemEntity>()
                    .lambda()
                    .in(SmartMatchSnapshotItemEntity::getSnapshotId, snapshotIds));
            snapshotIds.forEach(smartMatchSnapshotDao::deleteById);
        }
    }

    private String buildSmartMatchFeatureHash(AppUserEntity user,
                                              RecommendLoveEntity setting,
                                              List<String> selectedCities) {
        String raw = String.join("|",
                String.valueOf(user == null ? null : user.getUid()),
                String.valueOf(user == null ? null : user.getGender()),
                defaultIfBlank(resolveCity(user, null), ""),
                buildSmartMatchProfileSignature(user),
                defaultIfBlank(setting == null ? null : setting.getAge(), ""),
                defaultIfBlank(setting == null ? null : setting.getHeight(), ""),
                defaultIfBlank(setting == null ? null : setting.getEdu(), ""),
                defaultIfBlank(setting == null ? null : setting.getCityids(), ""),
                String.valueOf(setting != null && Objects.equals(setting.getRecFollowus(), 1)),
                String.valueOf(setting != null && Objects.equals(setting.getRecBefollowus(), 1)),
                String.valueOf(setting != null && Objects.equals(setting.getRecOnlyBefollowus(), 1)),
                String.join(",", selectedCities == null ? Collections.emptyList() : selectedCities)
        );
        return sha256Hex(raw);
    }

    private String buildSmartMatchProfileSignature(AppUserEntity user) {
        if (user == null) {
            return "";
        }
        String raw = String.join("|",
                defaultIfBlank(user.getAvatar(), ""),
                defaultIfBlank(user.getFigur(), ""),
                String.valueOf(user.getAge()),
                defaultIfBlank(user.getBirthday(), ""),
                defaultIfBlank(resolveCity(user, null), ""),
                defaultIfBlank(user.getHomeCity(), ""),
                defaultIfBlank(user.getAbodeCity(), ""),
                defaultIfBlank(user.getJob(), ""),
                String.valueOf(user.getEducation()),
                defaultIfBlank(user.getSchool(), ""),
                String.valueOf(user.getIncome()),
                defaultIfBlank(user.getHeight(), ""),
                defaultIfBlank(user.getSelfIntroduction(), ""),
                defaultIfBlank(user.getLoveDeclaration(), ""),
                defaultIfBlank(user.getInterest(), ""),
                defaultIfBlank(user.getIntro(), ""),
                defaultIfBlank(user.getTagStr(), "")
        );
        return sha256Hex(raw);
    }

    private CandidatePool buildCandidatePool(AppUserEntity user, RecommendLoveEntity setting, Integer page, Integer size) {
        List<String> selectedCities = resolvePreferredCities(user, setting);
        Set<Integer> likedUids = new HashSet<>(listFollowUids(user.getUid(), 0));
        Set<Integer> fanUids = new HashSet<>(listFansUids(user.getUid()));
        Set<Integer> dislikedUids = new HashSet<>(listFollowUids(user.getUid(), 1));
        dislikedUids.add(user.getUid());
        int safeSize = Math.max(size == null ? 6 : size, 6);
        LinkedHashMap<Integer, AppUserEntity> mergedPool = new LinkedHashMap<>();
        String fallbackInsight = "";

        List<AppUserEntity> strictPool = queryCandidatePool(
                user,
                setting,
                likedUids,
                fanUids,
                dislikedUids,
                selectedCities,
                safeSize,
                true,
                true,
                true,
                true,
                true
        );
        appendDistinctCandidates(mergedPool, strictPool, safeSize);
        if (mergedPool.size() >= safeSize) {
            return new CandidatePool(new ArrayList<>(mergedPool.values()), likedUids, fanUids, selectedCities, "");
        }

        List<AppUserEntity> relaxedTraitPool = queryCandidatePool(
                user,
                setting,
                likedUids,
                fanUids,
                dislikedUids,
                selectedCities,
                safeSize,
                true,
                true,
                false,
                false,
                true
        );
        if (!relaxedTraitPool.isEmpty()) {
            appendDistinctCandidates(mergedPool, relaxedTraitPool, safeSize);
            fallbackInsight = "当前偏好下人池偏少，已先放宽学历和身高，继续给你保留城市与年龄优先。";
            if (mergedPool.size() >= safeSize) {
                return new CandidatePool(new ArrayList<>(mergedPool.values()), likedUids, fanUids, selectedCities, fallbackInsight);
            }
        }

        List<AppUserEntity> relaxedCityPool = queryCandidatePool(
                user,
                setting,
                likedUids,
                fanUids,
                dislikedUids,
                selectedCities,
                safeSize,
                false,
                true,
                false,
                false,
                true
        );
        if (!relaxedCityPool.isEmpty()) {
            appendDistinctCandidates(mergedPool, relaxedCityPool, safeSize);
            if (fallbackInsight.isBlank()) {
                fallbackInsight = "当前同城可推人较少，已扩展到更大范围，优先补充资料更完整、最近更活跃的人。";
            }
            if (mergedPool.size() >= safeSize) {
                return new CandidatePool(new ArrayList<>(mergedPool.values()), likedUids, fanUids, selectedCities, fallbackInsight);
            }
        }

        List<AppUserEntity> discoverPool = queryCandidatePool(
                user,
                setting,
                likedUids,
                fanUids,
                dislikedUids,
                selectedCities,
                safeSize,
                false,
                false,
                false,
                false,
                false
        );
        if (!discoverPool.isEmpty()) {
            appendDistinctCandidates(mergedPool, discoverPool, safeSize);
            if (fallbackInsight.isBlank()) {
                fallbackInsight = "你当前的偏好限制较多，已切到兜底推荐，先给你补一批基础条件不错、适合先看主页的人。";
            }
        }
        return new CandidatePool(
                new ArrayList<>(mergedPool.values()),
                likedUids,
                fanUids,
                selectedCities,
                fallbackInsight
        );
    }

    private List<AppUserEntity> queryCandidatePool(AppUserEntity user,
                                                   RecommendLoveEntity setting,
                                                   Set<Integer> likedUids,
                                                   Set<Integer> fanUids,
                                                   Set<Integer> dislikedUids,
                                                   List<String> selectedCities,
                                                   int size,
                                                   boolean applyCityFilter,
                                                   boolean applyAgeFilter,
                                                   boolean applyEducationFilter,
                                                   boolean applyHeightFilter,
                                                   boolean respectOnlyLoved) {
        QueryWrapper<AppUserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0)
                .notIn(!dislikedUids.isEmpty(), "uid", dislikedUids);

        if (user.getGender() != null && user.getGender() > 0) {
            wrapper.eq("gender", user.getGender() == 1 ? 2 : 1);
        }
        if (respectOnlyLoved && Objects.equals(setting.getRecOnlyBefollowus(), 1)) {
            if (likedUids.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in("uid", likedUids);
        }
        if (applyCityFilter) {
            applyCityFilter(wrapper, selectedCities);
        }
        if (applyAgeFilter) {
            List<Integer> ageRange = parseIntegerList(setting.getAge());
            if (ageRange.size() >= 2) {
                int minAge = Math.min(ageRange.get(0), ageRange.get(1));
                int maxAge = Math.max(ageRange.get(0), ageRange.get(1));
                wrapper.between("age", minAge, maxAge);
            }
        }

        wrapper.orderByDesc("update_time");

        long fetchSize = Math.max(24L, (long) size * 8L);
        Page<AppUserEntity> pageParam = new Page<>(1, fetchSize);
        IPage<AppUserEntity> userPage = appUserService.page(pageParam, wrapper);

        return userPage.getRecords().stream()
                .filter(candidate -> !applyEducationFilter || matchesEducation(candidate, setting))
                .filter(candidate -> !applyHeightFilter || matchesHeight(candidate, setting))
                .sorted(Comparator
                        .comparingDouble((AppUserEntity candidate) -> preliminaryScore(user, candidate, setting, likedUids, fanUids, selectedCities))
                        .reversed()
                        .thenComparing(this::profileCompleteness, Comparator.reverseOrder())
                        .thenComparing(AppUserEntity::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(Math.max(18, size * 4L))
                .collect(Collectors.toList());
    }

    private List<SmartMatchCandidateVo> buildLocalSmartMatches(AppUserEntity user,
                                                               RecommendLoveEntity setting,
                                                               CandidatePool pool,
                                                               int limit) {
        return pool.getCandidates().stream()
                .map(candidate -> buildLocalSmartCandidate(user, candidate, setting, pool))
                .sorted(Comparator.comparingInt(SmartMatchCandidateVo::getMatchScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private SmartMatchCandidateVo buildLocalSmartCandidate(AppUserEntity user,
                                                           AppUserEntity candidate,
                                                           RecommendLoveEntity setting,
                                                           CandidatePool pool) {
        List<String> reasons = new ArrayList<>();
        List<String> fitTags = new ArrayList<>();
        double score = 36;

        if (matchesCity(candidate, pool.getSelectedCities()) || isExactCityMatch(resolveCity(user, null), candidate.getLocationCity())
                || isExactCityMatch(resolveCity(user, null), candidate.getCity())
                || isExactCityMatch(resolveCity(user, null), candidate.getAbodeCity())) {
            score += 13;
            reasons.add("城市偏好更贴近，后续见面和推进关系更顺。");
            String city = firstNonBlank(candidate.getLocationCity(), candidate.getCity(), candidate.getAbodeCity());
            if (!city.isBlank()) {
                fitTags.add(normalizeCityName(city));
            }
        }

        List<Integer> ageRange = parseIntegerList(setting.getAge());
        if (ageRange.size() >= 2 && candidate.getAge() != null) {
            int min = Math.min(ageRange.get(0), ageRange.get(1));
            int max = Math.max(ageRange.get(0), ageRange.get(1));
            if (candidate.getAge() >= min && candidate.getAge() <= max) {
                score += 11;
                reasons.add("年龄落在你的偏好区间里，基础接受度更高。");
            }
        }

        List<Integer> heightRange = parseIntegerList(setting.getHeight());
        Integer candidateHeight = parseHeight(candidate.getHeight());
        if (heightRange.size() >= 2 && candidateHeight != null) {
            int min = Math.min(heightRange.get(0), heightRange.get(1));
            int max = Math.max(heightRange.get(0), heightRange.get(1));
            if (candidateHeight >= min && candidateHeight <= max) {
                score += 7;
                fitTags.add(candidateHeight + "cm");
            }
        }

        List<Integer> educationLevels = parseIntegerList(setting.getEdu());
        if (!educationLevels.isEmpty() && candidate.getEducation() != null && educationLevels.contains(candidate.getEducation())) {
            score += 10;
            reasons.add("学历偏好匹配，聊天节奏和长期预期更容易对齐。");
            fitTags.add(getEducationText(candidate.getEducation()));
        }

        Set<String> commonInterest = intersectKeywords(user.getInterest(), candidate.getInterest(), 2);
        if (!commonInterest.isEmpty()) {
            score += 12;
            reasons.add("你们在兴趣上有交集，开场更容易不尬聊。");
            fitTags.addAll(commonInterest);
        }

        if (isVerifiedCandidate(candidate)) {
            score += 7;
            reasons.add("资料可信度更高，适合主理人优先推荐。");
        }
        if (pool.getFanUids().contains(candidate.getUid())) {
            score += 8;
            reasons.add("对方已经对你有过关注信号，推进成功率会更高。");
            fitTags.add("对你有关注");
        }
        if (pool.getLikedUids().contains(candidate.getUid())) {
            score += 6;
            reasons.add("你之前已经表达过好感，继续认识更自然。");
            fitTags.add("你已心动");
        }

        long activeHours = hoursSince(candidate.getUpdateTime());
        if (activeHours <= 6) {
            score += 8;
            fitTags.add("最近活跃");
        } else if (activeHours <= 24) {
            score += 6;
        } else if (activeHours <= 72) {
            score += 3;
        }

        score += Math.min(8, profileCompleteness(candidate) / 12.0);
        if (candidate.getVip() != null && candidate.getVip() == 1) {
            score += 2;
        }

        int finalScore = Math.max(38, Math.min(97, (int) Math.round(score)));
        List<String> finalReasons = reasons.stream().distinct().limit(3).collect(Collectors.toList());
        if (finalReasons.isEmpty()) {
            finalReasons = Collections.singletonList("资料完整度和基础条件不错，可以先看看主页再决定要不要聊。");
        }
        List<String> finalTags = fitTags.stream().filter(item -> item != null && !item.isBlank()).distinct().limit(4).collect(Collectors.toList());

        return toSmartCandidateVo(
                candidate,
                finalScore,
                buildLocalSummary(candidate, finalScore),
                finalTags,
                finalReasons,
                Collections.emptyList(),
                "local_rule",
                null,
                pool.getLikedUids().contains(candidate.getUid()),
                pool.getFanUids().contains(candidate.getUid())
        );
    }

    private List<SmartMatchCandidateVo> buildRuntimeSmartMatches(JSONObject runtimeResponse, CandidatePool pool, int limit) {
        if (runtimeResponse == null) {
            return Collections.emptyList();
        }
        JSONArray matchArray = runtimeResponse.getJSONArray("matches");
        if (matchArray == null || matchArray.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, AppUserEntity> candidateMap = pool.getCandidates().stream()
                .filter(candidate -> candidate.getUid() != null)
                .collect(Collectors.toMap(AppUserEntity::getUid, candidate -> candidate, (left, right) -> left));
        String provider = defaultIfBlank(runtimeResponse.getString("provider"), "agent_runtime");
        List<SmartMatchCandidateVo> result = new ArrayList<>();
        for (int i = 0; i < matchArray.size() && result.size() < limit; i++) {
            JSONObject item = matchArray.getJSONObject(i);
            if (item == null) {
                continue;
            }
            Integer uid = item.getInteger("user_id");
            AppUserEntity candidate = candidateMap.get(uid);
            if (candidate == null) {
                continue;
            }
            SmartMatchCandidateVo candidateVo = toSmartCandidateVo(
                    candidate,
                    item.getIntValue("score"),
                    defaultIfBlank(item.getString("summary"), buildLocalSummary(candidate, item.getIntValue("score"))),
                    jsonArrayToStringList(item.getJSONArray("fit_tags")),
                    jsonArrayToStringList(item.getJSONArray("reasons")),
                    jsonArrayToStringList(item.getJSONArray("icebreak_openers")),
                    provider,
                    item.getString("debug_trace_id"),
                    pool.getLikedUids().contains(uid),
                    pool.getFanUids().contains(uid)
            );
            applyAutonomyHints(candidateVo, item);
            result.add(candidateVo);
        }
        return result;
    }

    private List<SmartMatchCandidateVo> mergeSmartMatches(List<SmartMatchCandidateVo> runtimeRanked,
                                                          List<SmartMatchCandidateVo> localRanked,
                                                          int limit) {
        Map<Integer, SmartMatchCandidateVo> merged = new HashMap<>();
        List<SmartMatchCandidateVo> result = new ArrayList<>();
        for (SmartMatchCandidateVo item : runtimeRanked) {
            if (item == null || item.getUid() == null || merged.containsKey(item.getUid())) {
                continue;
            }
            merged.put(item.getUid(), item);
            result.add(item);
            if (result.size() >= limit) {
                return result;
            }
        }
        for (SmartMatchCandidateVo item : localRanked) {
            if (item == null || item.getUid() == null || merged.containsKey(item.getUid())) {
                continue;
            }
            merged.put(item.getUid(), item);
            result.add(item);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private SmartMatchCandidateVo toSmartCandidateVo(AppUserEntity candidate,
                                                     Integer score,
                                                     String summary,
                                                     List<String> fitTags,
                                                     List<String> reasons,
                                                     List<String> icebreakOpeners,
                                                     String provider,
                                                     String debugTraceId,
                                                     boolean likedByOwner,
                                                     boolean likesOwner) {
        SmartMatchCandidateVo result = SmartMatchCandidateVo.builder()
                .uid(candidate.getUid())
                .username(defaultIfBlank(candidate.getUsername(), "用户" + candidate.getUid()))
                .avatar(defaultIfBlank(candidate.getAvatar(), candidate.getFigur()))
                .figur(candidate.getFigur())
                .gender(candidate.getGender())
                .city(defaultIfBlank(candidate.getLocationCity(), defaultIfBlank(candidate.getCity(), candidate.getAbodeCity())))
                .locationCity(candidate.getLocationCity())
                .abodeCity(candidate.getAbodeCity())
                .age(candidate.getAge())
                .height(parseHeight(candidate.getHeight()))
                .education(candidate.getEducation())
                .educationText(getEducationText(candidate.getEducation()))
                .job(candidate.getJob())
                .intro(candidate.getIntro())
                .selfIntroduction(candidate.getSelfIntroduction())
                .loveDeclaration(candidate.getLoveDeclaration())
                .interest(candidate.getInterest())
                .vip(candidate.getVip())
                .auditStatus(candidate.getAuditStatus())
                .identyCertifStatus(candidate.getIdentyCertifStatus())
                .eduCertifStatus(candidate.getEduCertifStatus())
                .matchScore(score == null ? 0 : score)
                .matchSummary(defaultIfBlank(summary, "适合先看看主页，再决定要不要进一步认识。"))
                .fitTags(fitTags == null ? Collections.emptyList() : fitTags)
                .reasons(reasons == null ? Collections.emptyList() : reasons)
                .icebreakOpeners(resolveIcebreakOpeners(candidate, icebreakOpeners, reasons))
                .provider(defaultIfBlank(provider, "local_rule"))
                .debugTraceId(defaultIfBlank(debugTraceId, null))
                .likedByOwner(likedByOwner)
                .likesOwner(likesOwner)
                .build();
        applyAutonomyHints(result, null);
        return result;
    }

    private void applyAutonomyHints(SmartMatchCandidateVo candidate, JSONObject runtimeItem) {
        if (candidate == null) {
            return;
        }
        Double runtimeReadiness = runtimeItem == null ? null : runtimeItem.getDouble("opening_readiness_score");
        double fallbackReadiness = Math.max(0.35D, Math.min(0.95D, (candidate.getMatchScore() == null ? 0 : candidate.getMatchScore()) / 100D));
        double readiness = runtimeReadiness == null ? fallbackReadiness : runtimeReadiness;
        String doNotOpenReason = runtimeItem == null ? null : defaultIfBlank(runtimeItem.getString("do_not_open_reason"), null);
        Boolean runtimeEligible = runtimeItem == null ? null : runtimeItem.getBoolean("auto_chat_eligible");
        boolean autoChatEligible = runtimeEligible != null
                ? runtimeEligible
                : readiness >= 0.68D
                && candidate.getIcebreakOpeners() != null
                && !candidate.getIcebreakOpeners().isEmpty()
                && !hasText(doNotOpenReason);

        if (!autoChatEligible && !hasText(doNotOpenReason)) {
            if (readiness < 0.68D) {
                doNotOpenReason = "当前更适合先看主页和动态，暂不建议直接自动起聊。";
            } else if (candidate.getIcebreakOpeners() == null || candidate.getIcebreakOpeners().isEmpty()) {
                doNotOpenReason = "缺少足够自然的破冰切口，建议先补充更多画像信号。";
            }
        }

        candidate.setOpeningReadinessScore(readiness);
        candidate.setOpeningStyleHint(defaultIfBlank(
                runtimeItem == null ? null : runtimeItem.getString("opening_style_hint"),
                firstListItem(candidate.getFitTags(), autoChatEligible ? "轻松自然开场" : "先看主页找共同话题")
        ));
        candidate.setRecommendedAction(defaultIfBlank(
                runtimeItem == null ? null : runtimeItem.getString("recommended_action"),
                autoChatEligible ? "open_chat" : "view_profile"
        ));
        candidate.setAutoChatEligible(autoChatEligible);
        candidate.setDoNotOpenReason(doNotOpenReason);
        candidate.setMasterStyleCode(defaultIfBlank(
                runtimeItem == null ? null : runtimeItem.getString("master_style_code"),
                autoChatEligible ? "steady_partner" : "gentle_observer"
        ));
    }

    private List<String> resolveIcebreakOpeners(AppUserEntity candidate,
                                                List<String> icebreakOpeners,
                                                List<String> reasons) {
        List<String> normalized = icebreakOpeners == null
                ? Collections.emptyList()
                : icebreakOpeners.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .limit(3)
                .collect(Collectors.toList());
        if (!normalized.isEmpty()) {
            return normalized;
        }
        String topic = firstNonBlank(candidate.getInterest(), candidate.getLoveDeclaration(), candidate.getIntro());
        String shortTopic = !hasText(topic) ? "最近的生活" : topic.length() > 18 ? topic.substring(0, 18) : topic;
        String first = "看到你资料里提到" + shortTopic + "，这个点一下让我记住你了，最近你还会花时间在这件事上吗？";
        String second = "刚看到你，感觉你是那种适合慢慢聊开的人，所以想认真和你打个招呼。";
        if (reasons != null && !reasons.isEmpty()) {
            String reason = reasons.get(0);
            if (hasText(reason)) {
                second = "看到你资料挺完整的，而且" + reason.replace("。", "") + "，就想来认识一下。";
            }
        }
        return Arrays.asList(first, second);
    }

    private int sameCityCandidateScore(AppUserEntity entity, String targetCity) {
        if (entity == null) {
            return 0;
        }
        int score = 0;

        if (isExactCityMatch(targetCity, entity.getLocationCity())) {
            score += 18;
        } else if (isExactCityMatch(targetCity, entity.getCity()) || isExactCityMatch(targetCity, entity.getAbodeCity())) {
            score += 12;
        }

        if (entity.getAuditStatus() != null && entity.getAuditStatus() == 1) {
            score += 10;
        }
        if (entity.getIdentyCertifStatus() != null && entity.getIdentyCertifStatus() == 1) {
            score += 8;
        }
        if (entity.getEduCertifStatus() != null && entity.getEduCertifStatus() == 1) {
            score += 6;
        }
        if (hasText(entity.getAvatar()) || hasText(entity.getFigur())) {
            score += 10;
        }
        if (hasText(entity.getSelfIntroduction()) || hasText(entity.getIntro())) {
            score += 9;
        }
        if (hasText(entity.getInterest())) {
            score += 9;
        }
        if (hasText(entity.getLoveDeclaration()) || hasText(entity.getAdminreHerart())) {
            score += 7;
        }
        if (hasText(entity.getJob())) {
            score += 6;
        }
        if (entity.getEducation() != null && entity.getEducation() > 0) {
            score += 4;
        }
        if (entity.getAge() != null && entity.getAge() > 0) {
            score += 2;
        }
        if (entity.getVip() != null && entity.getVip() == 1) {
            score += 3;
        }

        long hours = hoursSince(entity.getUpdateTime());
        if (hours <= 6) {
            score += 16;
        } else if (hours <= 24) {
            score += 12;
        } else if (hours <= 72) {
            score += 8;
        } else if (hours <= 168) {
            score += 4;
        }

        return score;
    }

    private RecommendLoveEntity loadSetting(AppUserEntity user) {
        RecommendLoveEntity setting = lambdaQuery().eq(RecommendLoveEntity::getUid, user.getUid()).one();
        if (setting != null) {
            return setting;
        }
        RecommendLoveEntity fallback = new RecommendLoveEntity();
        fallback.setUid(user.getUid());
        fallback.setAge("22,35");
        fallback.setHeight("150,190");
        fallback.setEdu("");
        fallback.setCityids(resolveCity(user, null));
        fallback.setRecBefollowus(0);
        fallback.setRecFollowus(0);
        fallback.setRecOnlyBefollowus(0);
        return fallback;
    }

    private void applyCityFilter(QueryWrapper<AppUserEntity> wrapper, List<String> selectedCities) {
        if (selectedCities == null || selectedCities.isEmpty()) {
            return;
        }
        wrapper.and(group -> {
            boolean first = true;
            for (String city : selectedCities) {
                if (!isMeaningfulCity(city)) {
                    continue;
                }
                if (first) {
                    group.and(inner -> inner.eq("location_city", city)
                            .or().eq("city", city)
                            .or().eq("abode_city", city)
                            .or().like("location_city", city)
                            .or().like("city", city)
                            .or().like("abode_city", city));
                    first = false;
                } else {
                    group.or(inner -> inner.eq("location_city", city)
                            .or().eq("city", city)
                            .or().eq("abode_city", city)
                            .or().like("location_city", city)
                            .or().like("city", city)
                            .or().like("abode_city", city));
                }
            }
        });
    }

    private List<String> resolvePreferredCities(AppUserEntity user, RecommendLoveEntity setting) {
        List<String> result = parseCsv(defaultIfBlank(setting.getCityids(), ""));
        if (result.isEmpty()) {
            String fallbackCity = resolveCity(user, null);
            if (isMeaningfulCity(fallbackCity)) {
                result.add(fallbackCity);
            }
        }
        return result.stream()
                .map(this::normalizeCityName)
                .filter(this::isMeaningfulCity)
                .distinct()
                .limit(2)
                .collect(Collectors.toList());
    }

    private boolean matchesEducation(AppUserEntity candidate, RecommendLoveEntity setting) {
        List<Integer> educationLevels = parseIntegerList(setting.getEdu());
        return educationLevels.isEmpty()
                || candidate.getEducation() == null
                || educationLevels.contains(candidate.getEducation());
    }

    private boolean matchesHeight(AppUserEntity candidate, RecommendLoveEntity setting) {
        List<Integer> heightRange = parseIntegerList(setting.getHeight());
        Integer height = parseHeight(candidate.getHeight());
        if (heightRange.size() < 2 || height == null) {
            return true;
        }
        int min = Math.min(heightRange.get(0), heightRange.get(1));
        int max = Math.max(heightRange.get(0), heightRange.get(1));
        return height >= min - 3 && height <= max + 3;
    }

    private double preliminaryScore(AppUserEntity owner,
                                    AppUserEntity candidate,
                                    RecommendLoveEntity setting,
                                    Set<Integer> likedUids,
                                    Set<Integer> fanUids,
                                    List<String> selectedCities) {
        double score = 24;
        if (matchesCity(candidate, selectedCities)) {
            score += 12;
        }
        if (likedUids.contains(candidate.getUid()) && Objects.equals(setting.getRecBefollowus(), 1)) {
            score += 10;
        }
        if (fanUids.contains(candidate.getUid()) && Objects.equals(setting.getRecFollowus(), 1)) {
            score += 9;
        }
        if (isVerifiedCandidate(candidate)) {
            score += 8;
        }
        score += Math.min(10, profileCompleteness(candidate) / 10.0);
        long activeHours = hoursSince(candidate.getUpdateTime());
        if (activeHours <= 6) {
            score += 9;
        } else if (activeHours <= 24) {
            score += 6;
        }
        Set<String> commonInterest = intersectKeywords(owner.getInterest(), candidate.getInterest(), 2);
        score += commonInterest.size() * 4;
        return score;
    }

    private int resolveSmartMatchDailyLimit(AppUserEntity user) {
        return Objects.equals(user.getVip(), 1) ? SMART_MATCH_VIP_LIMIT : SMART_MATCH_FREE_LIMIT;
    }

    private DailySmartMatchWindow applyDailySmartMatchWindow(Integer uid,
                                                             List<SmartMatchCandidateVo> ranked,
                                                             Set<Integer> actedUids,
                                                             int dailyLimit,
                                                             int visibleLimit,
                                                             String deliveryDay) {
        List<Integer> deliveredToday = readDeliveredSmartMatchUids(uid, deliveryDay);
        Map<Integer, SmartMatchCandidateVo> rankedMap = ranked.stream()
                .filter(item -> item != null && item.getUid() != null)
                .collect(Collectors.toMap(SmartMatchCandidateVo::getUid, item -> item, (left, right) -> left));

        List<Integer> normalizedDelivered = deliveredToday.stream()
                .filter(Objects::nonNull)
                .filter(uidItem -> rankedMap.containsKey(uidItem) || actedUids.contains(uidItem))
                .distinct()
                .limit(dailyLimit)
                .collect(Collectors.toCollection(ArrayList::new));
        Set<Integer> deliveredSet = new HashSet<>(normalizedDelivered);

        List<SmartMatchCandidateVo> visible = normalizedDelivered.stream()
                .filter(uidItem -> !actedUids.contains(uidItem))
                .map(rankedMap::get)
                .filter(Objects::nonNull)
                .limit(visibleLimit)
                .collect(Collectors.toCollection(ArrayList::new));

        int beforeDeliveryCount = normalizedDelivered.size();
        if (normalizedDelivered.size() < dailyLimit && visible.size() < visibleLimit) {
            for (SmartMatchCandidateVo candidate : ranked) {
                if (candidate == null || candidate.getUid() == null) {
                    continue;
                }
                if (deliveredSet.contains(candidate.getUid()) || actedUids.contains(candidate.getUid())) {
                    continue;
                }
                normalizedDelivered.add(candidate.getUid());
                deliveredSet.add(candidate.getUid());
                visible.add(candidate);
                if (normalizedDelivered.size() >= dailyLimit || visible.size() >= visibleLimit) {
                    break;
                }
            }
        }

        writeDeliveredSmartMatchUids(uid, deliveryDay, normalizedDelivered);
        return new DailySmartMatchWindow(
                visible,
                normalizedDelivered.size(),
                Math.max(normalizedDelivered.size() - beforeDeliveryCount, 0)
        );
    }

    private List<Integer> readDeliveredSmartMatchUids(Integer uid, String deliveryDay) {
        if (uid == null || !hasText(deliveryDay)) {
            return Collections.emptyList();
        }
        RList<String> list = RedisUtils.getClient().getList(buildSmartMatchDailyKey(uid, deliveryDay), StringCodec.INSTANCE);
        return list.readAll().stream()
                .map(this::parseInteger)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private void writeDeliveredSmartMatchUids(Integer uid, String deliveryDay, List<Integer> deliveredUids) {
        if (uid == null || !hasText(deliveryDay)) {
            return;
        }
        String redisKey = buildSmartMatchDailyKey(uid, deliveryDay);
        RList<String> list = RedisUtils.getClient().getList(redisKey, StringCodec.INSTANCE);
        list.delete();
        if (deliveredUids == null || deliveredUids.isEmpty()) {
            return;
        }
        List<String> values = deliveredUids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.toList());
        if (values.isEmpty()) {
            return;
        }
        list.addAll(values);
        list.expire(SMART_MATCH_DAILY_TTL);
    }

    private String buildSmartMatchDailyKey(Integer uid, String deliveryDay) {
        return "smartMatch:" + deliveryDay + ":" + uid + ":delivered";
    }

    private void syncSmartMatchQuotaSnapshot(AppUserEntity user,
                                             RecommendLoveEntity setting,
                                             int deliveredCount,
                                             int dailyLimit,
                                             int newlyDeliveredCount) {
        RecommendLoveEntity target = setting;
        if (target == null || target.getId() == null) {
            target = lambdaQuery().eq(RecommendLoveEntity::getUid, user.getUid()).one();
        }
        if (target == null) {
            target = new RecommendLoveEntity();
            target.setUid(user.getUid());
            target.setRecommendUid(0);
            target.setCreateTime(DateUtil.nowDateTime());
            target.setHasRecNums(0);
        }
        target.setVip(user.getVip());
        target.setRecNum(Math.max(dailyLimit - deliveredCount, 0));
        target.setHasRecNums(Math.max(0, (target.getHasRecNums() == null ? 0 : target.getHasRecNums()) + Math.max(newlyDeliveredCount, 0)));
        saveOrUpdate(target);
    }

    private String buildSettingSummary(RecommendLoveEntity setting, List<String> selectedCities, int deliveredCount, int dailyLimit) {
        List<String> parts = new ArrayList<>();
        parts.add("今日已解锁 " + Math.min(deliveredCount, dailyLimit) + "/" + dailyLimit);
        if (selectedCities != null && !selectedCities.isEmpty()) {
            parts.add(String.join(" / ", selectedCities));
        }
        if (hasText(setting.getAge())) {
            parts.add(setting.getAge().replace(",", "-") + "岁");
        }
        if (hasText(setting.getEdu())) {
            parts.add(parseIntegerList(setting.getEdu()).stream()
                    .map(this::getEducationText)
                    .filter(item -> item != null && !item.isBlank())
                    .distinct()
                    .collect(Collectors.joining(" / ")));
        }
        return parts.stream()
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.joining(" · "));
    }

    private boolean matchesCity(AppUserEntity candidate, List<String> selectedCities) {
        if (selectedCities == null || selectedCities.isEmpty()) {
            return false;
        }
        for (String city : selectedCities) {
            if (isExactCityMatch(city, candidate.getLocationCity())
                    || isExactCityMatch(city, candidate.getCity())
                    || isExactCityMatch(city, candidate.getAbodeCity())) {
                return true;
            }
        }
        return false;
    }

    private boolean isExactCityMatch(String targetCity, String candidateCity) {
        String left = normalizeCityName(targetCity);
        String right = normalizeCityName(candidateCity);
        return isMeaningfulCity(left) && left.equals(right);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private long hoursSince(Date value) {
        if (value == null) {
            return Long.MAX_VALUE;
        }
        long diff = System.currentTimeMillis() - value.getTime();
        if (diff <= 0) {
            return 0;
        }
        return diff / (60 * 60 * 1000);
    }

    private String resolveCity(AppUserEntity user, String inputCity) {
        return Arrays.asList(
                        inputCity,
                        user.getLocationCity(),
                        user.getCity(),
                        user.getAbodeCity(),
                        user.getHomeCity()
                ).stream()
                .map(this::normalizeCityName)
                .filter(this::isMeaningfulCity)
                .findFirst()
                .orElse(null);
    }

    private boolean isMeaningfulCity(String city) {
        if (city == null || city.isBlank()) {
            return false;
        }
        return !Arrays.asList("未知", "定位中", "null", "undefined").contains(city);
    }

    private String normalizeCityName(String rawCity) {
        if (rawCity == null) {
            return "";
        }

        String text = rawCity.trim();
        if (text.isEmpty()) {
            return "";
        }

        for (String directCity : Arrays.asList("北京市", "天津市", "上海市", "重庆市")) {
            if (text.contains(directCity)) {
                return directCity.replace("市", "");
            }
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("([^\\s省自治区特别行政区]+?(?:市|自治州|地区|盟))")
                .matcher(text);
        String lastMatched = "";
        while (matcher.find()) {
            lastMatched = matcher.group(1);
        }
        if (!lastMatched.isEmpty()) {
            return lastMatched.replace("市", "");
        }

        String[] tokens = text.split("[\\s,/-]+");
        List<String> skipTokens = Arrays.asList("联通", "移动", "电信", "未知");
        for (int i = tokens.length - 1; i >= 0; i--) {
            String token = tokens[i] == null ? "" : tokens[i].trim();
            if (token.isEmpty() || skipTokens.contains(token) || token.endsWith("省") || token.endsWith("自治区")) {
                continue;
            }
            return token.replace("市", "");
        }

        return text.replace("市", "");
    }

    private AppReccomentUserResponse toResponse(AppUserEntity entity, boolean likedByOwner, boolean likesOwner) {
        AppReccomentUserResponse resp = new AppReccomentUserResponse();
        BeanUtils.copyProperties(entity, resp);
        resp.setPassword(null);
        resp.setLikedByOwner(likedByOwner);
        resp.setLikesOwner(likesOwner);
        return resp;
    }

    private List<Integer> listFollowUids(Integer uid, Integer status) {
        List<FollowEntity> list = followService.lambdaQuery()
                .eq(FollowEntity::getUid, uid)
                .eq(status != null, FollowEntity::getStatus, status)
                .select(FollowEntity::getFollowUid)
                .list();
        return list.stream()
                .map(FollowEntity::getFollowUid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Integer> listFansUids(Integer uid) {
        List<FollowEntity> list = followService.lambdaQuery()
                .eq(FollowEntity::getFollowUid, uid)
                .eq(FollowEntity::getStatus, 0)
                .select(FollowEntity::getUid)
                .list();
        return list.stream()
                .map(FollowEntity::getUid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Integer> parseIntegerList(String raw) {
        List<Integer> result = new ArrayList<>();
        if (!hasText(raw)) {
            return result;
        }
        for (String item : raw.split(",")) {
            if (item == null || item.isBlank()) {
                continue;
            }
            try {
                result.add(Integer.parseInt(item.trim()));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private Integer parseInteger(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private void appendDistinctCandidates(Map<Integer, AppUserEntity> target,
                                          List<AppUserEntity> source,
                                          int limit) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        for (AppUserEntity candidate : source) {
            if (candidate == null || candidate.getUid() == null || target.containsKey(candidate.getUid())) {
                continue;
            }
            target.put(candidate.getUid(), candidate);
            if (target.size() >= limit) {
                break;
            }
        }
    }

    private List<String> parseCsv(String raw) {
        if (!hasText(raw)) {
            return new ArrayList<>();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toList());
    }

    private List<String> parseJsonStringList(String raw) {
        if (!hasText(raw)) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(raw, String.class).stream()
                    .filter(item -> item != null && !item.isBlank())
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            return parseCsv(raw);
        }
    }

    private Integer parseHeight(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (Exception ex) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private int profileCompleteness(AppUserEntity candidate) {
        int score = 14;
        if (hasText(candidate.getAvatar()) || hasText(candidate.getFigur())) {
            score += 16;
        }
        if (hasText(candidate.getSelfIntroduction()) || hasText(candidate.getIntro())) {
            score += 16;
        }
        if (hasText(candidate.getInterest())) {
            score += 14;
        }
        if (hasText(candidate.getLoveDeclaration())) {
            score += 12;
        }
        if (hasText(candidate.getJob())) {
            score += 10;
        }
        if (candidate.getEducation() != null) {
            score += 8;
        }
        if (candidate.getAge() != null) {
            score += 6;
        }
        return Math.max(0, Math.min(100, score));
    }

    private boolean isVerifiedCandidate(AppUserEntity candidate) {
        return Objects.equals(candidate.getAuditStatus(), 1)
                || Objects.equals(candidate.getIdentyCertifStatus(), 1)
                || Objects.equals(candidate.getEduCertifStatus(), 1);
    }

    private Set<String> intersectKeywords(String left, String right, int limit) {
        Set<String> leftSet = splitKeywords(left);
        Set<String> rightSet = splitKeywords(right);
        LinkedHashSet<String> common = new LinkedHashSet<>(leftSet);
        common.retainAll(rightSet);
        return common.stream().limit(limit).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> splitKeywords(String text) {
        if (!hasText(text)) {
            return Collections.emptySet();
        }
        return Arrays.stream(text.split("[,，、/\\s]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String buildLocalSummary(AppUserEntity candidate, int score) {
        String name = defaultIfBlank(candidate.getUsername(), "这位用户");
        if (score >= 85) {
            return name + "是这批里更值得优先推进的一位，适合先认真开场。";
        }
        if (score >= 72) {
            return name + "和你的基础契合度不错，先聊起来成功率会更高。";
        }
        return name + "可以先看看主页和动态，再决定要不要继续认识。";
    }

    private List<String> jsonArrayToStringList(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return Collections.emptyList();
        }
        return array.toJavaList(String.class).stream()
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .limit(4)
                .collect(Collectors.toList());
    }

    private String firstListItem(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return fallback;
    }

    private String getEducationText(Integer education) {
        if (education == null) {
            return "";
        }
        return switch (education) {
            case 0 -> "未知";
            case 1 -> "专科";
            case 2 -> "本科";
            case 3 -> "硕士";
            case 4 -> "博士";
            case 5 -> "博士后";
            default -> "学历" + education;
        };
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(defaultIfBlank(raw, "").getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            return String.valueOf(raw.hashCode());
        }
    }

    @Data
    @AllArgsConstructor
    private static class CandidatePool {
        private List<AppUserEntity> candidates;
        private Set<Integer> likedUids;
        private Set<Integer> fanUids;
        private List<String> selectedCities;
        private String fallbackInsight;
    }

    @Data
    @AllArgsConstructor
    private static class DailySmartMatchWindow {
        private List<SmartMatchCandidateVo> visible;
        private int deliveredCount;
        private int newlyDeliveredCount;
    }

    @Data
    @AllArgsConstructor
    private static class PreGeneratedSmartMatch {
        private List<SmartMatchCandidateVo> candidates;
        private String provider;
        private String insight;
        private List<String> selectedCities;
        private Boolean usedRuntime;
    }
}
