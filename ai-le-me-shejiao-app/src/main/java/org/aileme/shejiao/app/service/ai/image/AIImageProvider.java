package org.aileme.shejiao.app.service.ai.image;

import java.util.List;
import java.util.Map;

/**
 * AI 图片生成服务接口
 */
public interface AIImageProvider {

    /**
     * 生成图片
     *
     * @param prompt   提示词
     * @param images   参考图 URL 列表，可为空
     * @param params   额外参数
     * @return 生成后的图片 URL 列表
     */
    List<String> generateImages(String prompt, List<String> images, Map<String, Object> params);

    /**
     * 获取提供商名称
     */
    String getProviderName();
}
