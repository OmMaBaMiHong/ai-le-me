package org.aileme.shejiao.app.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.domain.vo.AgentSuggestionVo;
import org.aileme.shejiao.gateway.chat.ChatModelGatewayService;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 建议生成与解析
 */
@Slf4j
@Service
public class AgentLlmSuggestionService {

    @Autowired(required = false)
    private ChatModelGatewayService chatModelGatewayService;

    @Autowired
    private AgentSafetyService safetyService;

    @Autowired
    private ObjectMapper objectMapper;

    public List<AgentSuggestionVo> generateSuggestions(String scene,
                                                       String systemPrompt,
                                                       String userPrompt,
                                                       int limit,
                                                       String defaultNextAction) {
        if (chatModelGatewayService == null) {
            return List.of();
        }
        String result;
        try {
            result = chatModelGatewayService.complete(scene, systemPrompt, userPrompt, 420, 0.7D);
        } catch (Exception ex) {
            // 模型网关异常时回退到规则建议，避免接口整体失败
            log.warn("[agent-llm] complete failed, scene={}, fallback to rule suggestions: {}", scene, ex.getMessage());
            return List.of();
        }
        if (StringUtils.isBlank(result)) {
            return List.of();
        }
        List<AgentSuggestionVo> parsed = parseSuggestions(result, Math.max(1, limit), defaultNextAction);
        return parsed.isEmpty() ? List.of() : parsed;
    }

    private List<AgentSuggestionVo> parseSuggestions(String raw, int limit, String defaultNextAction) {
        String normalized = stripMarkdownCodeFence(StringUtils.trimToEmpty(raw));
        List<AgentSuggestionVo> fromJson = parseJsonSuggestions(normalized, limit, defaultNextAction);
        if (!fromJson.isEmpty()) {
            return fromJson;
        }
        return parseLineSuggestions(normalized, limit, defaultNextAction);
    }

    private List<AgentSuggestionVo> parseJsonSuggestions(String text, int limit, String defaultNextAction) {
        if (StringUtils.isBlank(text)) {
            return List.of();
        }
        List<AgentSuggestionVo> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(text);
            JsonNode suggestionsNode = root;
            if (root.isObject() && root.has("suggestions")) {
                suggestionsNode = root.get("suggestions");
            }

            if (!suggestionsNode.isArray()) {
                return List.of();
            }

            for (JsonNode item : suggestionsNode) {
                if (item == null || item.isNull()) {
                    continue;
                }
                String content = firstNonBlank(
                    item.path("text").asText(""),
                    item.path("content").asText(""),
                    item.path("message").asText("")
                );
                content = safetyService.sanitizeText(content, 120);
                if (StringUtils.isBlank(content)) {
                    continue;
                }

                String styleTag = firstNonBlank(item.path("styleTag").asText(""), item.path("style").asText("gentle"));
                String reason = safetyService.sanitizeText(firstNonBlank(item.path("reason").asText(""), "AI生成建议"), 40);
                String nextAction = firstNonBlank(
                    item.path("nextAction").asText(""),
                    item.path("action").asText(""),
                    defaultNextAction
                );

                result.add(AgentSuggestionVo.builder()
                    .suggestionId(safetyService.newSuggestionId())
                    .text(content)
                    .styleTag(StringUtils.defaultIfBlank(styleTag, "gentle"))
                    .riskLevel("low")
                    .reason(StringUtils.defaultIfBlank(reason, "AI生成建议"))
                    .nextAction(StringUtils.defaultIfBlank(nextAction, defaultNextAction))
                    .build());
                if (result.size() >= limit) {
                    break;
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return result;
    }

    private List<AgentSuggestionVo> parseLineSuggestions(String text, int limit, String defaultNextAction) {
        if (StringUtils.isBlank(text)) {
            return List.of();
        }
        List<AgentSuggestionVo> suggestions = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String content = StringUtils.trimToEmpty(line);
            content = StringUtils.removeStart(content, "-");
            content = StringUtils.removeStart(content, "*");
            content = StringUtils.trimToEmpty(content);
            content = safetyService.sanitizeText(content, 120);
            if (StringUtils.isBlank(content)) {
                continue;
            }
            suggestions.add(AgentSuggestionVo.builder()
                .suggestionId(safetyService.newSuggestionId())
                .text(content)
                .styleTag("gentle")
                .riskLevel("low")
                .reason("AI生成建议")
                .nextAction(defaultNextAction)
                .build());
            if (suggestions.size() >= limit) {
                break;
            }
        }
        return suggestions;
    }

    private String stripMarkdownCodeFence(String text) {
        if (StringUtils.startsWith(text, "```") && StringUtils.endsWith(text, "```")) {
            String stripped = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
            stripped = stripped.replaceFirst("\\s*```$", "");
            return stripped.trim();
        }
        return text;
    }

    private String firstNonBlank(String... values) {
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
