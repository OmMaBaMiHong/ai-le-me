package org.aileme.shejiao.app.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aileme.common.redis.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.BillService;
import org.aileme.shejiao.api.service.PersonaViewRecordService;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.api.service.UserPersonaSnapshotService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.app.runtime.compliance.MiniAppFilingFeatureService;
import org.aileme.shejiao.app.service.PersonaPreGenerationService;
import org.aileme.shejiao.app.service.PersonaQuotaService;
import org.aileme.shejiao.app.service.PersonaReportService;
import org.aileme.shejiao.app.service.TagProfileAggregateService;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.PersonaViewRecordEntity;
import org.aileme.shejiao.domain.entity.admin.UserPersonaSnapshotEntity;
import org.aileme.shejiao.domain.vo.PersonaReportVO;
import org.aileme.shejiao.domain.vo.TagProfileAggregateVO;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * App端 - AI人物画像
 *
 * @author system
 * @date 2026-02-14
 */
@Slf4j
@RestController
@RequestMapping("/app/persona")
@Tag(name = "移动端——AI人物画像")
public class AppPersonaController {

    @Autowired
    private UserPersonaSnapshotService personaSnapshotService;

    @Autowired
    private PersonaViewRecordService viewRecordService;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private PersonaQuotaService quotaService;

    @Autowired
    private TagProfileAggregateService tagProfileAggregateService;

    @Autowired
    private MiniAppFilingFeatureService miniAppFilingFeatureService;

    @Autowired
    private PersonaReportService personaReportService;

    @Autowired
    private PersonaPreGenerationService personaPreGenerationService;

    @Autowired
    private BillService billService;

    @Autowired
    private SysConfigService sysConfigService;

    @Login
    @PostMapping("/generate")
    @Operation(summary = "生成用户画像")
    public Result<Map<String, Object>> generate(
            @Parameter(hidden = true) @LoginUser AppUserEntity user,
            @RequestBody Map<String, Object> body
    ) {
        try {
            String source = (String) body.getOrDefault("source", "manual_generate");
            boolean forcePay = parseBoolean(body.get("forcePay"), false);

            // 1. 检查VIP权限和积分
            boolean canFree = quotaService.canGenerateForFree(user);
            int cost = quotaService.getGenerateCost();

            if (!canFree && !forcePay) {
                // 需要付费但用户未确认
                Map<String, Object> quotaInfo = new HashMap<>();
                quotaInfo.put("needPay", true);
                quotaInfo.put("cost", cost);
                quotaInfo.put("vipRemainingQuota", quotaService.getVipRemainingQuota(user));
                quotaInfo.put("message", "VIP免费配额已用完，生成需消耗" + cost + "积分");
                return new Result<Map<String, Object>>().ok(quotaInfo);
            }

            // 2. 扣费(如果需要)
            if (!canFree) {
                // TODO: 实际扣除积分
                log.info("用户{}生成画像扣除{}积分", user.getUid(), cost);
                // 示例:integralService.deduct(user.getUid(), cost, "生成AI画像");
            }

            // 3. 生成画像
            UserPersonaSnapshotEntity snapshot = personaSnapshotService.generatePersona(user.getUid(), source);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("snapshotId", snapshot.getId());
            result.put("imageUrl", snapshot.getImageUrl());
            result.put("summary", snapshot.getSummary());
            result.put("generatedAt", snapshot.getGeneratedAt());
            result.put("paidIntegral", canFree ? 0 : cost);
            appendPersonaFields(result, snapshot);
            appendTagProfileFields(result, user.getUid());
            quotaService.recordManualGeneration(user.getUid());
            personaPreGenerationService.triggerProfileRefresh(user.getUid(), "manual_generate_refresh");

            return new Result<Map<String, Object>>().ok(result);
        } catch (Exception e) {
            log.error("生成画像失败", e);
            return new Result<Map<String, Object>>().error(e.getMessage());
        }
    }

