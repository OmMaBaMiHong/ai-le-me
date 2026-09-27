package org.aileme.shejiao.app.service.agent;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AccountService;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.PlatformBusinessConfigService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.vo.AgentCompanionStatusVo;

import java.util.Date;

@Service
public class AgentAccessPolicyService {

    private static final String STATUS_SCENE_CODE = "reply_suggest";

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private AgentGovernanceService agentGovernanceService;

    @Autowired
    private PlatformBusinessConfigService businessConfigService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AgentUsageBillingService agentUsageBillingService;

    public AppUserEntity requireCompanionAccess(AppUserEntity currentUser) {
        AppUserEntity refreshed = refreshUser(currentUser);
        if (refreshed == null || refreshed.getUid() == null) {
            throw new LinfengException("用户不存在");
        }
        if (isRequireVip() && !isVipActive(refreshed)) {
            throw new LinfengException("智能恋爱助手仅限VIP使用");
        }
        if (!agentGovernanceService.isCapabilityEnabled(refreshed.getUid(), AgentGovernanceConstants.CAPABILITY_COMPANION_ENABLED)) {
            throw new LinfengException("请先开启智能恋爱助手开关");
        }
        return refreshed;
    }

    public AgentCompanionStatusVo getStatus(AppUserEntity currentUser) {
        AppUserEntity refreshed = refreshUser(currentUser);
        boolean vipActive = isVipActive(refreshed);
        boolean companionEnabled = refreshed != null
                && refreshed.getUid() != null
                && agentGovernanceService.isCapabilityEnabled(refreshed.getUid(), AgentGovernanceConstants.CAPABILITY_COMPANION_ENABLED);
        boolean autoChatEnabled = refreshed != null
                && refreshed.getUid() != null
                && agentGovernanceService.isCapabilityEnabled(refreshed.getUid(), AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND);
        boolean requireVip = isRequireVip();
        String billingMode = agentUsageBillingService.getBillingMode(STATUS_SCENE_CODE);
        boolean billingEnabled = agentUsageBillingService.isBillingEnabled(STATUS_SCENE_CODE);
        int coinBalance = refreshed == null || refreshed.getUid() == null ? 0 : accountService.getCoinBalance(refreshed.getUid()).intValue();
        int freeQuota = billingEnabled ? agentUsageBillingService.getVipDailyFreeQuota(STATUS_SCENE_CODE) : 0;
        int freeRemaining = billingEnabled ? agentUsageBillingService.getVipDailyFreeRemaining(refreshed, STATUS_SCENE_CODE) : 0;
        int estimatedReplyCoin = billingEnabled ? agentUsageBillingService.estimateCoin(STATUS_SCENE_CODE, 160) : 0;
        String pauseReason = resolvePauseReason(requireVip, vipActive, companionEnabled, billingEnabled, freeRemaining, coinBalance, estimatedReplyCoin);
        return AgentCompanionStatusVo.builder()
                .available(StringUtils.isBlank(pauseReason))
                .requireVip(requireVip)
                .vipActive(vipActive)
                .vipExpireTime(refreshed == null ? null : refreshed.getVipExpireTime())
                .companionEnabled(companionEnabled)
                .autoChatEnabled(autoChatEnabled)
                .billingEnabled(billingEnabled)
                .billingMode(billingMode)
                .coinBalance(coinBalance)
                .vipDailyFreeQuota(freeQuota)
                .vipDailyFreeRemaining(freeRemaining)
                .estimatedReplyCoin(estimatedReplyCoin)
                .pauseReason(StringUtils.trimToNull(pauseReason))
                .build();
    }

    private AppUserEntity refreshUser(AppUserEntity currentUser) {
        if (currentUser == null) {
            return null;
        }
        return appUserService.vipExpirationCheck(currentUser);
    }

    private boolean isRequireVip() {
        return Boolean.TRUE.equals(businessConfigService.getBoolean("agent.companion.requireVip", true));
    }

    private boolean isVipActive(AppUserEntity user) {
        return user != null
                && Constant.VIP_USER.equals(user.getVip())
                && user.getVipExpireTime() != null
                && user.getVipExpireTime().after(new Date());
    }

    private String resolvePauseReason(boolean requireVip,
                                      boolean vipActive,
                                      boolean companionEnabled,
                                      boolean billingEnabled,
                                      int freeRemaining,
                                      int coinBalance,
                                      int estimatedReplyCoin) {
        if (requireVip && !vipActive) {
            return "当前不是有效VIP";
        }
        if (!companionEnabled) {
            return "恋爱助手总开关未开启";
        }
        if (billingEnabled && freeRemaining <= 0 && coinBalance < estimatedReplyCoin) {
            return "爱情币不足，自动能力将暂停";
        }
        return "";
    }
}
