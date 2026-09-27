package org.aileme.shejiao.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 网关配置
 */
@ConfigurationProperties(prefix = "aigateway")
public class AiGatewayProperties {

    /**
     * 默认AI提供商（当数据库未配置或配置为mock时生效）
     */
    private String defaultAiProvider = "doubao";

    /**
     * 请求超时秒数
     */
    private int timeoutSeconds = 30;

    /**
     * 候补提供商
     */
    private List<String> fallbackProviders = new ArrayList<>(List.of("qwen", "deepseek", "openai_codex", "mock"));

    public String getDefaultAiProvider() {
        return defaultAiProvider;
    }

    public void setDefaultAiProvider(String defaultAiProvider) {
        this.defaultAiProvider = defaultAiProvider;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public List<String> getFallbackProviders() {
        return fallbackProviders;
    }

    public void setFallbackProviders(List<String> fallbackProviders) {
        this.fallbackProviders = fallbackProviders;
    }
}
