package org.aileme.shejiao.app.service.ai.video.strategy;

import org.aileme.shejiao.app.service.ai.video.AIVideoProvider;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.util.List;
import java.util.Map;

/**
 * 视频服务商策略（模板接口）
 */
public interface VideoProviderStrategy {

    /**
     * 规范化后的服务商编码
     */
    String providerCode();

    /**
     * 别名列表（用于兼容 provider 映射）
     */
    default List<String> aliases() {
        return List.of(providerCode());
    }

    /**
     * 基于运行时配置创建 provider 实例
     */
    AIVideoProvider createProvider(ThirdPartyRouteConfigService routeConfigService);

    /**
     * 基于指定 profile 的运行时配置创建 provider 实例
     */
    default AIVideoProvider createProvider(ThirdPartyRouteConfigService routeConfigService,
                                           Map<String, String> providerConfigs) {
        return createProvider(routeConfigService);
    }
}
