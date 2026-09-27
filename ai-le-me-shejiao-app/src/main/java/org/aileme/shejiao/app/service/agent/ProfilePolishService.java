package org.aileme.shejiao.app.service.agent;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.domain.vo.AgentSuggestionVo;

import java.util.ArrayList;
import java.util.List;

/**
 * 文案润色服务
 */
@Service
public class ProfilePolishService {

    @Autowired
    private AgentSafetyService safetyService;
    @Autowired
    private AgentLlmSuggestionService llmSuggestionService;

    public List<AgentSuggestionVo> polish(String source, String style) {
        String raw = safetyService.sanitizeText(source, 300);
        List<AgentSuggestionVo> result = new ArrayList<>();

        if (StringUtils.isBlank(raw)) {
            result.add(build("你好，我想认真认识你，可以先从兴趣爱好聊起吗？", "polite", "空文案兜底"));
            return result;
        }

        List<AgentSuggestionVo> aiSuggestions = llmSuggestionService.generateSuggestions(
                "profile_polish",
                "你是婚恋社交App文案润色助手。请把输入改写成更自然、更有礼貌的私信文案。"
                    + "必须只输出JSON，不要markdown。JSON格式：{\"suggestions\":[{\"text\":\"...\",\"styleTag\":\"polite|active|sincere\",\"reason\":\"...\",\"nextAction\":\"fill_draft\"}]}",
                buildPolishPrompt(raw, style),
                3,
                "fill_draft"
        );
        if (!aiSuggestions.isEmpty()) {
            if (StringUtils.isNotBlank(style)) {
                aiSuggestions.sort((a, b) -> {
                    if (StringUtils.equalsIgnoreCase(a.getStyleTag(), style)) return -1;
                    if (StringUtils.equalsIgnoreCase(b.getStyleTag(), style)) return 1;
                    return 0;
                });
            }
            return aiSuggestions;
        }

        result.add(build(toPolite(raw), "polite", "礼貌润色，降低打扰感"));
        result.add(build(toActive(raw), "active", "更主动，提升回复概率"));
        result.add(build(toSincere(raw), "sincere", "更真诚，传达长期意愿"));

        if (StringUtils.isNotBlank(style)) {
            result.sort((a, b) -> {
                if (StringUtils.equalsIgnoreCase(a.getStyleTag(), style)) return -1;
                if (StringUtils.equalsIgnoreCase(b.getStyleTag(), style)) return 1;
                return 0;
            });
        }

        return result;
    }

    private AgentSuggestionVo build(String text, String styleTag, String reason) {
        return AgentSuggestionVo.builder()
                .suggestionId(safetyService.newSuggestionId())
                .text(text)
                .styleTag(styleTag)
                .riskLevel("low")
                .reason(reason)
                .nextAction("fill_draft")
                .build();
    }

    private String toPolite(String source) {
        String text = source;
        if (!text.endsWith("？") && !text.endsWith("?")) {
            text = text + "，你方便的话可以聊聊吗？";
        }
        return "你好，" + stripLeadPronoun(text);
    }

    private String toActive(String source) {
        return "我想主动认识你，" + stripLeadPronoun(source) + "；如果你愿意，我们现在就从一个小问题开始。";
    }

    private String toSincere(String source) {
        return "我来这里是认真找长期关系的。" + stripLeadPronoun(source) + "，希望我们都真诚一点。";
    }

    private String stripLeadPronoun(String source) {
        if (StringUtils.isBlank(source)) {
            return "";
        }
        String text = source.trim();
        if (text.startsWith("我")) {
            return text.substring(1);
        }
        return text;
    }

    private String buildPolishPrompt(String source, String style) {
        return "请输出3条改写建议："
                + "\n原文：" + source
                + "\n优先风格：" + StringUtils.defaultIfBlank(style, "sincere")
                + "\n要求：每条不超过45字、表达自然、避免油腻。";
    }
}
