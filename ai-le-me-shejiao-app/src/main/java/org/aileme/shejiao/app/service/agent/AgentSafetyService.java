package org.aileme.shejiao.app.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.SensitiveService;
import org.aileme.shejiao.domain.vo.AgentSuggestionVo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent 安全与合规处理
 */
@Slf4j
@Service
public class AgentSafetyService {

    @Autowired
    private SensitiveService sensitiveService;

    private static final List<String> STRANGER_RESTRICTED_WORDS = Arrays.asList(
            "宝贝", "亲爱的", "老婆", "老公", "约吗", "今晚见", "开房", "私密照"
    );

    public String sanitizeText(String source, int maxLen) {
        if (StringUtils.isBlank(source)) {
            return "";
        }
        String normalized = source
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        if (normalized.length() > maxLen) {
            return normalized.substring(0, maxLen);
        }
        return normalized;
    }

    public boolean isSensitive(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(sensitiveService.checkContent(text));
        } catch (Exception ex) {
            log.warn("[agent-safety] sensitive check blocked: {}", ex.getMessage());
            return true;
        }
    }

    public List<AgentSuggestionVo> enforcePolicy(List<AgentSuggestionVo> raw, boolean isFriend) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        return raw.stream()
                .map(item -> normalizeSuggestion(item, isFriend))
                .filter(item -> StringUtils.isNotBlank(item.getText()))
                .collect(Collectors.toList());
    }

    public List<AgentSuggestionVo> fallbackSuggestions(String scene) {
        List<AgentSuggestionVo> fallback = new ArrayList<>();
        fallback.add(AgentSuggestionVo.builder()
                .suggestionId(newSuggestionId())
                .text("你好，很高兴认识你。想先从兴趣爱好开始聊聊吗？")
                .styleTag("gentle")
                .riskLevel("low")
                .reason("模型异常时的安全兜底")
                .nextAction(defaultNextAction(scene))
                .build());
        fallback.add(AgentSuggestionVo.builder()
                .suggestionId(newSuggestionId())
                .text("我在认真找长期关系，想先简单认识一下你，方便吗？")
                .styleTag("sincere")
                .riskLevel("low")
                .reason("模型异常时的安全兜底")
                .nextAction(defaultNextAction(scene))
                .build());
        fallback.add(AgentSuggestionVo.builder()
                .suggestionId(newSuggestionId())
                .text("如果你愿意，我们可以先互相看一下 AI 自我介绍视频再聊。")
                .styleTag("video")
                .riskLevel("low")
                .reason("模型异常时的安全兜底")
                .nextAction("jump_ai_video")
                .build());
        return fallback;
    }

    public String defaultNextAction(String scene) {
        if (StringUtils.equalsAnyIgnoreCase(scene, "same_city", "visitors", "fans")) {
            return "open_chat";
        }
        if (StringUtils.equalsAnyIgnoreCase(scene, "video_script")) {
            return "jump_ai_video";
        }
        return "fill_draft";
    }

    public String newSuggestionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private AgentSuggestionVo normalizeSuggestion(AgentSuggestionVo item, boolean isFriend) {
        String text = sanitizeText(item.getText(), 120);
        if (!isFriend) {
            text = downToneForStranger(text);
        }

        String riskLevel = StringUtils.defaultIfBlank(item.getRiskLevel(), "low").toLowerCase(Locale.ROOT);
        if (isSensitive(text)) {
            text = "我们先从兴趣和日常聊起，慢慢了解彼此会更舒服。";
            riskLevel = "high";
        }

        return AgentSuggestionVo.builder()
                .suggestionId(StringUtils.defaultIfBlank(item.getSuggestionId(), newSuggestionId()))
                .text(text)
                .styleTag(StringUtils.defaultIfBlank(item.getStyleTag(), "neutral"))
                .riskLevel(riskLevel)
                .reason(StringUtils.defaultIfBlank(item.getReason(), "基于当前场景生成"))
                .nextAction(StringUtils.defaultIfBlank(item.getNextAction(), "fill_draft"))
                .build();
    }

    private String downToneForStranger(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        String safe = text;
        for (String word : STRANGER_RESTRICTED_WORDS) {
            if (safe.contains(word)) {
                safe = safe.replace(word, "你");
            }
        }
        return safe;
    }
}
