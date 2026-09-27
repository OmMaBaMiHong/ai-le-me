package org.aileme.shejiao.gateway.chat.provider;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OpenAI Codex 提供商策略
 */
@Component
public class OpenAiCodexProviderStrategy extends AbstractLlmProviderStrategy {

    @Override
    public String providerCode() {
        return "openai_codex";
    }

    @Override
    public List<String> aliases() {
        return List.of("openai_codex", "openai-codex", "codex", "openai");
    }

    @Override
    public String defaultModel() {
        return "gpt-5-codex";
    }

    @Override
    public String defaultEndpoint(Environment environment) {
        return firstNonBlank(
            environment.getProperty("aigateway.openai.endpoint"),
            environment.getProperty("aigateway.openai.base-url"),
            "https://api.openai.com/v1"
        );
    }

    @Override
    protected String resolveApiKeyFromEnvironment(Environment environment) {
        return firstNonBlank(
            environment.getProperty("aigateway.openai.api-key"),
            environment.getProperty("OPENAI_API_KEY")
        );
    }
}

