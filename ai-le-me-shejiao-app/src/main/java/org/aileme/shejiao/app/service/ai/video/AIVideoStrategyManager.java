package org.aileme.shejiao.app.service.ai.video;

import lombok.extern.slf4j.Slf4j;
import org.aileme.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.app.service.ai.video.strategy.VideoProviderStrategy;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;
import org.aileme.shejiao.gateway.runtime.ThirdPartyResolvedRoute;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * AI视频生成策略管理器
 * 根据配置动态选择视频服务提供商
 *
 * @author system
 * @date 2026-03-01
 */
@Slf4j
@Service
public class AIVideoStrategyManager {

    private static final String ROUTE_KEY_SEPARATOR = "#";

    private final ThirdPartyRouteConfigService routeConfigService;
    private final Map<String, VideoProviderStrategy> providerStrategyRegistry = new LinkedHashMap<>();
    private static final VideoProviderStrategy FALLBACK_MOCK_STRATEGY = new VideoProviderStrategy() {
        @Override
        public String providerCode() {
            return "mock";
        }

        @Override
        public AIVideoProvider createProvider(ThirdPartyRouteConfigService routeConfigService) {
            return new MockVideoProvider();
        }
    };

    @Value("${shejiao.ai-video.provider:doubao}")
    private String defaultProviderCode;

    @Value("${shejiao.ai-video.allow-mock-fallback:false}")
    private boolean allowMockFallback;

    public AIVideoStrategyManager(ThirdPartyRouteConfigService routeConfigService,
                                  List<VideoProviderStrategy> providerStrategies) {
        this.routeConfigService = routeConfigService;
        if (providerStrategies != null) {
            for (VideoProviderStrategy strategy : providerStrategies) {
                registerProviderStrategy(strategy.providerCode(), strategy);
                List<String> aliases = strategy.aliases();
                if (aliases != null) {
                    for (String alias : aliases) {
                        registerProviderStrategy(alias, strategy);
                    }
                }
            }
        }
    }

    /**
     * 获取当前视频服务提供商(从数据库动态读取配置)
     *
     * @return 视频服务提供商实例
     */
    public AIVideoProvider getCurrentProvider() {
        return getProviderForRoute(Collections.emptyMap());
    }

    /**
     * 创建视频生成任务
     *
     * @param prompt 提示词
     * @param images 素材图片
     * @param params 额外参数
     * @return 任务ID
     */
    public String createVideoTask(String prompt, List<String> images, Map<String, Object> params) {
        AIVideoProvider provider = getCurrentProvider();
        log.info("使用视频服务提供商: {} 创建任务", provider.getProviderName());
        return provider.createVideoTask(prompt, images, params);
    }

    /**
     * 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    public AIVideoProvider.VideoTaskStatus queryTaskStatus(String taskId) {
        AIVideoProvider provider = getCurrentProvider();
        return provider.queryTaskStatus(taskId);
    }

    /**
     * 根据指定提供商查询任务状态（避免任务创建后切换配置导致查错服务商）
     */
    public AIVideoProvider.VideoTaskStatus queryTaskStatus(String taskId, String providerCode) {
        AIVideoProvider provider = getProviderByRouteKey(providerCode);
        return provider.queryTaskStatus(taskId);
    }

    /**
     * 获取当前提供商编码
     */
    public String getCurrentProviderCode() {
        return resolveRoute(Collections.emptyMap()).getProviderCode();
    }

    public ThirdPartyResolvedRoute resolveRoute(Map<String, String> routeContext) {
        try {
            ThirdPartyResolvedRoute route = routeConfigService.resolveRoute("video", routeContext, defaultProviderCode);
            String current = normalizeProviderCode(route.getProviderCode());
            if (StringUtils.isNotBlank(current)) {
                route.setProviderCode(current);
                route.setProfileCode(normalizeProfileCode(route.getProfileCode()));
                return route;
            }
        } catch (Exception e) {
            log.warn("读取当前视频服务商失败: {}", e.getMessage());
        }

        ThirdPartyResolvedRoute route = new ThirdPartyResolvedRoute();
        route.setServiceType("video");
        route.setProviderCode(normalizeProviderCode(StringUtils.defaultIfBlank(defaultProviderCode, "doubao")));
        route.getConfigs().putAll(routeConfigService.getProviderConfigs("video", route.getProviderCode()));
        route.setProfileCode(normalizeProfileCode(route.getConfigs().get("profile_code")));
        return route;
    }

    /**
     * 根据服务商编码创建Provider
     */
    public AIVideoProvider getProviderByCode(String providerCode) {
        String normalized = normalizeProviderCode(StringUtils.defaultIfBlank(providerCode, getCurrentProviderCode()));
        ThirdPartyResolvedRoute route = new ThirdPartyResolvedRoute();
        route.setServiceType("video");
        route.setProviderCode(normalized);
        route.getConfigs().putAll(routeConfigService.getProviderConfigs("video", normalized));
        route.setProfileCode(normalizeProfileCode(route.getConfigs().get("profile_code")));
        return getProviderByResolvedRoute(route);
    }