    @Login
    @GetMapping("/my")
    @Operation(summary = "查看我的画像")
    public Result<Map<String, Object>> getMyPersona(
            @Parameter(hidden = true) @LoginUser AppUserEntity user
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasPersona", false);
        result.put("visibleToSelf", 1);
        result.put("visibleToOthers", 1);

        try {
            UserPersonaSnapshotEntity snapshot = personaSnapshotService.getLatestByUserId(user.getUid());
            if (snapshot != null) {
                result.put("hasPersona", true);
                result.put("snapshotId", snapshot.getId());
                result.put("imageUrl", snapshot.getImageUrl());
                result.put("summary", snapshot.getSummary());
                result.put("generatedAt", snapshot.getGeneratedAt());
                result.put("versionNo", snapshot.getVersionNo());
                result.put("visibleToSelf", snapshot.getVisibleToSelf() == null ? 1 : snapshot.getVisibleToSelf());
                result.put("visibleToOthers", snapshot.getVisibleToOthers() == null ? 1 : snapshot.getVisibleToOthers());
                appendPersonaFields(result, snapshot);
            }
        } catch (Exception e) {
            log.error("加载我的画像快照失败, uid={}", user.getUid(), e);
        }

        try {
            appendTagProfileFields(result, user.getUid());
        } catch (Exception e) {
            log.error("加载我的画像标签聚合失败, uid={}", user.getUid(), e);
        }

        try {
            result.put("vipRemainingQuota", quotaService.getVipRemainingQuota(user));
        } catch (Exception e) {
            log.error("加载我的画像VIP配额失败, uid={}", user.getUid(), e);
            result.put("vipRemainingQuota", 0);
        }

        try {
            result.put("generateCost", quotaService.getGenerateCost());
        } catch (Exception e) {
            log.error("加载我的画像生成费用失败, uid={}", user.getUid(), e);
            result.put("generateCost", 100);
        }

        return new Result<Map<String, Object>>().ok(result);
    }

    @Login
    @GetMapping("/view")
    @Operation(summary = "查看他人画像")
    public Result<Map<String, Object>> viewPersona(
            @Parameter(hidden = true) @LoginUser AppUserEntity user,
            @RequestParam Integer targetUserId
    ) {
        // 1. 检查是否已解锁
        boolean unlocked = viewRecordService.hasUnlocked(user.getUid(), targetUserId);

        // 2. 获取对方画像
        UserPersonaSnapshotEntity snapshot = personaSnapshotService.getLatestByUserId(targetUserId);

        Map<String, Object> result = new HashMap<>();
        if (snapshot == null) {
            result.put("hasPersona", false);
            return new Result<Map<String, Object>>().ok(result);
        }
        if (snapshot.getVisibleToOthers() != null && snapshot.getVisibleToOthers() == 0) {
            result.put("hasPersona", false);
            result.put("hidden", true);
            return new Result<Map<String, Object>>().ok(result);
        }

        result.put("hasPersona", true);
        result.put("unlocked", unlocked);
        result.put("snapshotId", snapshot.getId());
        result.put("summary", snapshot.getSummary());
        appendPersonaFields(result, snapshot);
        appendTagProfileFields(result, targetUserId);

        if (unlocked) {
            // 已解锁，返回完整图片URL
            result.put("imageUrl", snapshot.getImageUrl());
        } else {
            // 未解锁，返回模糊预览图
            result.put("blurImageUrl", buildBlurImageUrl(snapshot.getImageUrl()));
            result.put("unlockCost", quotaService.getViewCost());
        }

        return new Result<Map<String, Object>>().ok(result);
    }

