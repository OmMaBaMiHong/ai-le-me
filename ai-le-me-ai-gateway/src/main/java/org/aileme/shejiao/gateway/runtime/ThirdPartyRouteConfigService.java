package org.aileme.shejiao.gateway.runtime;

import java.util.Map;

/**
 * 运行时第三方路由配置读取
 *
 * <p>用于 AI/视频链路运行时读取，避免业务代码直接依赖 system 模块服务实现。
 */
public interface ThirdPartyRouteConfigService {

    /**
     * 获取当前服务类型的启用提供商
     */
    String getCurrentProviderCode(String serviceType, String fallbackCode);

    /**
     * 获取服务商配置
     */
    Map<String, String> getProviderConfigs(String serviceType, String providerCode);

    /**
     * 获取指定服务商配置项
     */
    String getConfigValue(String serviceType, String providerCode, String configKey);

    /**
     * 获取当前服务商配置项
     */
    String getConfigValueForCurrentProvider(String serviceType, String configKey, String fallbackProvider);

    /**
     * 获取指定服务商配置项（支持指定 profile）
     */
    default Map<String, String> getProviderConfigs(String serviceType, String providerCode, String profileCode) {
        return getProviderConfigs(serviceType, providerCode);
    }

    /**
     * 解析当前请求命中的服务商与 profile
     */
    default ThirdPartyResolvedRoute resolveRoute(String serviceType,
                                                 Map<String, String> routeContext,
                                                 String fallbackProvider) {
        ThirdPartyResolvedRoute route = new ThirdPartyResolvedRoute();
        route.setServiceType(serviceType);
        route.setProviderCode(getCurrentProviderCode(serviceType, fallbackProvider));
        route.getConfigs().putAll(getProviderConfigs(serviceType, route.getProviderCode()));
        return route;
    }
}
