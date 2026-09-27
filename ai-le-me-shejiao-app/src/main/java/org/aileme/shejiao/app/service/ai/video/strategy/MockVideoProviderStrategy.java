package org.aileme.shejiao.app.service.ai.video.strategy;

import org.springframework.stereotype.Component;
import org.aileme.shejiao.app.service.ai.video.AIVideoProvider;
import org.aileme.shejiao.app.service.ai.video.MockVideoProvider;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.util.List;

/**
 * Mock 视频策略
 */
@Component
public class MockVideoProviderStrategy extends AbstractVideoProviderStrategy {

    @Override
    public String providerCode() {
        return "mock";
    }

    @Override
    public List<String> aliases() {
        return List.of("mock");
    }

    @Override
    public AIVideoProvider createProvider(ThirdPartyRouteConfigService routeConfigService) {
        return new MockVideoProvider();
    }
}

