package org.aileme.shejiao.app.service.agent;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.vo.AgentSuggestionVo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 破冰建议服务
 */
@Service
public class IcebreakService {

    @Autowired
    private AgentSafetyService safetyService;
    @Autowired
    private AgentLlmSuggestionService llmSuggestionService;

    public List<AgentSuggestionVo> suggest(AppUserEntity me,
                                           AppUserEntity target,
                                           String scene,
                                           int limit,
                                           boolean isFriend,
                                           String personaSummary) {
        List<AgentSuggestionVo> aiSuggestions = llmSuggestionService.generateSuggestions(
                "icebreak",
                "你是婚恋社交App的破冰助手。请输出简洁真诚的开场白。"
                    + "必须只输出JSON，不要markdown。JSON格式：{\"suggestions\":[{\"text\":\"...\",\"styleTag\":\"sincere|gentle\",\"reason\":\"...\",\"nextAction\":\"fill_draft|jump_ai_video\"}]}",
                buildIcebreakPrompt(me, target, scene, isFriend, personaSummary, limit),
                Math.max(1, limit),
                safetyService.defaultNextAction(scene)
        );
        if (!aiSuggestions.isEmpty()) {
            return aiSuggestions;
        }

        List<String> candidates = new ArrayList<>();

        String city = firstNonBlank(target.getLocationCity(), target.getCity(), target.getAbodeCity());
        String hobby = firstNonBlank(target.getInterest(), target.getLoveDeclaration(), target.getIntro());
        String job = StringUtils.defaultIfBlank(target.getJob(), "");

        if (StringUtils.isNotBlank(city)) {
            candidates.add("我在同城推荐里看到你也在" + city + "，想认真认识一下你，方便聊聊吗？");
        }

        if (StringUtils.isNotBlank(job)) {
            candidates.add("看你资料里写了" + job + "，这个方向我挺感兴趣的，你平时工作节奏怎么样？");
        }

        if (StringUtils.isNotBlank(hobby)) {
            String brief = safetyService.sanitizeText(hobby, 24);
            candidates.add("你资料里提到“" + brief + "”，这点很加分。你最近最投入的一件事是什么？");
        }

        if (StringUtils.isNotBlank(personaSummary)) {
            String summary = safetyService.sanitizeText(personaSummary, 30);
            candidates.add("我看过你的 AI 画像总结“" + summary + "”，感觉你是认真型的人，想进一步认识你。");
        }

        candidates.add("我来这里是认真找长期关系的，如果你也一样，我们可以先从日常开始聊。" );
        candidates.add("如果你愿意，我们可以先互看 AI 自我介绍视频，再决定要不要继续深入聊。" );

        if (isFriend) {
            candidates.add("我们已经互相关注了，我想认真推进认识，你最近有空聊聊吗？");
        }

        Set<String> dedup = new LinkedHashSet<>(candidates);
        List<AgentSuggestionVo> result = new ArrayList<>();
        int index = 0;
        for (String text : dedup) {
            if (index >= limit) {
                break;
            }
            result.add(AgentSuggestionVo.builder()
                    .suggestionId(safetyService.newSuggestionId())
                    .text(text)
                    .styleTag(index == 0 ? "sincere" : "gentle")
                    .riskLevel("low")
                    .reason("结合同城资料和关系阶段生成")
                    .nextAction(index == 1 && text.contains("AI") ? "jump_ai_video" : safetyService.defaultNextAction(scene))
                    .build());
            index++;
        }

        return result;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String buildIcebreakPrompt(AppUserEntity me,
                                       AppUserEntity target,
                                       String scene,
                                       boolean isFriend,
                                       String personaSummary,
                                       int limit) {
        String city = firstNonBlank(target.getLocationCity(), target.getCity(), target.getAbodeCity());
        String hobby = firstNonBlank(target.getInterest(), target.getLoveDeclaration(), target.getIntro());
        String job = StringUtils.defaultIfBlank(target.getJob(), "");
        return "请生成" + Math.max(1, limit) + "条破冰开场白："
                + "\n我的昵称：" + safetyService.sanitizeText(me.getUsername(), 20)
                + "\n对方昵称：" + safetyService.sanitizeText(target.getUsername(), 20)
                + "\n场景：" + StringUtils.defaultIfBlank(scene, "same_city")
                + "\n是否已互相关注：" + (isFriend ? "是" : "否")
                + "\n对方城市：" + safetyService.sanitizeText(city, 20)
                + "\n对方职业：" + safetyService.sanitizeText(job, 20)
                + "\n对方兴趣：" + safetyService.sanitizeText(hobby, 30)
                + "\n画像摘要：" + safetyService.sanitizeText(personaSummary, 40)
                + "\n要求：每条不超过40字，避免骚扰和夸张承诺，至少1条可引导对方继续回答。";
    }
}
