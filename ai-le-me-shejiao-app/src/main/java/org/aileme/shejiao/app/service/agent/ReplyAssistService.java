package org.aileme.shejiao.app.service.agent;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.vo.AgentSuggestionVo;

import java.util.ArrayList;
import java.util.List;

/**
 * 续聊回复建议服务
 */
@Service
public class ReplyAssistService {

    @Autowired
    private AgentSafetyService safetyService;
    @Autowired
    private AgentLlmSuggestionService llmSuggestionService;

    public List<AgentSuggestionVo> suggest(AppUserEntity me,
                                           AppUserEntity target,
                                           List<String> lastMessages,
                                           String tone,
                                           boolean isFriend) {
        String last = extractLastMessage(lastMessages);
        List<AgentSuggestionVo> aiSuggestions = llmSuggestionService.generateSuggestions(
                "reply",
                "你是婚恋社交App的聊天助手。请生成自然、礼貌、真诚的续聊文案。"
                    + "必须只输出JSON，不要markdown。JSON格式：{\"suggestions\":[{\"text\":\"...\",\"styleTag\":\"sincere|polite|active\",\"reason\":\"...\",\"nextAction\":\"fill_draft\"}]}",
                buildReplyUserPrompt(me, target, lastMessages, tone, isFriend, last),
                3,
                "fill_draft"
        );
        if (!aiSuggestions.isEmpty()) {
            if (!isFriend && aiSuggestions.stream().noneMatch(item -> "jump_ai_video".equals(item.getNextAction()))) {
                aiSuggestions.add(build("如果你愿意，我们也可以先互看 AI 自我介绍视频，再继续聊。", "video", "陌生关系降低沟通门槛", "jump_ai_video"));
            }
            return aiSuggestions;
        }

        List<AgentSuggestionVo> suggestions = new ArrayList<>();

        String polite = StringUtils.isNotBlank(last)
                ? "你刚刚提到“" + last + "”，我想再多了解一点，你方便展开说说吗？"
                : "很高兴认识你，我想认真了解你，你更看重关系中的哪一点？";

        String active = StringUtils.isNotBlank(last)
                ? "你说到“" + last + "”我很有共鸣。要不要我们各自分享一件最近最开心的小事？"
                : "我们来个轻松开场吧：最近让你最开心的一件小事是什么？";

        String sincere = "我想认真推进这段认识，不着急，但希望彼此都真诚。你更期待怎样的相处节奏？";

        suggestions.add(build(polite, "polite", "继续提问，建立安全感", "fill_draft"));
        suggestions.add(build(active, "active", "提升互动感，减少冷场", "fill_draft"));
        suggestions.add(build(sincere, "sincere", "传达长期关系意愿", "fill_draft"));

        // 根据偏好语气前置
        if (StringUtils.isNotBlank(tone)) {
            suggestions.sort((a, b) -> {
                if (StringUtils.equalsIgnoreCase(a.getStyleTag(), tone)) return -1;
                if (StringUtils.equalsIgnoreCase(b.getStyleTag(), tone)) return 1;
                return 0;
            });
        }

        if (!isFriend) {
            suggestions.add(build("如果你愿意，我们也可以先互看 AI 自我介绍视频，再继续聊。", "video", "陌生关系降低沟通门槛", "jump_ai_video"));
        }

        return suggestions;
    }

    private AgentSuggestionVo build(String text, String styleTag, String reason, String nextAction) {
        return AgentSuggestionVo.builder()
                .suggestionId(safetyService.newSuggestionId())
                .text(text)
                .styleTag(styleTag)
                .riskLevel("low")
                .reason(reason)
                .nextAction(nextAction)
                .build();
    }

    private String extractLastMessage(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            String msg = safetyService.sanitizeText(messages.get(i), 26);
            if (StringUtils.isNotBlank(msg)) {
                return msg;
            }
        }
        return "";
    }

    private String buildReplyUserPrompt(AppUserEntity me,
                                        AppUserEntity target,
                                        List<String> lastMessages,
                                        String tone,
                                        boolean isFriend,
                                        String lastMessage) {
        String recent = lastMessages == null ? "" : String.join(" | ", lastMessages);
        return "请根据以下上下文生成3条续聊建议："
                + "\n我的昵称：" + safetyService.sanitizeText(me.getUsername(), 20)
                + "\n对方昵称：" + safetyService.sanitizeText(target.getUsername(), 20)
                + "\n关系：" + (isFriend ? "已互相关注" : "陌生阶段")
                + "\n偏好语气：" + StringUtils.defaultIfBlank(tone, "sincere")
                + "\n最近消息：" + safetyService.sanitizeText(recent, 220)
                + "\n最近一句重点：" + safetyService.sanitizeText(lastMessage, 40)
                + "\n要求：每条不超过38字，避免油腻和骚扰，内容具体不空泛。";
    }
}
