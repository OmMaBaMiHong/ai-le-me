package org.aileme.shejiao.app.service.ai.video.strategy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.app.service.ai.video.AIVideoProvider;
import org.aileme.shejiao.app.service.ai.video.DoubaoVideoProvider;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.util.List;
import java.util.Map;

/**
 * 豆包视频策略
 */
@Component
public class DoubaoVideoProviderStrategy extends AbstractVideoProviderStrategy {

    @Override
    public String providerCode() {
        return "doubao";
    }

    @Override
    public List<String> aliases() {
        return List.of("doubao");
    }

    @Override
    public AIVideoProvider createProvider(ThirdPartyRouteConfigService routeConfigService) {
        return createProvider(routeConfigService, routeConfigService.getProviderConfigs("video", providerCode()));
    }

    @Override
    public AIVideoProvider createProvider(ThirdPartyRouteConfigService routeConfigService,
                                          Map<String, String> providerConfigs) {
        String apiKey = cfg(providerConfigs, "api_key");
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalStateException("豆包视频未配置 api_key，请先在第三方服务配置中补齐 video/doubao");
        }

        DoubaoVideoProvider provider = new DoubaoVideoProvider();
        provider.setApiKey(apiKey);

        String endpoint = cfg(providerConfigs, "endpoint");
        if (StringUtils.isNotBlank(endpoint)) {
            provider.setEndpoint(endpoint);
        }
        String model = cfg(providerConfigs, "model");
        if (StringUtils.isNotBlank(model)) {
            provider.setModel(model);
        }
        provider.setTimeout(intCfg(cfg(providerConfigs, "timeout"), provider.getTimeout()));
        return provider;
    }
}
