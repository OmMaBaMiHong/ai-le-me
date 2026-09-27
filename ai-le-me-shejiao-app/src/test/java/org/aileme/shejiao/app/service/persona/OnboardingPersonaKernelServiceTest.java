package org.aileme.shejiao.app.service.persona;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.aileme.shejiao.domain.param.app.OnboardingAnswerForm;
import org.aileme.shejiao.domain.vo.OnboardingPersonaDraftVO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OnboardingPersonaKernelServiceTest {

    private final OnboardingPersonaKernelService kernelService = new OnboardingPersonaKernelService();

    @Test
    public void buildDraftExtractsRelationshipKernelFromAnswers() {
        OnboardingPersonaDraftVO draft = kernelService.buildDraft(List.of(
                answer("relationship_goal", "serious_marriage", "以结婚为前提"),
                answer("romance_pace", "slow_warm", "慢慢了解"),
                answer("communication_style", "text_gentle", "文字最舒服"),
                answer("emotional_need", "stable_response", "回应稳定"),
                answer("attraction_preference", "sincerity", "真诚"),
                answer("boundary", "too_fast", "太快推进"),
                answer("ideal_relationship", null, null, "舒服、稳定、真诚")
        ));

        assertEquals("serious_relationship", draft.getRelationshipGoal());
        assertEquals("slow_warm", draft.getRomancePace());
        assertEquals("text_first_gentle", draft.getCommunicationStyle());
        assertTrue(draft.getEmotionalNeeds().contains("stable_response"));
        assertTrue(draft.getAttractionPreferences().contains("sincerity"));
        assertTrue(draft.getRiskFlags().contains("dislike_fast_push"));
        assertTrue(draft.getTagCandidates().contains("慢热"));
        assertTrue(draft.getTagCandidates().contains("真诚优先"));
        assertTrue(draft.getSummary().contains("慢热"));
        assertTrue(draft.getSummary().contains("真诚"));

        String draftJson = JSON.toJSONString(draft);
        assertTrue(draftJson.contains("\"archetypeTitle\""));
        assertTrue(draftJson.contains("\"coreInsights\""));
        assertTrue(draftJson.contains("\"recommendedApproach\""));
        assertTrue(draftJson.contains("\"avoidSignals\""));
    }

    @Test
    public void buildDraftDerivesMatchmakerStyleOpeningAndProfileDraft() {
        OnboardingPersonaDraftVO draft = kernelService.buildDraft(List.of(
                answer("relationship_goal", "same_frequency_first", "先认识同频的人"),
                answer("romance_pace", "natural_progress", "自然推进"),
                answer("communication_style", "see_then_expand", "看人，聊得来什么都行"),
                answer("emotional_need", "value_alignment", "价值观契合"),
                answer("attraction_preference", "life_style_match", "生活方式合拍"),
                answer("boundary", "oily_flirting", "太油"),
                answer("ideal_relationship", null, null, "希望关系里有轻松感，也有长期稳定性")
        ));

        assertEquals("reliable_warm", draft.getPreferredMatchmakerStyle());
        assertEquals("life_based_natural", draft.getRecommendedOpeningStyle());
        assertTrue(draft.getProfileDraft().getIntro().contains("同频"));
        assertTrue(draft.getProfileDraft().getSelfIntroduction().contains("生活方式"));
        assertTrue(draft.getProfileDraft().getLoveDeclaration().contains("长期稳定"));

        String draftJson = JSON.toJSONString(draft);
        assertTrue(draftJson.contains("同频"));
        assertTrue(draftJson.contains("生活"));
        assertTrue(draftJson.contains("红娘"));
        assertTrue(draftJson.contains("避免"));
    }

    private OnboardingAnswerForm answer(String questionCode, String optionCode, String optionLabel) {
        return answer(questionCode, optionCode, optionLabel, null);
    }

    private OnboardingAnswerForm answer(String questionCode, String optionCode, String optionLabel, String freeText) {
        OnboardingAnswerForm form = new OnboardingAnswerForm();
        form.setQuestionCode(questionCode);
        form.setOptionCode(optionCode);
        form.setOptionLabel(optionLabel);
        form.setFreeText(freeText);
        return form;
    }
}
