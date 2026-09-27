package org.aileme.shejiao.gateway.chat.provider;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Qwen 提供商策略
 */
@Component
public class QwenProviderStrategy extends AbstractLlmProviderStrategy {

    @Override
    public String providerCode() {
        return "qwen";
    }

    @Override
    public List<String> aliases() {
        return List.of("qwen", "tongyi");
    }

    @Override
    public String defaultModel() {
        return "qwen-turbo";
    }

    @Override
    public String defaultEndpoint(Environment environment) {
        return firstNonBlank(
            environment.getProperty("aigateway.qwen.endpoint"),
            environment.getProperty("aigateway.qwen.base-url"),
            "https://dashscope.aliyuncs.com/compatible-mode/v1"
        );
    }

    @Override
    protected String resolveApiKeyFromEnvironment(Environment environment) {
        return firstNonBlank(
            environment.getProperty("aigateway.qwen.api-key"),
            environment.getProperty("DASHSCOPE_API_KEY")
        );
    }
}

