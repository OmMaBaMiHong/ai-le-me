package org.aileme.shejiao.app.service.ai.image.strategy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.app.service.ai.image.AIImageProvider;
import org.aileme.shejiao.app.service.ai.image.OpenAIImageProvider;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 图片策略
 */
@Component
public class OpenAIImageProviderStrategy extends AbstractImageProviderStrategy {

    @Override
    public String providerCode() {
        return "openai";
    }

    @Override
    public List<String> aliases() {
        return List.of("openai", "gpt_image", "gpt-image", "chatgpt_image");
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
            apiKey = StringUtils.trimToEmpty(System.getenv("OPENAI_API_KEY"));
        }
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalStateException("OpenAI 图片未配置 api_key，请先在第三方服务配置中补齐 image/openai 或设置 OPENAI_API_KEY");
        }

        OpenAIImageProvider provider = new OpenAIImageProvider();
        provider.setApiKey(apiKey);

        String endpoint = cfg(providerConfigs, "endpoint");
        if (StringUtils.isNotBlank(endpoint)) {
            provider.setEndpoint(endpoint);
        }
        String model = cfg(providerConfigs, "model");
        if (StringUtils.isNotBlank(model)) {
            provider.setModel(model);
        }
        String size = cfg(providerConfigs, "size");
        if (StringUtils.isNotBlank(size)) {
            provider.setDefaultSize(size);
        }
        String quality = cfg(providerConfigs, "quality");
        if (StringUtils.isNotBlank(quality)) {
            provider.setDefaultQuality(quality);
        }
        String background = cfg(providerConfigs, "background");
        if (StringUtils.isNotBlank(background)) {
            provider.setDefaultBackground(background);
        }
        String inputFidelity = cfg(providerConfigs, "input_fidelity");
        if (StringUtils.isNotBlank(inputFidelity)) {
            provider.setDefaultInputFidelity(inputFidelity);
        }
        String organizationId = cfg(providerConfigs, "organization_id");
        if (StringUtils.isNotBlank(organizationId)) {
            provider.setOrganizationId(organizationId);
        }
        String projectId = cfg(providerConfigs, "project_id");
        if (StringUtils.isNotBlank(projectId)) {
            provider.setProjectId(projectId);
        }

        provider.setDefaultMaxImages(intCfg(cfg(providerConfigs, "max_images"), provider.getDefaultMaxImages()));
        provider.setTimeout(intCfg(cfg(providerConfigs, "timeout"), provider.getTimeout()));
        provider.setMaxReferenceImages(intCfg(cfg(providerConfigs, "max_reference_images"), provider.getMaxReferenceImages()));
        return provider;
    }
}
