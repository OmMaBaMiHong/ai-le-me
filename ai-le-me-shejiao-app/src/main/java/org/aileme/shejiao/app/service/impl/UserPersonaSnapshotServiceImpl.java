package org.aileme.shejiao.app.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.UserPersonaSnapshotService;
import org.aileme.shejiao.app.dao.UserPersonaSnapshotDao;
import org.aileme.shejiao.app.service.PersonaReportService;
import org.aileme.shejiao.app.service.TagProfileAggregateService;
import org.aileme.shejiao.app.service.agent.AgentRuntimeBridgeService;
import org.aileme.shejiao.app.service.ai.AIPersonaStrategyManager;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.UserPersonaSnapshotEntity;
import org.aileme.shejiao.domain.vo.PersonaReportVO;
import org.aileme.shejiao.domain.vo.TagProfileAggregateVO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户画像快照服务实现
 *
 * @author system
 * @date 2026-02-14
 */
@Slf4j
@Service
public class UserPersonaSnapshotServiceImpl extends ServiceImpl<UserPersonaSnapshotDao, UserPersonaSnapshotEntity>
        implements UserPersonaSnapshotService {

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private AIPersonaStrategyManager aiPersonaStrategyManager;

    @Autowired
    private TagProfileAggregateService tagProfileAggregateService;

    @Autowired
    private PersonaReportService personaReportService;

    @Autowired(required = false)
    private AgentRuntimeBridgeService agentRuntimeBridgeService;

    @Override
    public UserPersonaSnapshotEntity getLatestByUserId(Integer userId) {
        return this.lambdaQuery()
                .eq(UserPersonaSnapshotEntity::getUserId, userId)
                .eq(UserPersonaSnapshotEntity::getStatus, 1)
                .orderByDesc(UserPersonaSnapshotEntity::getGeneratedAt)
                .last("LIMIT 1")
                .one();
    }

    @Override
    @DSTransactional
    public UserPersonaSnapshotEntity generatePersona(Integer userId, String source) {
        log.info("开始生成用户画像: userId={}, source={}", userId, source);

        // 1. 获取用户信息
        AppUserEntity user = appUserService.getById(userId);
        if (user == null) {
            throw new LinfengException("用户不存在");
        }

        // 2. 聚合特征数据，并先判断资料是否足够支撑画像
        TagProfileAggregateVO aggregate = tagProfileAggregateService.aggregate(user.getUid());
        ensurePersonaProfileReady(user, aggregate);
        String profileHash = buildProfileHash(user, aggregate);
        UserPersonaSnapshotEntity latestSnapshot = getLatestByUserId(userId);
        if (latestSnapshot != null && StringUtils.equals(profileHash, extractProfileHash(latestSnapshot.getPersonaJson()))) {
            UserPersonaSnapshotEntity current = ensureSnapshotAssets(latestSnapshot, userId, profileHash);
            cleanupOtherSnapshots(userId, current == null ? null : current.getId());
            return current;
        }
        Map<String, Object> features = buildUserFeatures(user, aggregate);

        // 3. 优先调用 Python runtime 生成增强画像，失败时回退到本地AI能力
        String personaJson = callRuntimeGeneratePersona(user);
        if (org.apache.commons.lang3.StringUtils.isBlank(personaJson)) {
            personaJson = callAIGeneratePersona(features);
        }
        personaJson = enrichPersonaJson(userId, source, profileHash, personaJson);

        // 4. 解析JSON提取summary
        String summary = extractSummary(personaJson);

        // 5. 渲染画像图片（这里先返回占位URL，后续实现真实渲染服务）
        String imageUrl = renderPersonaImage(personaJson, userId);

        // 6. 只保留当前一份画像，不累积历史版本
        LocalDateTime now = LocalDateTime.now();
        UserPersonaSnapshotEntity snapshot = latestSnapshot != null ? latestSnapshot : new UserPersonaSnapshotEntity();
        snapshot.setUserId(userId);
        snapshot.setPersonaJson(personaJson);
        snapshot.setImageUrl(imageUrl);
        snapshot.setSummary(summary);
        snapshot.setVersionNo(1);
        snapshot.setSource(source);
        snapshot.setVisibleToSelf(latestSnapshot != null && latestSnapshot.getVisibleToSelf() != null ? latestSnapshot.getVisibleToSelf() : 1);
        snapshot.setVisibleToOthers(latestSnapshot != null && latestSnapshot.getVisibleToOthers() != null ? latestSnapshot.getVisibleToOthers() : 1);
        snapshot.setStatus(1);
        snapshot.setGeneratedAt(now);
        snapshot.setUpdateTime(now);
        if (snapshot.getId() == null) {
            snapshot.setCreateTime(now);
            this.save(snapshot);
        } else {
            this.updateById(snapshot);
        }
        cleanupOtherSnapshots(userId, snapshot.getId());

        log.info("用户画像生成完成: userId={}, snapshotId={}", userId, snapshot.getId());
        return snapshot;
    }

    private void cleanupOtherSnapshots(Integer userId, Integer keepId) {
        if (userId == null) {
            return;
        }
        List<UserPersonaSnapshotEntity> snapshots = this.lambdaQuery()
                .eq(UserPersonaSnapshotEntity::getUserId, userId)
                .list();
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        List<Integer> removeIds = snapshots.stream()
                .map(UserPersonaSnapshotEntity::getId)
                .filter(Objects::nonNull)
                .filter(id -> keepId == null || !Objects.equals(id, keepId))
                .collect(Collectors.toList());
        if (!removeIds.isEmpty()) {
            this.removeByIds(removeIds);
        }
    }

    private UserPersonaSnapshotEntity ensureSnapshotAssets(UserPersonaSnapshotEntity snapshot,
                                                           Integer userId,
                                                           String profileHash) {
        if (snapshot == null || StringUtils.isBlank(snapshot.getPersonaJson())) {
            return snapshot;
        }
        try {
            JSONObject root = JSON.parseObject(snapshot.getPersonaJson());
            if (root == null) {
                return snapshot;
            }
            boolean changed = false;
            if (!StringUtils.equals(profileHash, root.getString("profile_hash"))) {
                root.put("profile_hash", profileHash);
                changed = true;
            }
            if (!Objects.equals(snapshot.getVersionNo(), 1)) {
                snapshot.setVersionNo(1);
                changed = true;
            }
            if (root.getJSONObject("report_cache") == null || root.getJSONObject("report_cache").isEmpty()) {
                PersonaReportVO report = personaReportService.buildReportFromPersonaJson(userId, root);
                if (report != null) {
                    root.put("report_cache", JSON.toJSON(report));
                    changed = true;
                }
            }
            if (!changed) {
                return snapshot;
            }
            snapshot.setPersonaJson(root.toJSONString());
            snapshot.setSummary(extractSummary(snapshot.getPersonaJson()));
            LocalDateTime now = LocalDateTime.now();
            snapshot.setUpdateTime(now);
            this.lambdaUpdate()
                    .eq(UserPersonaSnapshotEntity::getId, snapshot.getId())
                    .set(UserPersonaSnapshotEntity::getPersonaJson, snapshot.getPersonaJson())
                    .set(UserPersonaSnapshotEntity::getSummary, snapshot.getSummary())
                    .set(UserPersonaSnapshotEntity::getVersionNo, snapshot.getVersionNo())
                    .set(UserPersonaSnapshotEntity::getUpdateTime, now)
                    .update();
            return snapshot;
        } catch (Exception ex) {
            log.warn("补全画像快照缓存失败, snapshotId={}, err={}", snapshot.getId(), ex.getMessage());
            return snapshot;
        }
    }

    /**
     * 聚合用户特征数据
     */
    private Map<String, Object> buildUserFeatures(AppUserEntity user, TagProfileAggregateVO aggregate) {
        Map<String, Object> features = new HashMap<>();

        // 基础信息
        Map<String, Object> basic = new HashMap<>();
        basic.put("gender", resolveGenderText(user.getGender()));
        basic.put("age", user.getAge());
        basic.put("city", user.getCity());
        basic.put("job", user.getJob());
        features.put("basic", basic);

        // 标签数据（统一聚合口径）
        Map<String, Object> tags = new HashMap<>();
        tags.put("selfTags", aggregate.getSelfTags());
        tags.put("impressionTop", aggregate.getImpressionTop());
        tags.put("followTopicTags", aggregate.getFollowTopicTags());
        tags.put("behaviorTags", aggregate.getBehaviorTags());
        features.put("tags", tags);
        features.put("tagProfileAggregate", aggregate);

        // 自我介绍等文本内容
        Map<String, Object> content = new HashMap<>();
        content.put("selfIntro", user.getSelfIntroduction());
        content.put("interest", user.getInterest());
        content.put("loveDeclaration", user.getLoveDeclaration());
        features.put("content", content);

        return features;
    }

    private void ensurePersonaProfileReady(AppUserEntity user, TagProfileAggregateVO aggregate) {
        int structuredScore = 0;
        int contentScore = 0;

        if (hasUsableProfileMedia(user)) {
            structuredScore += 2;
        }
        if (user.getAge() != null && user.getAge() > 0) {
            structuredScore += 1;
        }
        if (StringUtils.isNotBlank(resolvePrimaryCity(user))) {
            structuredScore += 1;
        }
        if (StringUtils.isNotBlank(user.getJob())) {
            structuredScore += 1;
        }
        if (user.getEducation() != null && user.getEducation() > 0) {
            structuredScore += 1;
        }

        if (StringUtils.isNotBlank(user.getSelfIntroduction())) {
            contentScore += 2;
        }
        if (StringUtils.isNotBlank(user.getLoveDeclaration())) {
            contentScore += 1;
        }
        if (StringUtils.isNotBlank(user.getInterest())) {
            contentScore += 1;
        }
        if (aggregate != null && aggregate.getSelfTags() != null && !aggregate.getSelfTags().isEmpty()) {
            contentScore += 1;
        }

        boolean hasBaseIdentity = structuredScore >= 3;
        boolean hasEnoughExpression = contentScore >= 2;
        boolean hasEnoughSignals = structuredScore + contentScore >= 5;
        if (hasBaseIdentity && hasEnoughExpression && hasEnoughSignals) {
            return;
        }

        throw new LinfengException("资料太少，暂不建议生成画像。先完善头像或形象照、城市/职业/学历，以及自我介绍、兴趣爱好后再试");
    }

    private String enrichPersonaJson(Integer userId, String source, String profileHash, String personaJson) {
        JSONObject root;
        try {
            root = JSON.parseObject(personaJson);
        } catch (Exception ex) {
            root = new JSONObject(true);
            root.put("summary", "画像生成成功");
        }
        if (root == null) {
            root = new JSONObject(true);
        }
        root.put("profile_hash", profileHash);
        root.put("generated_source", source);
        PersonaReportVO report = personaReportService.buildReportFromPersonaJson(userId, root);
        if (report != null) {
            root.put("report_cache", JSON.toJSON(report));
        }
        return root.toJSONString();
    }

    /**
     * 调用AI生成画像(通过策略管理器自动选择提供商)
     */
    private String callAIGeneratePersona(Map<String, Object> features) {
        try {
            return aiPersonaStrategyManager.generatePersona(features);
        } catch (Exception e) {
            log.error("AI生成画像失败，使用兜底数据", e);
            return buildFallbackPersona(features);
        }
    }

    private String callRuntimeGeneratePersona(AppUserEntity user) {
        try {
            if (agentRuntimeBridgeService == null || !agentRuntimeBridgeService.isPersonaEnabled()) {
                return "";
            }
            JSONObject runtimeReport = agentRuntimeBridgeService.generatePersonaReport(user, user);
            if (runtimeReport == null || runtimeReport.isEmpty()) {
                return "";
            }
            return runtimeReport.toJSONString();
        } catch (Exception e) {
            log.warn("Python runtime 生成画像失败，回退本地AI: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 兜底画像(当AI调用失败时使用)
     */
    private String buildFallbackPersona(Map<String, Object> features) {
        return "{"
                + "\"summary\":\"慢热但非常认真，对长期关系有期待\","
                + "\"provider\":\"fallback_local\","
                + "\"personality\":{\"introvert_extrovert\":\"偏内向\",\"stability\":\"情绪比较稳定\"},"
                + "\"love_style\":{\"attitude\":\"认真、慢热、不随便开始\",\"risk_points\":[\"容易多想\",\"表达不够主动\"]},"
                + "\"social_style\":{\"online\":\"擅长文字表达\",\"offline\":\"初次见面略拘谨\"},"
                + "\"tags_highlight\":[\"慢热认真\",\"重视长期关系\",\"愿意沟通\"],"
                + "\"suggestions\":[\"在聊天中可以多分享自己的真实想法\",\"适当参加线下圈子活动\"],"
                + "\"interest_clusters\":[\"慢热认真\",\"重视长期关系\",\"愿意沟通\"],"
                + "\"approach_suggestions\":[\"在聊天中可以多分享自己的真实想法\",\"适当参加线下圈子活动\"],"
                + "\"evidence_digest\":[\"fallback persona response\"]"
                + "}";
    }

    /**
     * 从JSON中提取summary
     */
    private String extractSummary(String personaJson) {
        try {
            JSONObject root = JSON.parseObject(personaJson);
            String summary = root.getString("summary");
            if (summary != null && !summary.trim().isEmpty()) {
                return summary;
            }
        } catch (Exception e) {
            log.warn("提取画像summary失败，使用默认文案: {}", e.getMessage());
        }
        return "AI画像生成成功";
    }

    /**
     * 渲染画像图片（当前返回占位URL）
     * TODO: 实际实现HTML模板+截图服务
     */
    private String renderPersonaImage(String personaJson, Integer userId) {
        // 返回占位图片URL
        return "https://example.com/persona/placeholder_" + userId + ".png";
    }

    private String buildProfileHash(AppUserEntity user, TagProfileAggregateVO aggregate) {
        String raw = String.join("|",
                StringUtils.defaultString(user.getAvatar()),
                StringUtils.defaultString(user.getFigur()),
                String.valueOf(user.getAge()),
                StringUtils.defaultString(resolvePrimaryCity(user)),
                StringUtils.defaultString(user.getJob()),
                String.valueOf(user.getEducation()),
                StringUtils.defaultString(user.getSelfIntroduction()),
                StringUtils.defaultString(user.getLoveDeclaration()),
                StringUtils.defaultString(user.getInterest()),
                StringUtils.defaultString(user.getIntro()),
                joinValues(aggregate == null ? null : aggregate.getSelfTags()),
                joinValues(aggregate == null ? null : aggregate.getBehaviorTags()),
                joinValues(aggregate == null ? null : aggregate.getImpressionTop()),
                joinValues(aggregate == null ? null : aggregate.getFollowTopicTags())
        );
        return sha256Hex(raw);
    }

    private String extractProfileHash(String personaJson) {
        if (StringUtils.isBlank(personaJson)) {
            return "";
        }
        try {
            JSONObject root = JSON.parseObject(personaJson);
            return root == null ? "" : StringUtils.defaultString(root.getString("profile_hash"));
        } catch (Exception ex) {
            return "";
        }
    }

    private String joinValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(StringUtils.defaultString(raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format(Locale.ROOT, "%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            return Integer.toHexString(StringUtils.defaultString(raw).hashCode());
        }
    }

    private String resolveGenderText(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        if (gender == 1) {
            return "男";
        }
        if (gender == 2) {
            return "女";
        }
        return "未知";
    }

    private boolean hasUsableProfileMedia(AppUserEntity user) {
        if (isUsableProfileImage(user.getAvatar())) {
            return true;
        }
        for (String item : parseFigureImages(user.getFigur())) {
            if (isUsableProfileImage(item)) {
                return true;
            }
        }
        return false;
    }

    private List<String> parseFigureImages(String figur) {
        if (StringUtils.isBlank(figur)) {
            return Collections.emptyList();
        }
        String raw = figur.trim();
        if (raw.startsWith("[")) {
            List<String> list = JSON.parseArray(raw, String.class);
            return list == null ? Collections.emptyList() : list;
        }
        String[] split = raw.split("\\s*,\\s*");
        List<String> list = new ArrayList<>(split.length);
        for (String item : split) {
            if (StringUtils.isNotBlank(item)) {
                list.add(item.trim());
            }
        }
        return list;
    }

    private boolean isUsableProfileImage(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        return !StringUtils.containsAny(normalized,
                "/static/default-avatar.png",
                "/static/images/default-avatar.png",
                "/static/images/unlogin_avatar.png",
                "default-avatar",
                "default_avatar",
                "avatar-default");
    }

    private String resolvePrimaryCity(AppUserEntity user) {
        if (StringUtils.isNotBlank(user.getLocationCity())) {
            return user.getLocationCity();
        }
        if (StringUtils.isNotBlank(user.getCity())) {
            return user.getCity();
        }
        if (StringUtils.isNotBlank(user.getAbodeCity())) {
            return user.getAbodeCity();
        }
        return user.getHomeCity();
    }

}
