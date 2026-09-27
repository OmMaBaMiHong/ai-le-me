package org.aileme.shejiao.app.service.ai.image.strategy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.app.service.ai.image.AIImageProvider;
import org.aileme.shejiao.app.service.ai.image.DoubaoImageProvider;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.util.List;
import java.util.Map;

/**
 * 豆包图片策略
 */
@Component
public class DoubaoImageProviderStrategy extends AbstractImageProviderStrategy {

    @Override
    public String providerCode() {
        return "doubao";
    }

    @Override
    public List<String> aliases() {
        return List.of("doubao", "doubao_image", "seedream");
    }

    @Override
    public AIImageProvider createProvider(ThirdPartyRouteConfigService routeConfigService) {
        return createProvider(routeConfigService, routeConfigService.getProviderConfigs("image", providerCode()));
    }

    @Override
    public AIImageProvider createProvider(ThirdPartyRouteConfigService routeConfigService,
                                          Map<String, String> providerConfigs) {
        String apiKey = cfg(providerConfigs, "api_key");
        if (StringUtils.isBlank(apiKey)) {
            apiKey = StringUtils.trimToEmpty(routeConfigService.getConfigValue("video", "doubao", "api_key"));
        }
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalStateException("豆包图片未配置 api_key，请先在第三方服务配置中补齐 image/doubao 或 video/doubao");
        }

        DoubaoImageProvider provider = new DoubaoImageProvider();
        provider.setApiKey(apiKey);

        String endpoint = cfg(providerConfigs, "endpoint");
        if (StringUtils.isNotBlank(endpoint)) {
            provider.setEndpoint(endpoint);
        }

        String model = cfg(providerConfigs, "model");
        if (StringUtils.isNotBlank(model)) {
            provider.setModel(model);
        }
        String fallbackModel = cfg(providerConfigs, "fallback_model");
        if (StringUtils.isNotBlank(fallbackModel)) {
            provider.setFallbackModel(fallbackModel);
        }
        String fallbackModels = cfg(providerConfigs, "fallback_models");
        if (StringUtils.isNotBlank(fallbackModels)) {
            provider.setFallbackModels(fallbackModels);
        }
        String size = cfg(providerConfigs, "size");
        if (StringUtils.isNotBlank(size)) {
            provider.setDefaultSize(size);
        }
        String responseFormat = cfg(providerConfigs, "response_format");
        if (StringUtils.isNotBlank(responseFormat)) {
            provider.setDefaultResponseFormat(responseFormat);
        }
        provider.setDefaultWatermark(boolCfg(cfg(providerConfigs, "watermark"), provider.getDefaultWatermark()));
        provider.setDefaultMaxImages(intCfg(cfg(providerConfigs, "max_images"), provider.getDefaultMaxImages()));
        provider.setTimeout(intCfg(cfg(providerConfigs, "timeout"), provider.getTimeout()));
        provider.setAutoFailover(boolCfg(cfg(providerConfigs, "auto_failover"), provider.getAutoFailover()));
        provider.setCircuitFailureThreshold(intCfg(cfg(providerConfigs, "circuit_failure_threshold"), provider.getCircuitFailureThreshold()));
        provider.setCircuitOpenSeconds(intCfg(cfg(providerConfigs, "circuit_open_seconds"), provider.getCircuitOpenSeconds()));
        return provider;
    }
}
