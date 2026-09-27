package org.aileme.shejiao.app.service.ai.image.strategy;

import org.aileme.shejiao.app.service.ai.image.AIImageProvider;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.util.List;
import java.util.Map;

/**
 * 图片服务商策略接口
 */
public interface ImageProviderStrategy {

    String providerCode();

    default List<String> aliases() {
        return List.of(providerCode());
    }

    AIImageProvider createProvider(ThirdPartyRouteConfigService routeConfigService);

    default AIImageProvider createProvider(ThirdPartyRouteConfigService routeConfigService,
                                           Map<String, String> providerConfigs) {
        return createProvider(routeConfigService);
    }
}
