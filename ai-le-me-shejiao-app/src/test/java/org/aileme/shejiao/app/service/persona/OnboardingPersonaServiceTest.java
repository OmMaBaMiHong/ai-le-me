package org.aileme.shejiao.app.service.persona;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.app.dao.UserOnboardingPersonaDao;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.UserOnboardingPersonaEntity;
import org.aileme.shejiao.domain.param.app.OnboardingAnswerForm;
import org.aileme.shejiao.domain.param.app.OnboardingPersonaAssessForm;
import org.aileme.shejiao.domain.param.app.OnboardingPersonaConfirmForm;
import org.aileme.shejiao.domain.vo.OnboardingPersonaDraftVO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OnboardingPersonaServiceTest {

    @Mock
    private UserOnboardingPersonaDao onboardingPersonaDao;

    @Mock
    private AppUserService appUserService;

    @Mock
    private OnboardingPersonaKernelService kernelService;

    @InjectMocks
    private OnboardingPersonaService onboardingPersonaService;

    @Test
    public void assessCreatesOrUpdatesDraftAndMarksNeedOnboardingFalseAfterConfirm() {
        OnboardingPersonaAssessForm form = new OnboardingPersonaAssessForm();
        form.setAnswers(List.of(answer("relationship_goal", "serious_marriage", "以结婚为前提")));

        OnboardingPersonaDraftVO draft = new OnboardingPersonaDraftVO();
        draft.setSummary("你偏认真关系导向");

        when(kernelService.buildDraft(form.getAnswers())).thenReturn(draft);
        when(onboardingPersonaDao.selectOne(any())).thenReturn(null);

        OnboardingPersonaDraftVO saved = onboardingPersonaService.assess(1001, form);

        assertEquals("你偏认真关系导向", saved.getSummary());
        verify(onboardingPersonaDao).insert(any(UserOnboardingPersonaEntity.class));
        assertTrue(onboardingPersonaService.shouldShowOnboarding(null));
        UserOnboardingPersonaEntity record = new UserOnboardingPersonaEntity();
        record.setConfirmed(1);
        assertFalse(onboardingPersonaService.shouldShowOnboarding(record));
    }

    @Test
    public void confirmOnlyBackfillsBlankProfileFields() {
        AppUserEntity user = new AppUserEntity();
        user.setUid(1001);
        user.setIntro("已有签名");
        user.setSelfIntroduction("");
        user.setLoveDeclaration(null);
        user.setTagStr(null);

        UserOnboardingPersonaEntity record = new UserOnboardingPersonaEntity();
        record.setUserId(1001);

        OnboardingPersonaConfirmForm form = new OnboardingPersonaConfirmForm();
        form.setApplyProfileDraft(true);
        form.setApplyTagCandidates(true);

        OnboardingPersonaDraftVO draft = new OnboardingPersonaDraftVO();
        draft.setTagCandidates(List.of("慢热", "真诚优先"));
        OnboardingPersonaDraftVO.ProfileDraft profileDraft = new OnboardingPersonaDraftVO.ProfileDraft();
        profileDraft.setIntro("建议签名");
        profileDraft.setSelfIntroduction("我是一个慢热但真诚的人");
        profileDraft.setLoveDeclaration("想要稳定、舒服、长期的关系");
        draft.setProfileDraft(profileDraft);

        when(appUserService.getById(1001)).thenReturn(user);
        when(onboardingPersonaDao.selectOne(any())).thenReturn(record);
        when(kernelService.parseDraft(record)).thenReturn(draft);
        when(appUserService.updateById(any(AppUserEntity.class))).thenReturn(true);

        onboardingPersonaService.confirm(1001, form);

        ArgumentCaptor<AppUserEntity> userCaptor = ArgumentCaptor.forClass(AppUserEntity.class);
        verify(appUserService).updateById(userCaptor.capture());
        AppUserEntity updated = userCaptor.getValue();
        assertEquals("已有签名", updated.getIntro());
        assertEquals("我是一个慢热但真诚的人", updated.getSelfIntroduction());
        assertEquals("想要稳定、舒服、长期的关系", updated.getLoveDeclaration());
        assertTrue(updated.getTagStr().contains("慢热"));
        verify(onboardingPersonaDao).updateById(any(UserOnboardingPersonaEntity.class));
    }

    private OnboardingAnswerForm answer(String questionCode, String optionCode, String optionLabel) {
        OnboardingAnswerForm form = new OnboardingAnswerForm();
        form.setQuestionCode(questionCode);
        form.setOptionCode(optionCode);
        form.setOptionLabel(optionLabel);
        return form;
    }
}
