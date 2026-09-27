package org.aileme.shejiao.app.service.agent;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AccountService;
import org.aileme.shejiao.api.service.BillService;
import org.aileme.shejiao.api.service.PlatformBusinessConfigService;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AgentCoinBillingPlugin implements AgentBillingPlugin {

    private static final String DAILY_FREE_KEY_PREFIX = "agent:companion:quota:day:";
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PlatformBusinessConfigService businessConfigService;
    private final AccountService accountService;
    private final BillService billService;
    private final AgentCompanionQuotaStore agentCompanionQuotaStore;

    @Autowired
    public AgentCoinBillingPlugin(PlatformBusinessConfigService businessConfigService,
                                  AccountService accountService,
                                  BillService billService,
                                  AgentCompanionQuotaStore agentCompanionQuotaStore) {
        this.businessConfigService = businessConfigService;
        this.accountService = accountService;
        this.billService = billService;
        this.agentCompanionQuotaStore = agentCompanionQuotaStore;
    }

    @Override
    public String modeCode() {
        return "coin";
    }

    @Override
    public boolean isBillingEnabled(String sceneCode) {
        return Boolean.TRUE.equals(businessConfigService.getBoolean("agent.companion.billing.enabled", true));
    }

    @Override
    public void ensureSufficientBudget(AppUserEntity user, String sceneCode, int inputChars) {
        if (user == null || !isBillingEnabled(sceneCode)) {
            return;
        }
        if (canConsumeVipFreeQuota(user, sceneCode)) {
            return;
        }
        int requiredCoin = estimateCoin(sceneCode, inputChars);
        int balance = accountService.getCoinBalance(user.getUid()).intValue();
        if (balance < requiredCoin) {
            throw new LinfengException("爱情币不足，请先充值或等待明日免费额度恢复");
        }
    }

    @Override
    public void settleUsage(AppUserEntity user,
                            Integer targetUid,
                            String sceneCode,
                            int inputChars,
                            String referenceId) {
        if (user == null || !isBillingEnabled(sceneCode)) {
            return;
        }
        if (consumeVipFreeQuotaIfAvailable(user, sceneCode)) {
            return;
        }
        int requiredCoin = estimateCoin(sceneCode, inputChars);
        BigDecimal remain = accountService.decreaseCoin(user.getUid(), requiredCoin);
        billService.expend(
                user.getUid(),
                "智能恋爱助手",
                BillDetailEnum.CATEGORY_2.getValue(),
                BillDetailEnum.TYPE_22.getValue(),
                requiredCoin,
                remain.doubleValue(),
                buildBillMark(sceneCode, inputChars),
                StringUtils.defaultIfBlank(referenceId, sceneCode),
                targetUid
        );
    }

    @Override
    public int estimateCoin(String sceneCode, int inputChars) {
        int inputTokens = estimateInputTokens(inputChars);
        int outputTokens = resolveEstimatedOutputTokens(sceneCode);
        int inputPer1k = businessConfigService.getInt("agent.companion.billing.inputCoinPer1kTokens", 1);
        int outputPer1k = businessConfigService.getInt("agent.companion.billing.outputCoinPer1kTokens", 4);
        int minCoin = resolveSceneMinCoin(sceneCode);
        double estimate = (inputTokens * Math.max(0, inputPer1k) + outputTokens * Math.max(0, outputPer1k)) / 1000D;
        int ceilValue = (int) Math.ceil(estimate);
        return Math.max(Math.max(0, minCoin), ceilValue);
    }

    @Override
    public int getVipDailyFreeQuota(String sceneCode) {
        return Math.max(0, businessConfigService.getInt("agent.companion.billing.vipDailyFreeCalls", 20));
    }

    @Override
    public int getVipDailyFreeRemaining(AppUserEntity user, String sceneCode) {
        if (user == null || !isVipActive(user)) {
            return 0;
        }
        int quota = getVipDailyFreeQuota(sceneCode);
        int used = agentCompanionQuotaStore.getInt(buildDailyFreeKey(user.getUid()));
        return Math.max(0, quota - used);
    }

    private boolean canConsumeVipFreeQuota(AppUserEntity user, String sceneCode) {
        return isVipActive(user) && getVipDailyFreeRemaining(user, sceneCode) > 0;
    }

    private boolean consumeVipFreeQuotaIfAvailable(AppUserEntity user, String sceneCode) {
        if (!canConsumeVipFreeQuota(user, sceneCode)) {
            return false;
        }
        agentCompanionQuotaStore.increment(buildDailyFreeKey(user.getUid()), currentDayExpireTtl());
        return true;
    }

    private boolean isVipActive(AppUserEntity user) {
        if (user == null || user.getUid() == null) {
            return false;
        }
        return Constant.VIP_USER.equals(user.getVip())
                && user.getVipExpireTime() != null
                && user.getVipExpireTime().after(new java.util.Date());
    }

    private int estimateInputTokens(int inputChars) {
        int safeChars = Math.max(0, inputChars);
        return Math.max(1, (int) Math.ceil(safeChars / 4D));
    }

    private int resolveEstimatedOutputTokens(String sceneCode) {
        return Math.max(64, businessConfigService.getInt(sceneOutputTokensKey(sceneCode), 220));
    }

    private int resolveSceneMinCoin(String sceneCode) {
        int globalMin = businessConfigService.getInt("agent.companion.billing.minCoinPerCall", 1);
        return Math.max(globalMin, businessConfigService.getInt(sceneMinCoinKey(sceneCode), globalMin));
    }

    private String buildBillMark(String sceneCode, int inputChars) {
        return "scene=" + StringUtils.defaultIfBlank(sceneCode, "unknown")
                + ",estimatedInputTokens=" + estimateInputTokens(inputChars)
                + ",estimatedOutputTokens=" + resolveEstimatedOutputTokens(sceneCode);
    }

    private String buildDailyFreeKey(Integer uid) {
        return DAILY_FREE_KEY_PREFIX + LocalDate.now().format(DAY_FORMATTER) + ":" + uid;
    }

    private Duration currentDayExpireTtl() {
        LocalDateTime nextDay = LocalDate.now().plusDays(1).atStartOfDay();
        Duration duration = Duration.between(LocalDateTime.now(), nextDay).plusDays(1);
        return duration.isNegative() || duration.isZero() ? Duration.ofDays(1) : duration;
    }

    private String sceneOutputTokensKey(String sceneCode) {
        return "agent.companion.billing.scene." + normalizeScene(sceneCode) + ".outputTokens";
    }

    private String sceneMinCoinKey(String sceneCode) {
        return "agent.companion.billing.scene." + normalizeScene(sceneCode) + ".minCoin";
    }

    private String normalizeScene(String sceneCode) {
        return StringUtils.defaultIfBlank(sceneCode, "default").trim();
    }
}
