package org.aileme.shejiao.app.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.aileme.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.util.Map;

/**
 * AI画像生成策略管理器
 * 根据配置动态选择AI提供商
 *
 * @author system
 * @date 2026-02-14
 */
@Slf4j
@Service
public class AIPersonaStrategyManager {

    @Autowired(required = false)
    private AIPersonaProvider mockPersonaProvider;

    @Autowired
    private ThirdPartyRouteConfigService routeConfigService;

    /**
     * 获取当前AI提供商（从数据库动态读取配置）
     *
     * @return AI提供商实例
     */
    public AIPersonaProvider getCurrentProvider() {
        return getProviderByCode(getCurrentProviderCode());
    }

    /**
     * 获取当前AI提供商编码
     */
    public String getCurrentProviderCode() {
        try {
            String currentProvider = routeConfigService.getCurrentProviderCode("ai", "mock");
            if (StringUtils.isNotBlank(currentProvider)) {
                return currentProvider.toLowerCase();
            }
        } catch (Exception e) {
            log.warn("读取当前AI提供商失败: {}", e.getMessage());
        }

        return "mock";
    }

    /**
     * 根据服务商编码创建Provider
     */
    public AIPersonaProvider getProviderByCode(String providerCode) {
        String normalized = StringUtils.defaultIfBlank(providerCode, "mock").toLowerCase();
        switch (normalized) {
            case "tongyi":
                return createTongyiProvider();
            case "zhipu":
                return createZhipuProvider();
            case "mock":
                return safeMockProvider();
            default:
                log.warn("未知AI提供商: {}, 使用Mock", normalized);
                return safeMockProvider();
        }
    }

    /**
     * 动态创建通义千问Provider
     */
    private AIPersonaProvider createTongyiProvider() {
        String apiKey = routeConfigService.getConfigValue("ai", "tongyi", "api_key");
        String model = routeConfigService.getConfigValue("ai", "tongyi", "model");
        
        if (StringUtils.isBlank(apiKey)) {
            log.warn("通义千问API Key未配置，降级到Mock");
            return safeMockProvider();
        }

        TongyiPersonaProvider provider = new TongyiPersonaProvider();
        provider.setApiKey(apiKey);
        provider.setModel(StringUtils.isNotBlank(model) ? model : "qwen-turbo");
        return provider;
    }

    /**
     * 动态创建智谱GLM Provider
     */
    private AIPersonaProvider createZhipuProvider() {
        String apiKey = routeConfigService.getConfigValue("ai", "zhipu", "api_key");
        String model = routeConfigService.getConfigValue("ai", "zhipu", "model");
        
        if (StringUtils.isBlank(apiKey)) {
            log.warn("智谱GLM API Key未配置，降级到Mock");
            return safeMockProvider();
        }

        ZhipuPersonaProvider provider = new ZhipuPersonaProvider();
        provider.setApiKey(apiKey);
        provider.setModel(StringUtils.isNotBlank(model) ? model : "glm-4");
        return provider;
    }

    /**
     * 生成画像(自动选择提供商)
     *
     * @param features 用户特征
     * @return 画像JSON
     */
    public String generatePersona(Map<String, Object> features) {
        AIPersonaProvider provider = getCurrentProvider();
        log.info("使用AI提供商: {} 生成画像", provider.getProviderName());
        return provider.generatePersona(features);
    }

    private AIPersonaProvider safeMockProvider() {
        return mockPersonaProvider != null ? mockPersonaProvider : new MockPersonaProvider();
    }
}
