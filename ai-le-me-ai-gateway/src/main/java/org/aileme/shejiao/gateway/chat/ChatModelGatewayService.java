package org.aileme.shejiao.gateway.chat;

/**
 * 聊天模型网关
 */
public interface ChatModelGatewayService {

    /**
     * 获取当前路由提供商
     */
    String currentProviderCode();

    /**
     * 获取模型回复文本
     */
    String complete(String scene, String systemPrompt, String userPrompt, Integer maxTokens, Double temperature);

    /**
     * 获取模型回复文本，并允许调用方为特定 scene 指定首选兜底 provider。
     */
    default String complete(String scene,
                            String systemPrompt,
                            String userPrompt,
                            Integer maxTokens,
                            Double temperature,
                            String fallbackProvider) {
        return complete(scene, systemPrompt, userPrompt, maxTokens, temperature);
    }

    /**
     * 获取模型回复文本，并允许调用方指定首选 provider 与 model。
     */
    default String complete(String scene,
                            String systemPrompt,
                            String userPrompt,
                            Integer maxTokens,
                            Double temperature,
                            String fallbackProvider,
                            String preferredModel) {
        return complete(scene, systemPrompt, userPrompt, maxTokens, temperature, fallbackProvider);
    }
}
