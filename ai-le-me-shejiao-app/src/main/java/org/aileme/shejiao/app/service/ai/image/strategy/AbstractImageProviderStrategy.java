package org.aileme.shejiao.app.service.ai.image.strategy;

import org.apache.commons.lang3.StringUtils;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.util.Map;

/**
 * 图片服务商策略抽象模板
 */
public abstract class AbstractImageProviderStrategy implements ImageProviderStrategy {

    protected String cfg(ThirdPartyRouteConfigService routeConfigService, String provider, String key) {
        return StringUtils.trimToEmpty(routeConfigService.getConfigValue("image", provider, key));
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

    protected Boolean boolCfg(String value, Boolean defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        if ("1".equals(value) || "true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("0".equals(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return defaultValue;
    }
}
