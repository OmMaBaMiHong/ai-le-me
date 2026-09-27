package org.aileme.shejiao.gateway.chat.provider;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 豆包 / 火山方舟提供商策略
 */
@Component
public class DoubaoProviderStrategy extends AbstractLlmProviderStrategy {

    @Override
    public String providerCode() {
        return "doubao";
    }

    @Override
    public List<String> aliases() {
        return List.of("doubao", "ark", "volcengine", "huoshan");
    }

    @Override
    public String defaultModel() {
        return "ep-20260323145008-tbqqd";
    }

    @Override
    public String defaultEndpoint(Environment environment) {
        return firstNonBlank(
            environment.getProperty("aigateway.doubao.endpoint"),
            environment.getProperty("aigateway.doubao.base-url"),
            "https://ark.cn-beijing.volces.com/api/v3"
        );
    }

    @Override
    protected String resolveApiKeyFromEnvironment(Environment environment) {
        return firstNonBlank(
            environment.getProperty("aigateway.doubao.api-key"),
            environment.getProperty("ARK_API_KEY"),
            environment.getProperty("VOLCENGINE_ARK_API_KEY")
        );
    }
}