    @Login
    @PostMapping("/visibility")
    @Operation(summary = "更新我的画像可见范围")
    public Result<Map<String, Object>> updateVisibility(
            @Parameter(hidden = true) @LoginUser AppUserEntity user,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        try {
            UserPersonaSnapshotEntity snapshot = personaSnapshotService.getLatestByUserId(user.getUid());
            if (snapshot == null) {
                throw new LinfengException("请先生成画像后再设置可见范围");
            }
            boolean visibleToOthers = parseBoolean(body == null ? null : body.get("visibleToOthers"), true);
            int visibleFlag = visibleToOthers ? 1 : 0;

            boolean updated = personaSnapshotService.lambdaUpdate()
                    .eq(UserPersonaSnapshotEntity::getUserId, user.getUid())
                    .eq(UserPersonaSnapshotEntity::getStatus, 1)
                    .set(UserPersonaSnapshotEntity::getVisibleToOthers, visibleFlag)
                    .update();
            if (!updated) {
                throw new LinfengException("更新失败，请稍后重试");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("hasPersona", true);
            result.put("visibleToOthers", visibleFlag);
            result.put("visibleToSelf", snapshot.getVisibleToSelf() == null ? 1 : snapshot.getVisibleToSelf());
            result.put("snapshotId", snapshot.getId());
            return new Result<Map<String, Object>>().ok(result);
        } catch (Exception e) {
            log.error("更新画像可见范围失败", e);
            return new Result<Map<String, Object>>().error(e.getMessage());
        }
    }

    @Login
    @PostMapping("/unlock")
    @Operation(summary = "解锁他人画像")
    public Result<Map<String, Object>> unlockPersona(
            @Parameter(hidden = true) @LoginUser AppUserEntity user,
            @RequestBody Map<String, Object> body
    ) {
        miniAppFilingFeatureService.requirePersonaPaidEnabled();
        try {
            Integer targetUserId = Integer.valueOf(body.get("targetUserId").toString());
            String payType = (String) body.getOrDefault("payType", "integral");
            if (targetUserId == null || targetUserId <= 0) {
                throw new LinfengException("目标用户不能为空");
            }
            if (user.getUid().equals(targetUserId)) {
                throw new LinfengException("不能解锁自己的画像");
            }

            // 从配置读取解锁费用
            int cost = quotaService.getViewCost();
            BigDecimal costDecimal = new BigDecimal(cost);

            // 获取对方画像
            UserPersonaSnapshotEntity snapshot = personaSnapshotService.getLatestByUserId(targetUserId);
            if (snapshot == null) {
                throw new LinfengException("对方暂未生成画像");
            }
            if (snapshot.getVisibleToOthers() != null && snapshot.getVisibleToOthers() == 0) {
                throw new LinfengException("对方暂未开放画像查看");
            }
            if (viewRecordService.hasUnlocked(user.getUid(), targetUserId)) {
                Map<String, Object> result = new HashMap<>();
                result.put("unlocked", true);
                result.put("imageUrl", snapshot.getImageUrl());
                result.put("paidIntegral", 0);
                result.put("remainingIntegral", appUserService.getById(user.getUid()).getIntegral());
                appendPersonaFields(result, snapshot);
                appendTagProfileFields(result, targetUserId);
                return new Result<Map<String, Object>>().ok(result);
            }

            int paidIntegral = 0;
            AppUserEntity latest = appUserService.getById(user.getUid());
            if (latest == null) {
                throw new LinfengException("用户不存在");
            }
            if (!"integral".equalsIgnoreCase(StringUtils.defaultString(payType))) {
                throw new LinfengException("暂不支持该支付方式");
            }
            if (cost > 0) {
                int currentIntegral = latest.getIntegral() == null ? 0 : latest.getIntegral();
                if (currentIntegral < cost) {
                    throw new LinfengException("积分不足，无法解锁画像");
                }
                boolean updated = appUserService.lambdaUpdate()
                        .set(AppUserEntity::getIntegral, currentIntegral - cost)
                        .eq(AppUserEntity::getUid, user.getUid())
                        .ge(AppUserEntity::getIntegral, cost)
                        .update();
                if (!updated) {
                    throw new LinfengException("积分扣除失败，请稍后重试");
                }
                paidIntegral = cost;
                int remain = currentIntegral - cost;
                billService.expend(
                        user.getUid(),
                        "解锁用户画像",
                        BillDetailEnum.CATEGORY_2.getValue(),
                        BillDetailEnum.TYPE_8.getValue(),
                        cost,
                        remain,
                        "解锁用户画像扣除积分",
                        "",
                        null
                );
                RedisUtils.deleteObject("userId:" + user.getUid());
            }

            // 创建解锁记录
            PersonaViewRecordEntity record = viewRecordService.unlockPersona(
                    user.getUid(),
                    targetUserId,
                    snapshot.getId(),
                    payType,
                    costDecimal
            );

            Map<String, Object> result = new HashMap<>();
            result.put("unlocked", true);
            result.put("imageUrl", snapshot.getImageUrl());
            result.put("recordId", record.getId());
            AppUserEntity afterPay = appUserService.getById(user.getUid());
            result.put("paidIntegral", paidIntegral);
            result.put("remainingIntegral", afterPay == null || afterPay.getIntegral() == null ? 0 : afterPay.getIntegral());
            appendPersonaFields(result, snapshot);
            appendTagProfileFields(result, targetUserId);

            return new Result<Map<String, Object>>().ok(result);
        } catch (Exception e) {
            log.error("解锁画像失败", e);
            return new Result<Map<String, Object>>().error(e.getMessage());
        }
    }

    @Login
    @GetMapping("/tagProfile")
    @Operation(summary = "查看标签画像聚合")
    public Result<TagProfileAggregateVO> tagProfile(
            @Parameter(hidden = true) @LoginUser AppUserEntity user,
            @RequestParam(required = false) Integer userId
    ) {
        Integer targetUid = userId != null && userId > 0 ? userId : user.getUid();
        TagProfileAggregateVO aggregate = tagProfileAggregateService.aggregate(targetUid);
        return new Result<TagProfileAggregateVO>().ok(aggregate);
    }

    @Login
    @PostMapping("/report/download")
    @Operation(summary = "积分下载画像报告")
    public Result<Map<String, Object>> downloadReport(
            @Parameter(hidden = true) @LoginUser AppUserEntity user,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        miniAppFilingFeatureService.requirePersonaPaidEnabled();
        try {
            boolean forcePay = parseBoolean(body == null ? null : body.get("forcePay"), false);
            Integer requestedTargetUid = parseInteger(body == null ? null : body.get("targetUserId"), user.getUid());
            Integer targetUserId = requestedTargetUid == null || requestedTargetUid <= 0 ? user.getUid() : requestedTargetUid;
            boolean selfDownload = user.getUid().equals(targetUserId);
            int cost = getReportCost();

            if (!selfDownload) {
                AppUserEntity targetUser = appUserService.getById(targetUserId);
                if (targetUser == null) {
                    throw new LinfengException("目标用户不存在");
                }
                UserPersonaSnapshotEntity snapshot = personaSnapshotService.getLatestByUserId(targetUserId);
                if (snapshot != null && snapshot.getVisibleToOthers() != null && snapshot.getVisibleToOthers() == 0) {
                    throw new LinfengException("对方暂未开放画像查看");
                }
            }

            AppUserEntity latest = appUserService.getById(user.getUid());
            if (latest == null) {
                throw new LinfengException("用户不存在");
            }

            Map<String, Object> result = new HashMap<>();
            if (cost > 0 && !forcePay) {
                result.put("needPay", true);
                result.put("cost", cost);
                result.put("currentIntegral", latest.getIntegral() == null ? 0 : latest.getIntegral());
                result.put("message", "下载画像报告需消耗" + cost + "积分");
                return new Result<Map<String, Object>>().ok(result);
            }

            int paidIntegral = 0;
            if (cost > 0) {
                int currentIntegral = latest.getIntegral() == null ? 0 : latest.getIntegral();
                if (currentIntegral < cost) {
                    throw new LinfengException("积分不足，无法下载画像报告");
                }
                boolean updated = appUserService.lambdaUpdate()
                        .set(AppUserEntity::getIntegral, currentIntegral - cost)
                        .eq(AppUserEntity::getUid, user.getUid())
                        .ge(AppUserEntity::getIntegral, cost)
                        .update();
                if (!updated) {
                    throw new LinfengException("积分扣除失败，请稍后重试");
                }
                paidIntegral = cost;
                int remain = currentIntegral - cost;
                billService.expend(
                        user.getUid(),
                        selfDownload ? "下载画像报告" : "下载用户画像报告",
                        BillDetailEnum.CATEGORY_2.getValue(),
                        BillDetailEnum.TYPE_8.getValue(),
                        cost,
                        remain,
                        selfDownload ? "下载画像报告扣除积分" : "下载用户画像报告扣除积分",
                        "",
                        null
                );
                RedisUtils.deleteObject("userId:" + user.getUid());
            }

            UserPersonaSnapshotEntity snapshot = personaSnapshotService.getLatestByUserId(targetUserId);
            PersonaReportVO report = personaReportService.extractCachedReport(snapshot);
            if (report == null) {
                report = personaReportService.buildReport(targetUserId);
            }
            AppUserEntity afterPay = appUserService.getById(user.getUid());
            result.put("needPay", false);
            result.put("paidIntegral", paidIntegral);
            result.put("remainingIntegral", afterPay == null || afterPay.getIntegral() == null ? 0 : afterPay.getIntegral());
            result.put("targetUserId", targetUserId);
            result.put("report", report);
            return new Result<Map<String, Object>>().ok(result);
        } catch (Exception e) {
            log.error("下载画像报告失败", e);
            return new Result<Map<String, Object>>().error(e.getMessage());
        }
    }

    @Login
    @DeleteMapping("/delete")
    @Operation(summary = "删除我的画像")
    public Result<Void> deleteMyPersona(
            @Parameter(hidden = true) @LoginUser AppUserEntity user
    ) {
        // 逻辑删除（将status设为0）
        personaSnapshotService.lambdaUpdate()
                .eq(UserPersonaSnapshotEntity::getUserId, user.getUid())
                .set(UserPersonaSnapshotEntity::getStatus, 0)
                .update();

        return new Result<Void>().ok();
    }

    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        if (StringUtils.isBlank(text)) {
            return defaultValue;
        }
        if (StringUtils.equalsAnyIgnoreCase(text, "1", "true", "yes", "y", "on")) {
            return true;
        }
        if (StringUtils.equalsAnyIgnoreCase(text, "0", "false", "no", "n", "off")) {
            return false;
        }
        return defaultValue;
    }

    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        if (StringUtils.isNumeric(text)) {
            try {
                return Integer.parseInt(text);
            } catch (Exception ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private void appendPersonaFields(Map<String, Object> result, UserPersonaSnapshotEntity snapshot) {
        if (snapshot == null || StringUtils.isBlank(snapshot.getPersonaJson())) {
            result.putIfAbsent("tagsHighlight", Collections.emptyList());
            return;
        }
        try {
            JSONObject persona = JSON.parseObject(snapshot.getPersonaJson());
            if (persona == null) {
                result.putIfAbsent("tagsHighlight", Collections.emptyList());
                return;
            }

            JSONArray tagsHighlight = persona.getJSONArray("tags_highlight");
            if (tagsHighlight == null) {
                tagsHighlight = persona.getJSONArray("interest_clusters");
            }
            List<String> tags = tagsHighlight != null ? tagsHighlight.toJavaList(String.class) : Collections.emptyList();
            result.put("tagsHighlight", tags);

            JSONArray suggestions = persona.getJSONArray("suggestions");
            if (suggestions == null) {
                suggestions = persona.getJSONArray("approach_suggestions");
            }
            if (suggestions != null) {
                result.put("suggestions", suggestions.toJavaList(String.class));
            }
            JSONObject personaKernel = persona.getJSONObject("persona_kernel");
            if (personaKernel != null && !personaKernel.isEmpty()) {
                result.put("personaKernel", personaKernel);
                if (personaKernel.getJSONArray("coach_style_candidates") != null) {
                    result.put("coachStyleCandidates", personaKernel.getJSONArray("coach_style_candidates").toJavaList(Map.class));
                }
                if (personaKernel.getJSONArray("love_language") != null) {
                    result.put("loveLanguage", personaKernel.getJSONArray("love_language").toJavaList(String.class));
                }
                if (StringUtils.isNotBlank(personaKernel.getString("romance_pace"))) {
                    result.put("romancePace", personaKernel.getString("romance_pace"));
                }
                if (StringUtils.isNotBlank(personaKernel.getString("preferred_master_style_code"))) {
                    result.put("preferredMasterStyleCode", personaKernel.getString("preferred_master_style_code"));
                }
                if (StringUtils.isNotBlank(personaKernel.getString("style_router_reason"))) {
                    result.put("styleRouterReason", personaKernel.getString("style_router_reason"));
                }
            }
            JSONObject personality = persona.getJSONObject("personality");
            if (personality == null && persona.getJSONArray("core_traits") != null) {
                personality = new JSONObject(true);
                JSONArray coreTraits = persona.getJSONArray("core_traits");
                for (int i = 0; i < coreTraits.size(); i++) {
                    JSONObject trait = coreTraits.getJSONObject(i);
                    if (trait == null) {
                        continue;
                    }
                    String name = trait.getString("name");
                    if ("stability".equalsIgnoreCase(name)) {
                        personality.put("stability", trait.getString("reason"));
                    } else if ("openness".equalsIgnoreCase(name)) {
                        personality.put("introvert_extrovert", trait.getString("reason"));
                    } else if ("engagement".equalsIgnoreCase(name)) {
                        personality.put("openness", trait.getString("reason"));
                    }
                }
            }
            if (personality != null) {
                result.put("personality", personality);
            }
            JSONObject loveStyle = persona.getJSONObject("love_style");
            if (loveStyle == null && (persona.getString("attachment_style") != null || persona.getJSONArray("risk_flags") != null)) {
                loveStyle = new JSONObject(true);
                loveStyle.put("attitude", persona.getString("attachment_style"));
                loveStyle.put("risk_points", persona.getJSONArray("risk_flags"));
            }
            if (loveStyle == null && personaKernel != null
                    && (personaKernel.getString("attachment_style") != null || personaKernel.getJSONArray("taboo_rules") != null)) {
                loveStyle = new JSONObject(true);
                loveStyle.put("attitude", personaKernel.getString("attachment_style"));
                loveStyle.put("risk_points", personaKernel.getJSONArray("taboo_rules"));
            }
            if (loveStyle != null) {
                result.put("loveStyle", loveStyle);
            }
            JSONObject socialStyle = persona.getJSONObject("social_style");
            if (socialStyle == null && persona.getString("emotional_style") != null) {
                socialStyle = new JSONObject(true);
                socialStyle.put("online", persona.getString("emotional_style"));
                socialStyle.put("offline", persona.getString("emotional_style"));
            }
            if (socialStyle == null && personaKernel != null && StringUtils.isNotBlank(personaKernel.getString("expression_style"))) {
                socialStyle = new JSONObject(true);
                socialStyle.put("online", personaKernel.getString("expression_style"));
                socialStyle.put("offline", personaKernel.getString("expression_style"));
            }
            if (socialStyle != null) {
                result.put("socialStyle", socialStyle);
            }
            if (persona.getJSONArray("evidence_digest") != null) {
                result.put("evidenceDigest", persona.getJSONArray("evidence_digest").toJavaList(String.class));
            }
            if (StringUtils.isNotBlank(persona.getString("debug_trace_id"))) {
                result.put("debugTraceId", persona.getString("debug_trace_id"));
            }
        } catch (Exception e) {
            log.warn("解析personaJson失败, snapshotId={}, err={}", snapshot.getId(), e.getMessage());
            result.putIfAbsent("tagsHighlight", Collections.emptyList());
        }
    }

    private void appendTagProfileFields(Map<String, Object> result, Integer userId) {
        if (!isEnabled("persona_aggregate_enabled", true)) {
            return;
        }
        TagProfileAggregateVO aggregate = tagProfileAggregateService.aggregate(userId);
        if (aggregate == null) {
            return;
        }
        result.put("tagProfile", aggregate);
        result.put("selfTags", aggregate.getSelfTags());
        result.put("impressionTop", aggregate.getImpressionTop());
        result.put("followTopicTags", aggregate.getFollowTopicTags());
        result.put("behaviorTags", aggregate.getBehaviorTags());
    }

    private int getReportCost() {
        try {
            String cost = sysConfigService.getValue("persona_report_cost_integral");
            if (StringUtils.isNumeric(StringUtils.trimToEmpty(cost))) {
                return Integer.parseInt(cost.trim());
            }
        } catch (Exception ignored) {
        }
        return 30;
    }

    private boolean isEnabled(String key, boolean defaultValue) {
        try {
            String value = sysConfigService.getValue(key);
            if (StringUtils.isBlank(value)) {
                return defaultValue;
            }
            return !"0".equals(value.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private String buildBlurImageUrl(String imageUrl) {
        if (StringUtils.isBlank(imageUrl)) {
            return "";
        }
        int dotIndex = imageUrl.lastIndexOf('.');
        int slashIndex = imageUrl.lastIndexOf('/');
        if (dotIndex > slashIndex) {
            return imageUrl.substring(0, dotIndex) + "_blur" + imageUrl.substring(dotIndex);
        }
        return imageUrl + "_blur";
    }
}
