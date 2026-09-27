package org.aileme.shejiao.app.service.ai.video;

import java.util.List;
import java.util.Map;

/**
 * 不可用的视频服务商占位实现。
 * 用于在未配置完成时给出明确错误，而不是悄悄回退到 mock。
 */
public class UnavailableVideoProvider implements AIVideoProvider {

    private final String providerName;
    private final String message;

    public UnavailableVideoProvider(String providerName, String message) {
        this.providerName = providerName;
        this.message = message;
    }

    @Override
    public String createVideoTask(String prompt, List<String> images, Map<String, Object> params) {
        throw new IllegalStateException(message);
    }

    @Override
    public VideoTaskStatus queryTaskStatus(String taskId) {
        throw new IllegalStateException(message);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }
}
