package org.aileme.shejiao.app.service.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.aileme.shejiao.api.service.AccountService;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.PlatformBusinessConfigService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.vo.AgentCompanionStatusVo;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgentAccessPolicyServiceTest {

    @Mock
    private AppUserService appUserService;

    @Mock
    private AgentGovernanceService agentGovernanceService;

    @Mock
    private PlatformBusinessConfigService businessConfigService;

    @Mock
    private AccountService accountService;

    @Mock
    private AgentUsageBillingService agentUsageBillingService;

    @InjectMocks
    private AgentAccessPolicyService agentAccessPolicyService;

    @Test
    public void requireCompanionAccessRejectsNonVipUsersWhenVipIsRequired() {
        AppUserEntity user = user(1001, Constant.COMMON_USER);
        when(appUserService.vipExpirationCheck(user)).thenReturn(user);
        when(businessConfigService.getBoolean("agent.companion.requireVip", true)).thenReturn(true);

        assertThrows(LinfengException.class, () -> agentAccessPolicyService.requireCompanionAccess(user));
    }

    @Test
    public void requireCompanionAccessRejectsWhenMasterSwitchIsDisabled() {
        AppUserEntity user = user(1001, Constant.VIP_USER);
        when(appUserService.vipExpirationCheck(user)).thenReturn(user);
        when(businessConfigService.getBoolean("agent.companion.requireVip", true)).thenReturn(true);
        when(agentGovernanceService.isCapabilityEnabled(1001, AgentGovernanceConstants.CAPABILITY_COMPANION_ENABLED)).thenReturn(false);

        assertThrows(LinfengException.class, () -> agentAccessPolicyService.requireCompanionAccess(user));
    }

    @Test
    public void getStatusSummarizesVipSwitchQuotaAndCoinBalance() {
        AppUserEntity user = user(1001, Constant.VIP_USER);
        when(appUserService.vipExpirationCheck(user)).thenReturn(user);
        when(businessConfigService.getBoolean("agent.companion.requireVip", true)).thenReturn(true);
        when(agentGovernanceService.isCapabilityEnabled(1001, AgentGovernanceConstants.CAPABILITY_COMPANION_ENABLED)).thenReturn(true);
        when(agentGovernanceService.isCapabilityEnabled(1001, AgentGovernanceConstants.CAPABILITY_MESSAGE_AUTO_SEND)).thenReturn(true);
        when(accountService.getCoinBalance(1001)).thenReturn(BigDecimal.valueOf(88));
        when(agentUsageBillingService.getBillingMode("reply_suggest")).thenReturn("coin");
        when(agentUsageBillingService.isBillingEnabled("reply_suggest")).thenReturn(true);
        when(agentUsageBillingService.getVipDailyFreeQuota("reply_suggest")).thenReturn(20);
        when(agentUsageBillingService.getVipDailyFreeRemaining(user, "reply_suggest")).thenReturn(7);
        when(agentUsageBillingService.estimateCoin("reply_suggest", 160)).thenReturn(3);

        AgentCompanionStatusVo status = agentAccessPolicyService.getStatus(user);

        assertTrue(Boolean.TRUE.equals(status.getVipActive()));
        assertTrue(Boolean.TRUE.equals(status.getCompanionEnabled()));
        assertTrue(Boolean.TRUE.equals(status.getAutoChatEnabled()));
        assertEquals(88, status.getCoinBalance());
        assertEquals(20, status.getVipDailyFreeQuota());
        assertEquals(7, status.getVipDailyFreeRemaining());
        assertEquals(3, status.getEstimatedReplyCoin());
        assertEquals("coin", status.getBillingMode());
    }

    private AppUserEntity user(int uid, int vip) {
        AppUserEntity entity = new AppUserEntity();
        entity.setUid(uid);
        entity.setVip(vip);
        entity.setVipExpireTime(new Date(System.currentTimeMillis() + 86_400_000L));
        entity.setIntegral(100);
        return entity;
    }
}
