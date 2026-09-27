package org.aileme.shejiao.app.service.persona;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.domain.entity.admin.UserOnboardingPersonaEntity;
import org.aileme.shejiao.domain.param.app.OnboardingAnswerForm;
import org.aileme.shejiao.domain.vo.OnboardingPersonaDraftVO;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class OnboardingPersonaKernelService {

    public OnboardingPersonaDraftVO buildDraft(List<OnboardingAnswerForm> answers) {
        OnboardingPersonaDraftVO draft = new OnboardingPersonaDraftVO();
        if (answers == null) {
            answers = new ArrayList<>();
        }
        String idealRelationship = "";
        Set<String> tags = new LinkedHashSet<>();
        for (OnboardingAnswerForm answer : answers) {
            if (answer == null || StringUtils.isBlank(answer.getQuestionCode())) {
                continue;
            }
            String code = StringUtils.trimToEmpty(answer.getQuestionCode()).toLowerCase(Locale.ROOT);
            String optionCode = StringUtils.trimToEmpty(answer.getOptionCode()).toLowerCase(Locale.ROOT);
            switch (code) {
                case "relationship_goal" -> applyRelationshipGoal(draft, optionCode, tags);
                case "romance_pace" -> applyRomancePace(draft, optionCode, tags);
                case "communication_style" -> applyCommunicationStyle(draft, optionCode);
                case "emotional_need" -> applyEmotionalNeed(draft, optionCode, tags);
                case "attraction_preference" -> applyAttractionPreference(draft, optionCode, tags);
                case "boundary" -> applyBoundary(draft, optionCode);
                case "ideal_relationship" -> idealRelationship = StringUtils.defaultString(answer.getFreeText()).trim();
                default -> {
                }
            }
        }
        draft.setPreferredMatchmakerStyle(resolveMatchmakerStyle(draft));
        draft.setRecommendedOpeningStyle(resolveOpeningStyle(draft));
        if (StringUtils.isNotBlank(draft.getRomancePace()) && "slow_warm".equals(draft.getRomancePace())) {
            tags.add("慢热");
        }
        draft.setTagCandidates(new ArrayList<>(tags).subList(0, Math.min(tags.size(), 6)));
        draft.setArchetypeTitle(buildArchetypeTitle(draft));
        draft.setArchetypeSubtitle(buildArchetypeSubtitle(draft));
        draft.setCoreInsights(buildCoreInsights(draft, idealRelationship));
        draft.setRecommendedApproach(buildRecommendedApproach(draft));
        draft.setAvoidSignals(buildAvoidSignals(draft));
        draft.setSummary(buildSummary(draft, idealRelationship));
        draft.setProfileDraft(buildProfileDraft(draft, idealRelationship));
        return draft;
    }

    public OnboardingPersonaDraftVO parseDraft(UserOnboardingPersonaEntity entity) {
        if (entity == null || StringUtils.isBlank(entity.getPersonaJson())) {
            return new OnboardingPersonaDraftVO();
        }
        return JSON.parseObject(entity.getPersonaJson(), OnboardingPersonaDraftVO.class);
    }

    private void applyRelationshipGoal(OnboardingPersonaDraftVO draft, String optionCode, Set<String> tags) {
        switch (optionCode) {
            case "serious_marriage", "serious_relationship" -> {
                draft.setRelationshipGoal("serious_relationship");
                tags.add("结婚导向");
            }
            case "same_frequency_first" -> {
                draft.setRelationshipGoal("same_frequency_first");
                tags.add("同频优先");
            }
            case "natural_explore", "explore_natural" -> draft.setRelationshipGoal("natural_explore");
            default -> draft.setRelationshipGoal("serious_relationship");
        }
    }

    private void applyRomancePace(OnboardingPersonaDraftVO draft, String optionCode, Set<String> tags) {
        switch (optionCode) {
            case "slow_warm" -> {
                draft.setRomancePace("slow_warm");
                tags.add("慢热");
            }
            case "natural_progress" -> draft.setRomancePace("natural_progress");
            case "clear_direct" -> {
                draft.setRomancePace("clear_direct");
                tags.add("明确表达");
            }
            case "fast_if_right" -> draft.setRomancePace("fast_if_right");
            default -> draft.setRomancePace("natural_progress");
        }
    }

    private void applyCommunicationStyle(OnboardingPersonaDraftVO draft, String optionCode) {
        switch (optionCode) {
            case "text_gentle" -> draft.setCommunicationStyle("text_first_gentle");
            case "voice_real" -> draft.setCommunicationStyle("voice_first_real");
            case "offline_important" -> draft.setCommunicationStyle("offline_first_real");
            case "see_then_expand" -> draft.setCommunicationStyle("adaptive_after_sync");
            default -> draft.setCommunicationStyle("text_first_gentle");
        }
    }

    private void applyEmotionalNeed(OnboardingPersonaDraftVO draft, String optionCode, Set<String> tags) {
        if (StringUtils.isBlank(optionCode)) {
            return;
        }
        draft.getEmotionalNeeds().add(optionCode);
        switch (optionCode) {
            case "stable_response" -> tags.add("稳定回应");
            case "clear_intent" -> tags.add("明确意图");
            case "consistent_action" -> tags.add("行动一致");
            case "value_alignment" -> tags.add("价值观契合");
            default -> {
            }
        }
    }

    private void applyAttractionPreference(OnboardingPersonaDraftVO draft, String optionCode, Set<String> tags) {
        if (StringUtils.isBlank(optionCode)) {
            return;
        }
        draft.getAttractionPreferences().add(optionCode);
        switch (optionCode) {
            case "sincerity" -> tags.add("真诚优先");
            case "emotional_stability" -> tags.add("情绪稳定控");
            case "life_style_match" -> tags.add("生活感");
            case "interesting" -> tags.add("有趣灵魂");
            case "ambitious" -> tags.add("上进心");
            default -> {
            }
        }
    }

    private void applyBoundary(OnboardingPersonaDraftVO draft, String optionCode) {
        switch (optionCode) {
            case "too_fast" -> draft.getRiskFlags().add("dislike_fast_push");
            case "oily_flirting" -> draft.getRiskFlags().add("dislike_oily_flirting");
            case "too_cold" -> draft.getRiskFlags().add("dislike_cold_response");
            case "disappear" -> draft.getRiskFlags().add("dislike_disappearing");
            case "just_words" -> draft.getRiskFlags().add("dislike_words_without_action");
            default -> {
            }
        }
    }

    private String resolveMatchmakerStyle(OnboardingPersonaDraftVO draft) {
        if ("slow_warm".equals(draft.getRomancePace())
                || draft.getEmotionalNeeds().contains("stable_response")
                || draft.getEmotionalNeeds().contains("value_alignment")) {
            return "reliable_warm";
        }
        if ("clear_direct".equals(draft.getRomancePace())
                || draft.getEmotionalNeeds().contains("clear_intent")) {
            return "clear_direct";
        }
        return "sincere_natural";
    }

    private String resolveOpeningStyle(OnboardingPersonaDraftVO draft) {
        if (draft.getAttractionPreferences().contains("life_style_match")
                || draft.getAttractionPreferences().contains("sincerity")
                || draft.getRiskFlags().contains("dislike_oily_flirting")) {
            return "life_based_natural";
        }
        if ("text_first_gentle".equals(draft.getCommunicationStyle())) {
            return "text_gentle_probe";
        }
        return "clear_light";
    }

    private String buildSummary(OnboardingPersonaDraftVO draft, String idealRelationship) {
        String paceText = paceSummaryText(draft);
        String attractionText = attractionFocusText(draft);
        String needText = emotionalNeedText(draft);
        String tail = StringUtils.isNotBlank(idealRelationship) ? "，也希望关系里" + idealRelationship : "";
        return "你偏" + paceText + "型关系节奏，重视" + needText + "和" + attractionText + "表达" + tail + "。";
    }

    private OnboardingPersonaDraftVO.ProfileDraft buildProfileDraft(OnboardingPersonaDraftVO draft, String idealRelationship) {
        OnboardingPersonaDraftVO.ProfileDraft profileDraft = new OnboardingPersonaDraftVO.ProfileDraft();
        String goalText = switch (StringUtils.defaultString(draft.getRelationshipGoal())) {
            case "same_frequency_first" -> "想先认识同频的人";
            case "natural_explore" -> "期待自然舒服的相识";
            default -> "认真看待长期关系";
        };
        String paceText = "slow_warm".equals(draft.getRomancePace()) ? "慢热真诚" : "自然真诚";
        profileDraft.setIntro(paceText + "，" + goalText);
        String styleText = draft.getAttractionPreferences().contains("life_style_match")
                ? "我更看重生活方式是否合拍"
                : "我更看重相处时的真诚和稳定";
        profileDraft.setSelfIntroduction(styleText + "，希望能先从舒服的交流开始，慢慢建立信任。");
        String loveText = StringUtils.isNotBlank(idealRelationship)
                ? idealRelationship
                : "想要一段舒服、稳定、长期的关系";
        profileDraft.setLoveDeclaration("我理想里的关系是" + loveText + "。");
        return profileDraft;
    }

    private String buildArchetypeTitle(OnboardingPersonaDraftVO draft) {
        if ("same_frequency_first".equals(draft.getRelationshipGoal())) {
            return "同频感受型";
        }
        if ("natural_explore".equals(draft.getRelationshipGoal())) {
            return "自然靠近型";
        }
        if ("clear_direct".equals(draft.getRomancePace())) {
            return "明确投入型";
        }
        if ("slow_warm".equals(draft.getRomancePace())) {
            return "慢热认真型";
        }
        return "真诚长期型";
    }

    private String buildArchetypeSubtitle(OnboardingPersonaDraftVO draft) {
        return "你更容易被" + attractionFocusText(draft) + "打动，也会因为" + emotionalNeedText(draft) + "而更有安全感。";
    }

    private List<String> buildCoreInsights(OnboardingPersonaDraftVO draft, String idealRelationship) {
        List<String> insights = new ArrayList<>();
        insights.add("你更适合" + romancePaceText(draft) + "的推进方式，太用力或太模糊都容易让你退后。");
        insights.add("比起表演感和技巧感，你更容易被" + attractionFocusText(draft) + "以及" + emotionalNeedText(draft) + "打动。");
        if ("same_frequency_first".equals(draft.getRelationshipGoal()) || "natural_explore".equals(draft.getRelationshipGoal())) {
            insights.add("你需要先建立同频和舒服感，确认状态对了，关系才会自然往前走。");
        } else if (StringUtils.isNotBlank(idealRelationship)) {
            insights.add("你不是随便看看型，更希望关系最后能走向" + idealRelationship + "。");
        } else {
            insights.add("你会认真看待长期关系，希望相处既有真诚，也有明确的方向感。");
        }
        return insights;
    }

    private List<String> buildRecommendedApproach(OnboardingPersonaDraftVO draft) {
        List<String> approach = new ArrayList<>();
        approach.add("推荐对象会优先靠近" + matchFocusText(draft) + "这一类信号，先减少明显不合适的人。");
        approach.add("红娘会更偏" + matchmakerStyleText(draft) + "的陪伴方式，帮你降低试探成本。");
        approach.add("破冰会优先使用" + openingStyleText(draft) + "，同时先帮你补齐更贴近你的资料文案和标签。");
        return approach;
    }

    private List<String> buildAvoidSignals(OnboardingPersonaDraftVO draft) {
        List<String> avoidSignals = new ArrayList<>();
        for (String riskFlag : draft.getRiskFlags()) {
            switch (riskFlag) {
                case "dislike_fast_push" -> avoidSignals.add("避免一上来推进过快，让关系失去建立信任的空间。");
                case "dislike_oily_flirting" -> avoidSignals.add("避免油腻和模板化撩法，你更吃真诚而不是套路。");
                case "dislike_cold_response" -> avoidSignals.add("避免忽冷忽热或反馈太淡，这会快速消耗你的安全感。");
                case "dislike_disappearing" -> avoidSignals.add("避免聊着聊着消失，稳定回应比偶尔热情更重要。");
                case "dislike_words_without_action" -> avoidSignals.add("避免只会说不会做，行动一致才会让你真正信任。");
                default -> {
                }
            }
        }
        if (avoidSignals.isEmpty()) {
            avoidSignals.add("避免过度试探，真诚和边界感会比技巧更重要。");
        }
        return avoidSignals.subList(0, Math.min(avoidSignals.size(), 3));
    }

    private String paceSummaryText(OnboardingPersonaDraftVO draft) {
        return switch (StringUtils.defaultString(draft.getRomancePace())) {
            case "slow_warm" -> "慢热";
            case "clear_direct" -> "明确";
            default -> "自然";
        };
    }

    private String romancePaceText(OnboardingPersonaDraftVO draft) {
        return switch (StringUtils.defaultString(draft.getRomancePace())) {
            case "slow_warm" -> "慢慢建立信任";
            case "clear_direct" -> "清楚表达、稳步推进";
            case "fast_if_right" -> "遇到对的人时适度提速";
            default -> "顺着感觉自然靠近";
        };
    }

    private String emotionalNeedText(OnboardingPersonaDraftVO draft) {
        if (draft.getEmotionalNeeds().contains("stable_response")) {
            return "稳定回应";
        }
        if (draft.getEmotionalNeeds().contains("clear_intent")) {
            return "明确表达";
        }
        if (draft.getEmotionalNeeds().contains("consistent_action")) {
            return "行动一致";
        }
        if (draft.getEmotionalNeeds().contains("value_alignment")) {
            return "价值观契合";
        }
        return "舒服相处";
    }

    private String attractionFocusText(OnboardingPersonaDraftVO draft) {
        if (draft.getAttractionPreferences().contains("sincerity")) {
            return "真诚";
        }
        if (draft.getAttractionPreferences().contains("emotional_stability")) {
            return "情绪稳定";
        }
        if (draft.getAttractionPreferences().contains("life_style_match")) {
            return "生活方式合拍";
        }
        if (draft.getAttractionPreferences().contains("interesting")) {
            return "有趣和松弛感";
        }
        if (draft.getAttractionPreferences().contains("ambitious")) {
            return "上进和成长性";
        }
        return "同频";
    }

    private String matchFocusText(OnboardingPersonaDraftVO draft) {
        if ("serious_relationship".equals(draft.getRelationshipGoal())) {
            return "认真、稳定、表达相对清楚";
        }
        if ("same_frequency_first".equals(draft.getRelationshipGoal())) {
            return "同频、自然、有生活感";
        }
        return "舒服、真诚、不表演";
    }

    private String matchmakerStyleText(OnboardingPersonaDraftVO draft) {
        return switch (StringUtils.defaultString(draft.getPreferredMatchmakerStyle())) {
            case "reliable_warm" -> "可靠温和";
            case "clear_direct" -> "明确高效";
            default -> "自然真诚";
        };
    }

    private String openingStyleText(OnboardingPersonaDraftVO draft) {
        return switch (StringUtils.defaultString(draft.getRecommendedOpeningStyle())) {
            case "text_gentle_probe" -> "轻文字慢探测";
            case "clear_light" -> "清爽直接";
            default -> "生活感自然";
        };
    }
}
