package org.aileme.shejiao.app.runtime.compliance;

import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.PlatformBusinessConfigService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.exception.MiniAppFeatureDisabledException;
import org.aileme.shejiao.common.utils.Constant;

@Service
public class MiniAppFilingFeatureService {

    public static final String KEY_ENABLED = Constant.IS_OPEN;
    public static final String KEY_ASSISTANT = KEY_ENABLED;
    public static final String KEY_SOCIAL_INTENT = KEY_ENABLED;
    public static final String KEY_PAYMENT = KEY_ENABLED;
    public static final String KEY_HONGNIANG = KEY_ENABLED;
    public static final String KEY_AI_VIDEO = KEY_ENABLED;
    public static final String KEY_PERSONA_PAID = KEY_ENABLED;

    private final PlatformBusinessConfigService businessConfigService;

    public MiniAppFilingFeatureService(PlatformBusinessConfigService businessConfigService) {
        this.businessConfigService = businessConfigService;
    }

    public boolean isAssistantEnabled() {
        return isFeatureEnabled(KEY_ASSISTANT);
    }

    public boolean isSocialIntentEnabled() {
        return isFeatureEnabled(KEY_SOCIAL_INTENT);
    }

    public boolean isPaymentEnabled() {
        return isFeatureEnabled(KEY_PAYMENT);
    }

    public boolean isHongniangEnabled() {
        return isFeatureEnabled(KEY_HONGNIANG);
    }

    public boolean isAiVideoEnabled() {
        return isFeatureEnabled(KEY_AI_VIDEO);
    }

    public boolean isPersonaPaidEnabled() {
        return isFeatureEnabled(KEY_PERSONA_PAID);
    }

    public boolean isFeatureEnabled(String key) {
        String rawValue = businessConfigService.getString(KEY_ENABLED, MiniAppTotalSwitchSupport.ENABLED_VALUE);
        return MiniAppTotalSwitchSupport.isEnabled(rawValue, true);
    }

    public boolean isFeatureEnabledForPlatform(String platformCode, String key) {
        return isFeatureEnabled(key);
    }

    public void requireAssistantEnabled() {
        requireFeature(KEY_ASSISTANT, "当前能力暂未开放");
    }

    public void requireSocialIntentEnabled() {
        requireFeature(KEY_SOCIAL_INTENT, "申请微信和送心意暂未开放");
    }

    public void requirePaymentEnabled() {
        requireFeature(KEY_PAYMENT, "充值与账户能力暂未开放");
    }

    public void requireHongniangEnabled() {
        requireFeature(KEY_HONGNIANG, "红娘与活动入口暂未开放");
    }

    public void requireAiVideoEnabled() {
        requireFeature(KEY_AI_VIDEO, "AI创作入口暂未开放");
    }

    public void requirePersonaPaidEnabled() {
        requireFeature(KEY_PERSONA_PAID, "画像付费查看暂未开放");
    }

    private void requireFeature(String key, String message) {
        if (!isFeatureEnabled(key)) {
            throw new MiniAppFeatureDisabledException(message);
        }
    }
}
