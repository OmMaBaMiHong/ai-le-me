package org.aileme.shejiao.app.service.ai.image;

import lombok.extern.slf4j.Slf4j;
import org.aileme.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.app.service.ai.image.strategy.ImageProviderStrategy;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;
import org.aileme.shejiao.gateway.runtime.ThirdPartyResolvedRoute;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * AI 图片生成策略管理器
 */
@Slf4j
@Service
public class AIImageStrategyManager {

    private final ThirdPartyRouteConfigService routeConfigService;
    private final Map<String, ImageProviderStrategy> providerStrategyRegistry = new LinkedHashMap<>();

    @Value("${shejiao.ai-image.provider:doubao}")
    private String defaultProviderCode;

    public AIImageStrategyManager(ThirdPartyRouteConfigService routeConfigService,
                                  List<ImageProviderStrategy> providerStrategies) {
        this.routeConfigService = routeConfigService;
        if (providerStrategies != null) {
            for (ImageProviderStrategy strategy : providerStrategies) {
                registerProviderStrategy(strategy.providerCode(), strategy);
                for (String alias : strategy.aliases()) {
                    registerProviderStrategy(alias, strategy);
                }
            }
        }
    }

    public AIImageProvider getCurrentProvider() {
        return getProviderForRoute(Collections.emptyMap());
    }

    public String getCurrentProviderCode() {
        try {
            String current = routeConfigService.getCurrentProviderCode("image", defaultProviderCode);
            if (StringUtils.isNotBlank(current)) {
                return normalizeProviderCode(current);
            }
        } catch (Exception e) {
            log.warn("读取当前图片服务商失败，使用默认配置: {}", e.getMessage());
        }
        return normalizeProviderCode(StringUtils.defaultIfBlank(defaultProviderCode, "doubao"));
    }

    public List<String> generateImages(String prompt, List<String> images, Map<String, Object> params) {
        Map<String, String> routeContext = buildRouteContext(images, params);
        AIImageProvider provider = getProviderForRoute(routeContext);
        log.info("使用图片服务提供商: {} 生成图片", provider.getProviderName());
        return provider.generateImages(prompt, images, params);
    }

    public AIImageProvider getProviderByCode(String providerCode) {
        String normalized = normalizeProviderCode(StringUtils.defaultIfBlank(providerCode, getCurrentProviderCode()));
        ImageProviderStrategy strategy = providerStrategyRegistry.get(normalized);
        if (strategy == null) {
            return new UnavailableImageProvider(normalized, "未知图片服务商，请先在第三方配置中切换到已支持的 image provider");
        }
        try {
            return strategy.createProvider(routeConfigService, routeConfigService.getProviderConfigs("image", normalized));
        } catch (Exception e) {
            log.error("创建图片服务商 {} 失败", normalized, e);
            return new UnavailableImageProvider(normalized, StringUtils.defaultIfBlank(e.getMessage(), "图片生成服务暂不可用"));
        }
    }

    public AIImageProvider getProviderForRoute(Map<String, String> routeContext) {
        ThirdPartyResolvedRoute route = routeConfigService.resolveRoute("image", routeContext, defaultProviderCode);
        String normalized = normalizeProviderCode(StringUtils.defaultIfBlank(route.getProviderCode(), getCurrentProviderCode()));
        ImageProviderStrategy strategy = providerStrategyRegistry.get(normalized);
        if (strategy == null) {
            return new UnavailableImageProvider(normalized, "未知图片服务商，请先在第三方配置中切换到已支持的 image provider");
        }
        try {
            return strategy.createProvider(routeConfigService, route.getConfigs());
        } catch (Exception e) {
            log.error("按路由创建图片服务商 {} 失败", normalized, e);
            return new UnavailableImageProvider(normalized, StringUtils.defaultIfBlank(e.getMessage(), "图片生成服务暂不可用"));
        }
    }

    private Map<String, String> buildRouteContext(List<String> images, Map<String, Object> params) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("content_mode", images != null && !images.isEmpty() ? "i2i" : "t2i");
        context.put("function_type", stringParam(params, "function_type", "image_generate"));
        putIfNotBlank(context, "scene_code", stringParam(params, "scene_code", ""));
        putIfNotBlank(context, "template_code", stringParam(params, "template_code", ""));
        return context;
    }

    private void putIfNotBlank(Map<String, String> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value.trim());
        }
    }

    private String stringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params == null || !params.containsKey(key) || params.get(key) == null) {
            return defaultValue;
        }
        return String.valueOf(params.get(key));
    }

    private void registerProviderStrategy(String provider, ImageProviderStrategy strategy) {
        String normalized = normalizeProviderCode(provider);
        if (StringUtils.isBlank(normalized) || strategy == null) {
            return;
        }
        providerStrategyRegistry.put(normalized, strategy);
    }

    private String normalizeProviderCode(String providerCode) {
        return StringUtils.lowerCase(StringUtils.trimToEmpty(providerCode)).replace('-', '_');
    }

    public List<String> listRegisteredProviders() {
        return List.copyOf(new LinkedHashSet<>(providerStrategyRegistry.keySet()));
    }
}
