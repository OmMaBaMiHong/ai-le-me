package org.aileme.shejiao.app.service.ai.video.strategy;

import org.apache.commons.lang3.StringUtils;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.util.Map;

/**
 * 视频服务商策略抽象模板
 */
public abstract class AbstractVideoProviderStrategy implements VideoProviderStrategy {

    protected String cfg(ThirdPartyRouteConfigService routeConfigService, String provider, String key) {
        return StringUtils.trimToEmpty(routeConfigService.getConfigValue("video", provider, key));
    }

    protected String cfg(Map<String, String> providerConfigs, String key) {
        if (providerConfigs == null || providerConfigs.isEmpty()) {
            return "";
        }
        return StringUtils.trimToEmpty(providerConfigs.get(key));
    }

    protected Integer intCfg(String value, Integer defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
