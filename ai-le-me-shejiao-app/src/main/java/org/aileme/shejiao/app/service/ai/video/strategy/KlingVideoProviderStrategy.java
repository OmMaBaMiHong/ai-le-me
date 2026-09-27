package org.aileme.shejiao.app.service.ai.video.strategy;

import org.springframework.stereotype.Component;
import org.aileme.shejiao.app.service.ai.video.AIVideoProvider;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.util.List;

/**
 * 可灵视频策略（预留）
 */
@Component
public class KlingVideoProviderStrategy extends AbstractVideoProviderStrategy {

    @Override
    public String providerCode() {
        return "kling";
    }

    @Override
    public List<String> aliases() {
        return List.of("kling");
    }

    @Override
    public AIVideoProvider createProvider(ThirdPartyRouteConfigService routeConfigService) {
        throw new IllegalStateException("可灵视频 Provider 还未接入完成，请先切换到 doubao 或 jimeng");
    }
}

