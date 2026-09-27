package org.aileme.shejiao.app.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Mock AI画像生成实现(用于开发测试)
 *
 * @author system
 * @date 2026-02-14
 */
@Slf4j
@Service("mockPersonaProvider")
public class MockPersonaProvider implements AIPersonaProvider {

    @Override
    public String generatePersona(Map<String, Object> features) {
        log.info("Mock生成画像(测试模式): features={}", features);

        // 从特征中提取一些信息做个性化mock
        String gender = "TA";
        if (features.containsKey("basic")) {
            Map<String, Object> basic = (Map<String, Object>) features.get("basic");
            gender = basic.getOrDefault("gender", "TA").toString();
        }

        return "{"
                + "\"summary\":\"" + gender + "是一个认真且有趣的人，值得深入了解\","
                + "\"personality\":{\"introvert_extrovert\":\"偏内向但愿意社交\",\"stability\":\"情绪比较稳定\",\"openness\":\"对新事物持开放态度\"},"
                + "\"love_style\":{\"attitude\":\"认真、慢热、重视长期关系\",\"risk_points\":[\"有时会想太多\",\"表达上可能不够直接\"]},"
                + "\"social_style\":{\"online\":\"擅长文字表达\",\"offline\":\"初次见面略拘谨但熟悉后很自在\"},"
                + "\"tags_highlight\":[\"真诚靠谱\",\"认真对待感情\",\"有责任感\"],"
                + "\"suggestions\":[\"多分享真实想法，减少误会\",\"线下活动是认识新朋友的好方式\",\"保持耐心，好的关系需要时间培养\"]"
                + "}";
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}
