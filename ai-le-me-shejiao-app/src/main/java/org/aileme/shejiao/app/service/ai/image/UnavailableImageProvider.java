package org.aileme.shejiao.app.service.ai.image;

import java.util.List;
import java.util.Map;

/**
 * 不可用的图片服务商占位实现
 */
public class UnavailableImageProvider implements AIImageProvider {

    private final String providerName;
    private final String message;

    public UnavailableImageProvider(String providerName, String message) {
        this.providerName = providerName;
        this.message = message;
    }

    @Override
    public List<String> generateImages(String prompt, List<String> images, Map<String, Object> params) {
        throw new IllegalStateException(message);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }
}