    public AIVideoProvider getProviderForRoute(Map<String, String> routeContext) {
        return getProviderByResolvedRoute(resolveRoute(routeContext));
    }

    public AIVideoProvider getProviderForRoute(ThirdPartyResolvedRoute route) {
        return getProviderByResolvedRoute(route);
    }

    public String buildRouteKey(ThirdPartyResolvedRoute route) {
        if (route == null) {
            return "";
        }
        String providerCode = normalizeProviderCode(route.getProviderCode());
        String profileCode = normalizeProfileCode(route.getProfileCode());
        if (StringUtils.isBlank(profileCode)) {
            return providerCode;
        }
        return providerCode + ROUTE_KEY_SEPARATOR + profileCode;
    }

    private AIVideoProvider getProviderByRouteKey(String routeKey) {
        ThirdPartyResolvedRoute route = new ThirdPartyResolvedRoute();
        route.setServiceType("video");
        route.setProviderCode(getProviderCodeFromRouteKey(routeKey));
        route.setProfileCode(getProfileCodeFromRouteKey(routeKey));
        route.getConfigs().putAll(routeConfigService.getProviderConfigs("video", route.getProviderCode(), route.getProfileCode()));
        if (StringUtils.isBlank(route.getProfileCode())) {
            route.setProfileCode(normalizeProfileCode(route.getConfigs().get("profile_code")));
        }
        return getProviderByResolvedRoute(route);
    }

    private AIVideoProvider getProviderByResolvedRoute(ThirdPartyResolvedRoute route) {
        String normalized = normalizeProviderCode(StringUtils.defaultIfBlank(route.getProviderCode(), getCurrentProviderCode()));
        VideoProviderStrategy strategy = providerStrategyRegistry.get(normalized);
        if (strategy == null) {
            String message = String.format("未知视频服务商 %s，请先在第三方配置中切换到已支持的 provider", normalized);
            log.warn(message);
            return unavailableOrMock(normalized, message);
        }

        try {
            return strategy.createProvider(routeConfigService, route.getConfigs());
        } catch (Exception e) {
            log.error("创建视频服务商 {} 失败", normalized, e);
            return unavailableOrMock(normalized, e.getMessage());
        }
    }

    private AIVideoProvider unavailableOrMock(String providerCode, String message) {
        if (allowMockFallback) {
            log.warn("视频服务商 {} 不可用，已按配置回退到 mock: {}", providerCode, message);
            return createMockProvider();
        }
        return new UnavailableVideoProvider(providerCode, StringUtils.defaultIfBlank(message, "视频服务暂不可用"));
    }

    private AIVideoProvider createMockProvider() {
        try {
            VideoProviderStrategy mockStrategy = providerStrategyRegistry.get("mock");
            if (mockStrategy == null) {
                mockStrategy = FALLBACK_MOCK_STRATEGY;
            }
            return mockStrategy.createProvider(routeConfigService);
        } catch (Exception ex) {
            log.warn("创建 mock 视频服务商失败，降级使用内置 mock: {}", ex.getMessage());
            return new MockVideoProvider();
        }
    }

    private void registerProviderStrategy(String provider, VideoProviderStrategy strategy) {
        String normalized = normalizeProviderCode(provider);
        if (StringUtils.isBlank(normalized) || strategy == null) {
            return;
        }
        providerStrategyRegistry.put(normalized, strategy);
    }

    private String normalizeProviderCode(String providerCode) {
        return StringUtils.lowerCase(StringUtils.trimToEmpty(providerCode)).replace('-', '_');
    }

    private String normalizeProfileCode(String profileCode) {
        return StringUtils.lowerCase(StringUtils.trimToEmpty(profileCode)).replace('-', '_');
    }

    private String getProviderCodeFromRouteKey(String routeKey) {
        String candidate = StringUtils.defaultIfBlank(routeKey, getCurrentProviderCode());
        int idx = candidate.indexOf(ROUTE_KEY_SEPARATOR);
        if (idx < 0) {
            return normalizeProviderCode(candidate);
        }
        return normalizeProviderCode(candidate.substring(0, idx));
    }

    private String getProfileCodeFromRouteKey(String routeKey) {
        String candidate = StringUtils.trimToEmpty(routeKey);
        int idx = candidate.indexOf(ROUTE_KEY_SEPARATOR);
        if (idx < 0 || idx == candidate.length() - 1) {
            return "";
        }
        return normalizeProfileCode(candidate.substring(idx + 1));
    }

    /**
     * 供调试或运维页查看当前已注册 provider
     */
    public List<String> listRegisteredProviders() {
        return List.copyOf(new LinkedHashSet<>(providerStrategyRegistry.keySet()));
    }
}
