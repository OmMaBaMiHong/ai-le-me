package org.aileme.shejiao.gateway.chat.provider;

import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;

/**
 * LLM 提供商策略（模板接口）
 */
public interface LlmProviderStrategy {

    /**
     * 规范化后的提供商编码
     */
    String providerCode();

    /**
     * 别名列表（用于 provider 兼容映射）
     */
    default List<String> aliases() {
        return List.of(providerCode());
    }

    /**
     * 从配置/环境中解析 API Key
     */
    String resolveApiKey(Map<String, String> configs, Environment environment);

    /**
     * 默认模型
     */
    String defaultModel();

    /**
     * 默认 endpoint（不含 /chat/completions）
     */
    String defaultEndpoint(Environment environment);
}

