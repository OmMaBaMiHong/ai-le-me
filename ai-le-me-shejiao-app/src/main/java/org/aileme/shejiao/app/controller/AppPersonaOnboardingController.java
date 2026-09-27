package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.app.service.persona.OnboardingPersonaService;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.UserOnboardingPersonaEntity;
import org.aileme.shejiao.domain.param.app.OnboardingPersonaAssessForm;
import org.aileme.shejiao.domain.param.app.OnboardingPersonaConfirmForm;
import org.aileme.shejiao.domain.vo.OnboardingPersonaDraftVO;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/app/persona/onboarding")
@Tag(name = "移动端——首登关系画像")
public class AppPersonaOnboardingController {

    @Autowired
    private OnboardingPersonaService onboardingPersonaService;

    @Autowired
    private AppUserService appUserService;

    @Login
    @GetMapping("/my")
    @Operation(summary = "获取我的首登画像草稿")
    public Result<Map<String, Object>> my(@Parameter(hidden = true) @LoginUser AppUserEntity user) {
        UserOnboardingPersonaEntity record = onboardingPersonaService.getRecord(user.getUid());
        OnboardingPersonaDraftVO draft = record == null ? null : onboardingPersonaService.getMyDraft(user.getUid());
        return new Result<Map<String, Object>>().ok(buildResponse(record, draft, null));
    }

    @Login
    @PostMapping("/assess")
    @Operation(summary = "蒸馏首登关系画像草稿")
    public Result<Map<String, Object>> assess(
            @Parameter(hidden = true) @LoginUser AppUserEntity user,
            @RequestBody(required = false) OnboardingPersonaAssessForm form
    ) {
        OnboardingPersonaDraftVO draft = onboardingPersonaService.assess(user.getUid(), form);
        UserOnboardingPersonaEntity record = onboardingPersonaService.getRecord(user.getUid());
        return new Result<Map<String, Object>>().ok(buildResponse(record, draft, null));
    }

    @Login
    @PostMapping("/confirm")
    @Operation(summary = "确认首登关系画像并预填资料")
    public Result<Map<String, Object>> confirm(
            @Parameter(hidden = true) @LoginUser AppUserEntity user,
            @RequestBody(required = false) OnboardingPersonaConfirmForm form
    ) {
        OnboardingPersonaDraftVO draft = onboardingPersonaService.confirm(user.getUid(), form);
        UserOnboardingPersonaEntity record = onboardingPersonaService.getRecord(user.getUid());
        AppUserEntity refreshed = appUserService.getById(user.getUid());
        return new Result<Map<String, Object>>().ok(buildResponse(record, draft, refreshed));
    }

    private Map<String, Object> buildResponse(UserOnboardingPersonaEntity record,
                                              OnboardingPersonaDraftVO draft,
                                              AppUserEntity refreshedUser) {
        Map<String, Object> result = new HashMap<>();
        result.put("needOnboarding", onboardingPersonaService.shouldShowOnboarding(record));
        result.put("confirmed", record != null && Objects.equals(record.getConfirmed(), 1));
        result.put("hasDraft", record != null && draft != null);
        result.put("draft", draft);
        if (refreshedUser != null) {
            result.put("userInfo", appUserService.getUserInfo(refreshedUser));
        }
        return result;
    }
}
