package org.aileme.shejiao.app.service.ai;

import java.util.Map;

/**
 * AI人物画像生成服务接口
 * 
 * @author system
 * @date 2026-02-14
 */
public interface AIPersonaProvider {

    /**
     * 生成人物画像
     *
     * @param features 用户特征数据
     * @return 画像JSON字符串
     */
    String generatePersona(Map<String, Object> features);

    /**
     * 获取提供商名称
     *
     * @return 提供商标识
     */
    String getProviderName();
}
