package org.aileme.shejiao.gateway.chat.provider;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OpenAI GPT-5.4 场景专用策略。
 *
 * <p>用于需要固定优先命中 gpt-5.4 的内容工厂场景；若上游网关或代理不支持该模型，
 * 仍会由聊天网关继续走其它 fallback provider。
 */
@Component
public class OpenAiGpt54ProviderStrategy extends AbstractLlmProviderStrategy {

    @Override
    public String providerCode() {
        return "openai_gpt54";
    }

    @Override
    public List<String> aliases() {
        return List.of("openai_gpt54", "openai-gpt54", "gpt54", "gpt-5.4");
    }

    @Override
    public String defaultModel() {
        return "gpt-5.4";
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
