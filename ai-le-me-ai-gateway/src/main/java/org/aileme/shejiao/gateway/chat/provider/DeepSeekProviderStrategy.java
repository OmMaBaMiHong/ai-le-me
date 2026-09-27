package org.aileme.shejiao.gateway.chat.provider;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DeepSeek 提供商策略
 */
@Component
public class DeepSeekProviderStrategy extends AbstractLlmProviderStrategy {

    @Override
    public String providerCode() {
        return "deepseek";
    }

    @Override
    public List<String> aliases() {
        return List.of("deepseek");
    }

    @Override
    public String defaultModel() {
        return "deepseek-chat";
    }

    @Override
    public String defaultEndpoint(Environment environment) {
        return firstNonBlank(
            environment.getProperty("aigateway.deepseek.endpoint"),
            environment.getProperty("aigateway.deepseek.base-url"),
            "https://api.deepseek.com"
        );
    }

    @Override
    protected String resolveApiKeyFromEnvironment(Environment environment) {
        return firstNonBlank(
            environment.getProperty("aigateway.deepseek.api-key"),
            environment.getProperty("DEEPSEEK_API_KEY")
        );
    }
}

