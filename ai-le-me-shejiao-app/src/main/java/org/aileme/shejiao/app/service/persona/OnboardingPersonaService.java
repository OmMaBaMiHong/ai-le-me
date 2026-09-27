package org.aileme.shejiao.app.service.persona;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.aileme.common.redis.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.app.dao.UserOnboardingPersonaDao;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.UserOnboardingPersonaEntity;
import org.aileme.shejiao.domain.param.app.OnboardingPersonaAssessForm;
import org.aileme.shejiao.domain.param.app.OnboardingPersonaConfirmForm;
import org.aileme.shejiao.domain.vo.OnboardingPersonaDraftVO;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class OnboardingPersonaService {

    @Autowired
    private UserOnboardingPersonaDao onboardingPersonaDao;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private OnboardingPersonaKernelService kernelService;

    public OnboardingPersonaDraftVO assess(Integer userId, OnboardingPersonaAssessForm form) {
        OnboardingPersonaDraftVO draft = kernelService.buildDraft(form == null ? null : form.getAnswers());
        UserOnboardingPersonaEntity record = findByUserId(userId);
        Date now = new Date();
        if (record == null) {
            record = new UserOnboardingPersonaEntity();
            record.setUserId(userId);
            record.setCreateTime(now);
            record.setStatus(1);
            record.setConfirmed(0);
            record.setAnswersJson(JSON.toJSONString(form == null ? null : form.getAnswers()));
            record.setPersonaJson(JSON.toJSONString(draft));
            record.setSummary(draft.getSummary());
            record.setUpdateTime(now);
            onboardingPersonaDao.insert(record);
        } else {
            record.setAnswersJson(JSON.toJSONString(form == null ? null : form.getAnswers()));
            record.setPersonaJson(JSON.toJSONString(draft));
            record.setSummary(draft.getSummary());
            record.setConfirmed(0);
            record.setStatus(1);
            record.setUpdateTime(now);
            onboardingPersonaDao.updateById(record);
        }
        return draft;
    }

    public OnboardingPersonaDraftVO getMyDraft(Integer userId) {
        UserOnboardingPersonaEntity record = findByUserId(userId);
        if (record == null) {
            return new OnboardingPersonaDraftVO();
        }
        return kernelService.parseDraft(record);
    }

    public boolean shouldShowOnboarding(UserOnboardingPersonaEntity record) {
        return record == null || !Objects.equals(record.getConfirmed(), 1);
    }

    public boolean needOnboarding(Integer userId) {
        return shouldShowOnboarding(findByUserId(userId));
    }

    public UserOnboardingPersonaEntity getRecord(Integer userId) {
        return findByUserId(userId);
    }

    public OnboardingPersonaDraftVO confirm(Integer userId, OnboardingPersonaConfirmForm form) {
        UserOnboardingPersonaEntity record = findByUserId(userId);
        if (record == null) {
            throw new LinfengException("请先完成首登画像引导");
        }
        OnboardingPersonaDraftVO draft = kernelService.parseDraft(record);
        if (Boolean.TRUE.equals(form == null ? Boolean.TRUE : form.getApplyProfileDraft())
                || Boolean.TRUE.equals(form == null ? Boolean.TRUE : form.getApplyTagCandidates())) {
            applyDraftToUser(userId, draft, form);
        }
        record.setConfirmed(1);
        record.setUpdateTime(new Date());
        onboardingPersonaDao.updateById(record);
        return draft;
    }

    private void applyDraftToUser(Integer userId, OnboardingPersonaDraftVO draft, OnboardingPersonaConfirmForm form) {
        AppUserEntity user = appUserService.getById(userId);
        if (user == null) {
            throw new LinfengException("用户不存在");
        }
        boolean changed = false;
        boolean applyProfileDraft = form == null || Boolean.TRUE.equals(form.getApplyProfileDraft());
        boolean applyTagCandidates = form == null || Boolean.TRUE.equals(form.getApplyTagCandidates());
        if (applyProfileDraft && draft.getProfileDraft() != null) {
            if (StringUtils.isBlank(user.getIntro()) && StringUtils.isNotBlank(draft.getProfileDraft().getIntro())) {
                user.setIntro(draft.getProfileDraft().getIntro());
                changed = true;
            }
            if (StringUtils.isBlank(user.getSelfIntroduction()) && StringUtils.isNotBlank(draft.getProfileDraft().getSelfIntroduction())) {
                user.setSelfIntroduction(draft.getProfileDraft().getSelfIntroduction());
                changed = true;
            }
            if (StringUtils.isBlank(user.getLoveDeclaration()) && StringUtils.isNotBlank(draft.getProfileDraft().getLoveDeclaration())) {
                user.setLoveDeclaration(draft.getProfileDraft().getLoveDeclaration());
                changed = true;
            }
        }
        if (applyTagCandidates && draft.getTagCandidates() != null && !draft.getTagCandidates().isEmpty()) {
            List<String> merged = mergeTags(user.getTagStr(), draft.getTagCandidates());
            if (!merged.isEmpty()) {
                user.setTagStr(JSON.toJSONString(merged));
                changed = true;
            }
        }
        if (changed) {
            user.setUpdateTime(new Date());
            appUserService.updateById(user);
            clearUserCache(userId);
        }
    }

    private void clearUserCache(Integer userId) {
        try {
            RedisUtils.deleteObject("userId:" + userId);
        } catch (Throwable ignored) {
        }
    }

    private List<String> mergeTags(String rawTagStr, List<String> newTags) {
        Set<String> merged = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(rawTagStr)) {
            try {
                merged.addAll(JSONArray.parseArray(rawTagStr, String.class));
            } catch (Exception ignored) {
            }
        }
        merged.addAll(newTags);
        List<String> result = new ArrayList<>(merged);
        return result.subList(0, Math.min(result.size(), 6));
    }

    private UserOnboardingPersonaEntity findByUserId(Integer userId) {
        LambdaQueryWrapper<UserOnboardingPersonaEntity> wrapper = new LambdaQueryWrapper<UserOnboardingPersonaEntity>()
                .eq(UserOnboardingPersonaEntity::getUserId, userId)
                .eq(UserOnboardingPersonaEntity::getStatus, 1)
                .orderByDesc(UserOnboardingPersonaEntity::getId)
                .last("limit 1");
        return onboardingPersonaDao.selectOne(wrapper);
    }
}
