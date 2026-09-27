package org.aileme.shejiao.app.service.agent;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.PlatformBusinessConfigService;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;

@Service
public class AgentUsageBillingService {

    private static final String DEFAULT_BILLING_SCENE = "reply_suggest";
    private static final String BILLING_MODE_COIN = "coin";
    private static final String BILLING_MODE_LOGIC_ONLY = "logic_only";
    private final AgentBillingPluginRegistryService pluginRegistry;
    private final PlatformBusinessConfigService businessConfigService;

    @Autowired
    public AgentUsageBillingService(AgentBillingPluginRegistryService pluginRegistry,
                                    PlatformBusinessConfigService businessConfigService) {
        this.pluginRegistry = pluginRegistry;
        this.businessConfigService = businessConfigService;
    }

    public void ensureSufficientBudget(AppUserEntity user, String sceneCode, int inputChars) {
        resolvePlugin(sceneCode).ensureSufficientBudget(user, sceneCode, inputChars);
    }

    public void settleUsage(AppUserEntity user,
                            Integer targetUid,
                            String sceneCode,
                            int inputChars,
                            String referenceId) {
        resolvePlugin(sceneCode).settleUsage(user, targetUid, sceneCode, inputChars, referenceId);
    }

    public int estimateCoin(String sceneCode, int inputChars) {
        return resolvePlugin(sceneCode).estimateCoin(sceneCode, inputChars);
    }

    public int getVipDailyFreeQuota() {
        return getVipDailyFreeQuota(DEFAULT_BILLING_SCENE);
    }

    public int getVipDailyFreeQuota(String sceneCode) {
        return resolvePlugin(sceneCode).getVipDailyFreeQuota(sceneCode);
    }

    public int getVipDailyFreeRemaining(AppUserEntity user) {
        return getVipDailyFreeRemaining(user, DEFAULT_BILLING_SCENE);
    }

    public int getVipDailyFreeRemaining(AppUserEntity user, String sceneCode) {
        return resolvePlugin(sceneCode).getVipDailyFreeRemaining(user, sceneCode);
    }

    public boolean isBillingEnabled() {
        return isBillingEnabled(DEFAULT_BILLING_SCENE);
    }

    public boolean isBillingEnabled(String sceneCode) {
        return resolvePlugin(sceneCode).isBillingEnabled(sceneCode);
    }

    public String getBillingMode(String sceneCode) {
        return resolveBillingMode(sceneCode);
    }

    private String sceneBillingModeKey(String sceneCode) {
        return "agent.companion.billing.scene." + normalizeScene(sceneCode) + ".mode";
    }

    private String normalizeScene(String sceneCode) {
        return StringUtils.defaultIfBlank(sceneCode, "default").trim();
    }

    private AgentBillingPlugin resolvePlugin(String sceneCode) {
        return pluginRegistry.resolve(resolveBillingMode(sceneCode));
    }

    private String resolveBillingMode(String sceneCode) {
        String sceneMode = businessConfigService.getString(sceneBillingModeKey(sceneCode), "");
        if (StringUtils.isNotBlank(sceneMode)) {
            return normalizeBillingMode(sceneMode);
        }
        return normalizeBillingMode(businessConfigService.getString("agent.companion.billing.mode.default", BILLING_MODE_COIN));
    }

    private String normalizeBillingMode(String rawMode) {
        String normalized = StringUtils.trimToEmpty(rawMode).toLowerCase();
        if (StringUtils.isBlank(normalized)) {
            return BILLING_MODE_COIN;
        }
        return normalized;
    }
}
