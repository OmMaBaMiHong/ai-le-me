package org.aileme.shejiao.app.service;

import lombok.extern.slf4j.Slf4j;
import org.aileme.common.core.utils.StringUtils;
import org.aileme.common.redis.utils.RedisUtils;
import org.aileme.system.service.ISysThirdPartyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 画像配额检查服务
 *
 * @author system
 * @date 2026-02-14
 */
@Slf4j
@Service
public class PersonaQuotaService {

    private static final String PERSONA_MONTHLY_QUOTA_KEY = "persona:quota:month:";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Autowired
    private ISysThirdPartyService thirdPartyService;

    /**
     * 检查用户是否可以生成画像(VIP配额检查)
     *
     * @param user 用户信息
     * @return true=可以生成, false=配额不足需付费
     */
    public boolean canGenerateForFree(AppUserEntity user) {
        // 检查是否VIP
        if (!isVip(user)) {
            log.debug("用户{}非VIP，需要付费生成", user.getUid());
            return false;
        }

        // 检查本月已生成次数
        int monthlyGenerated = getMonthlyGeneratedCount(user.getUid());
        int vipFreeMonthly = getVipFreeMonthly();
        log.debug("用户{}本月已生成{}次，VIP免费配额{}次", user.getUid(), monthlyGenerated, vipFreeMonthly);

        return monthlyGenerated < vipFreeMonthly;
    }

    /**
     * 获取本月已生成次数
     */
    public int getMonthlyGeneratedCount(Integer userId) {
        if (userId == null || userId <= 0) {
            return 0;
        }
        Object cached = RedisUtils.getCacheObject(buildMonthlyQuotaKey(userId));
        if (cached == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(cached));
        } catch (Exception ex) {
            log.warn("读取画像月度配额计数失败, userId={}, raw={}", userId, cached);
            return 0;
        }
    }

    public void recordManualGeneration(Integer userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        String redisKey = buildMonthlyQuotaKey(userId);
        int next = getMonthlyGeneratedCount(userId) + 1;
        RedisUtils.setCacheObject(redisKey, String.valueOf(next), currentMonthExpireTtl());
    }

    /**
     * 获取VIP剩余免费次数
     */
    public int getVipRemainingQuota(AppUserEntity user) {
        if (!isVip(user)) {
            return 0;
        }
        int used = getMonthlyGeneratedCount(user.getUid());
        int vipFreeMonthly = getVipFreeMonthly();
        return Math.max(0, vipFreeMonthly - used);
    }

    /**
     * 检查用户是否VIP（简化版，后续根据实际VIP字段调整）
     */
    private boolean isVip(AppUserEntity user) {
        // TODO: 根据实际VIP字段调整
        // 暂时返回false，表示所有用户都需要消耗积分
        return false;
    }

    /**
     * 获取VIP月免费次数(从数据库读取)
     */
    private int getVipFreeMonthly() {
        String value = thirdPartyService.getConfigValue("ai", "common", "vip_free_monthly");
        return StringUtils.isNotBlank(value) ? Integer.parseInt(value) : 3;
    }

    /**
     * 获取生成画像费用(从数据库读取)
     */
    public int getGenerateCost() {
        String value = thirdPartyService.getConfigValue("ai", "common", "generate_cost");
        return StringUtils.isNotBlank(value) ? Integer.parseInt(value) : 100;
    }

    /**
     * 获取查看他人画像费用(从数据库读取)
     */
    public int getViewCost() {
        String value = thirdPartyService.getConfigValue("ai", "common", "view_cost");
        return StringUtils.isNotBlank(value) ? Integer.parseInt(value) : 50;
    }

    private String buildMonthlyQuotaKey(Integer userId) {
        return PERSONA_MONTHLY_QUOTA_KEY + YearMonth.now().format(MONTH_FORMATTER) + ":" + userId;
    }

    private Duration currentMonthExpireTtl() {
        LocalDateTime nextMonthStart = LocalDate.now().withDayOfMonth(1).plusMonths(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, nextMonthStart).plusDays(2);
        return duration.isNegative() || duration.isZero() ? Duration.ofDays(2) : duration;
    }
}
