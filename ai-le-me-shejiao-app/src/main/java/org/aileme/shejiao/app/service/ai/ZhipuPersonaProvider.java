package org.aileme.shejiao.app.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 智谱GLM AI画像生成实现
 *
 * @author system
 * @date 2026-02-14
 */
@Slf4j
@Service("zhipuPersonaProvider")
public class ZhipuPersonaProvider implements AIPersonaProvider {

    private String apiKey;
    private String model = "glm-4";

    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Setter methods for dynamic configuration
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String generatePersona(Map<String, Object> features) {
        log.info("智谱GLM生成画像: features={}", features);

        try {
            // 构建请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", buildSystemPrompt());
            messages.add(systemMsg);
            
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", buildUserPrompt(features));
            messages.add(userMsg);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");
                
                log.info("智谱GLM返回画像: {}", content);
                return content;
            } else {
                log.error("智谱GLM API调用失败: status={}, body={}", response.getStatusCode(), response.getBody());
                return buildFallbackPersona(features);
            }
        } catch (Exception e) {
            log.error("智谱GLM生成画像异常", e);
            return buildFallbackPersona(features);
        }
    }

    @Override
    public String getProviderName() {
        return "zhipu";
    }

    private String buildSystemPrompt() {
        return "你是一位专业的情感和社交分析师，擅长从用户资料、标签、内容等多维度信息中，分析出用户的人格特质、情感态度、社交风格等。\n\n"
                + "你需要根据用户提供的信息，生成一份结构化的人物画像JSON，必须严格按照以下格式输出，不要有任何额外文字：\n\n"
                + "{\n"
                + "  \"summary\": \"一句话总结这个人的核心特质\",\n"
                + "  \"personality\": {\n"
                + "    \"introvert_extrovert\": \"内向/外向倾向描述\",\n"
                + "    \"stability\": \"情绪稳定性描述\",\n"
                + "    \"openness\": \"对新事物的开放程度\"\n"
                + "  },\n"
                + "  \"love_style\": {\n"
                + "    \"attitude\": \"对待感情的态度\",\n"
                + "    \"risk_points\": [\"需要注意的风险点1\", \"风险点2\"]\n"
                + "  },\n"
                + "  \"social_style\": {\n"
                + "    \"online\": \"线上社交风格\",\n"
                + "    \"offline\": \"线下社交特点\"\n"
                + "  },\n"
                + "  \"tags_highlight\": [\"关键标签1\", \"关键标签2\", \"关键标签3\"],\n"
                + "  \"suggestions\": [\"给用户的建议1\", \"建议2\"]\n"
                + "}\n\n"
                + "注意：分析要客观、温和，避免负面标签化，必须严格输出JSON格式";
    }

    private String buildUserPrompt(Map<String, Object> features) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下用户生成人物画像分析：\n\n");

        if (features.containsKey("basic")) {
            Map<String, Object> basic = (Map<String, Object>) features.get("basic");
            prompt.append("【基础信息】\n");
            prompt.append("性别：").append(basic.getOrDefault("gender", "未知")).append("\n");
            prompt.append("年龄：").append(basic.getOrDefault("age", "未知")).append("\n");
            prompt.append("城市：").append(basic.getOrDefault("city", "未知")).append("\n");
            prompt.append("职业：").append(basic.getOrDefault("job", "未知")).append("\n\n");
        }

        if (features.containsKey("tags")) {
            Map<String, Object> tags = (Map<String, Object>) features.get("tags");
            if (tags.containsKey("selfTags")) {
                List<String> selfTags = (List<String>) tags.get("selfTags");
                if (!selfTags.isEmpty()) {
                    prompt.append("【自我标签】\n");
                    prompt.append(String.join("、", selfTags)).append("\n\n");
                }
            }
        }

        if (features.containsKey("content")) {
            Map<String, Object> content = (Map<String, Object>) features.get("content");
            if (content.get("selfIntro") != null && !content.get("selfIntro").toString().isEmpty()) {
                prompt.append("【自我介绍】\n").append(content.get("selfIntro")).append("\n\n");
            }
            if (content.get("interest") != null && !content.get("interest").toString().isEmpty()) {
                prompt.append("【兴趣爱好】\n").append(content.get("interest")).append("\n\n");
            }
            if (content.get("loveDeclaration") != null && !content.get("loveDeclaration").toString().isEmpty()) {
                prompt.append("【爱情观】\n").append(content.get("loveDeclaration")).append("\n\n");
            }
        }

        prompt.append("请基于以上信息，生成结构化的人物画像JSON。");
        return prompt.toString();
    }

    private String buildFallbackPersona(Map<String, Object> features) {
        return "{"
                + "\"summary\":\"一个真诚且期待遇见对的人的单身用户\","
                + "\"personality\":{\"introvert_extrovert\":\"性格温和\",\"stability\":\"情绪稳定\",\"openness\":\"对新事物保持开放\"},"
                + "\"love_style\":{\"attitude\":\"认真对待感情\",\"risk_points\":[\"建议多表达真实想法\",\"可以更主动一些\"]},"
                + "\"social_style\":{\"online\":\"擅长文字沟通\",\"offline\":\"见面需要时间熟悉\"},"
                + "\"tags_highlight\":[\"真诚\",\"靠谱\",\"期待长期关系\"],"
                + "\"suggestions\":[\"多参加线下社交活动\",\"在聊天中展现真实的自己\"]"
                + "}";
    }
}
