package org.aileme.shejiao.gateway.chat.provider;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * LLM 提供商策略抽象模板
 */
public abstract class AbstractLlmProviderStrategy implements LlmProviderStrategy {

    @Override
    public String resolveApiKey(Map<String, String> configs, Environment environment) {
        String apiKey = firstNonBlank(
            configs.get("api_key"),
            configs.get("apiKey"),
            configs.get("apikey"),
            configs.get("key"),
            configs.get("token")
        );
        if (StringUtils.isNotBlank(apiKey)) {
            return apiKey;
        }
        return resolveApiKeyFromEnvironment(environment);
    }

    protected abstract String resolveApiKeyFromEnvironment(Environment environment);

    protected String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }
}

