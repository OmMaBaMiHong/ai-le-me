package org.aileme.shejiao.app.service.ai.video.strategy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.app.service.ai.video.AIVideoProvider;
import org.aileme.shejiao.app.service.ai.video.JimengVideoProvider;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.util.List;
import java.util.Map;

/**
 * 即梦视频策略
 */
@Component
public class JimengVideoProviderStrategy extends AbstractVideoProviderStrategy {

    @Override
    public String providerCode() {
        return "jimeng";
    }

    @Override
    public List<String> aliases() {
        return List.of("jimeng");
    }

    @Override
    public AIVideoProvider createProvider(ThirdPartyRouteConfigService routeConfigService) {
        return createProvider(routeConfigService, routeConfigService.getProviderConfigs("video", providerCode()));
    }

    @Override
    public AIVideoProvider createProvider(ThirdPartyRouteConfigService routeConfigService,
                                          Map<String, String> providerConfigs) {
        String accessKeyId = cfg(providerConfigs, "access_key_id");
        String secretAccessKey = cfg(providerConfigs, "secret_access_key");
        String apiKey = cfg(providerConfigs, "api_key");
        if (StringUtils.isBlank(accessKeyId) && StringUtils.isBlank(apiKey)) {
            throw new IllegalStateException("即梦视频未配置 access_key_id/api_key，请先在第三方服务配置中补齐 video/jimeng");
        }

        JimengVideoProvider provider = new JimengVideoProvider();
        if (StringUtils.isNotBlank(accessKeyId)) {
            provider.setAccessKeyId(accessKeyId);
            provider.setSecretAccessKey(secretAccessKey);
        }
        if (StringUtils.isNotBlank(apiKey)) {
            provider.setApiKey(apiKey);
        }
        String endpoint = cfg(providerConfigs, "endpoint");
        if (StringUtils.isNotBlank(endpoint)) {
            provider.setEndpoint(endpoint);
        }
        provider.setTimeout(intCfg(cfg(providerConfigs, "timeout"), provider.getTimeout()));
        return provider;
    }
}
