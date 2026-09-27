package org.aileme.shejiao.app.service.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.aileme.shejiao.api.service.AccountService;
import org.aileme.shejiao.api.service.BillService;
import org.aileme.shejiao.api.service.PlatformBusinessConfigService;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgentUsageBillingServiceTest {

    @Mock
    private PlatformBusinessConfigService businessConfigService;

    @Mock
    private AccountService accountService;

    @Mock
    private BillService billService;

    @Mock
    private AgentCompanionQuotaStore agentCompanionQuotaStore;

    private AgentUsageBillingService agentUsageBillingService;

    @org.junit.jupiter.api.BeforeEach
    public void setUp() {
        AgentCoinBillingPlugin coinPlugin = new AgentCoinBillingPlugin(
                businessConfigService,
                accountService,
                billService,
                agentCompanionQuotaStore
        );
        AgentLogicOnlyBillingPlugin logicOnlyPlugin = new AgentLogicOnlyBillingPlugin();
        AgentBillingPluginRegistryService registry = new AgentBillingPluginRegistryService(List.of(coinPlugin, logicOnlyPlugin));
        agentUsageBillingService = new AgentUsageBillingService(registry, businessConfigService);
    }

    @Test
    public void settleUsageConsumesVipFreeQuotaBeforeChargingCoin() {
        AppUserEntity user = vipUser(1001);
        mockBillingEnabledConfig();
        mockVipFreeQuotaConfig();
        when(agentCompanionQuotaStore.getInt(anyString())).thenReturn(0);
        when(agentCompanionQuotaStore.increment(anyString(), any())).thenReturn(1);

        agentUsageBillingService.settleUsage(user, 2002, "reply_suggest", 320, "session-1");

        verify(accountService, never()).decreaseCoin(any(), any(Integer.class));
        verify(billService, never()).expend(any(), anyString(), anyString(), anyString(), any(Double.class), any(Double.class), anyString(), anyString(), any());
    }

    @Test
    public void settleUsageChargesCoinAfterVipFreeQuotaIsExhausted() {
        AppUserEntity user = vipUser(1001);
        mockEstimateBillingConfig();
        when(agentCompanionQuotaStore.getInt(anyString())).thenReturn(2);
        when(accountService.decreaseCoin(1001, 3)).thenReturn(BigDecimal.valueOf(17));

        agentUsageBillingService.settleUsage(user, 2002, "reply_suggest", 320, "session-1");

        verify(accountService).decreaseCoin(1001, 3);
        verify(billService).expend(
                eq(1001),
                eq("智能恋爱助手"),
                eq(BillDetailEnum.CATEGORY_2.getValue()),
                eq(BillDetailEnum.TYPE_22.getValue()),
                eq(3D),
                eq(17D),
                anyString(),
                eq("session-1"),
                eq(2002)
        );
    }

    @Test
    public void ensureSufficientBudgetRejectsWhenCoinIsNotEnoughAndNoFreeQuotaLeft() {
        AppUserEntity user = vipUser(1001);
        mockEstimateBillingConfig();
        when(agentCompanionQuotaStore.getInt(anyString())).thenReturn(2);
        when(accountService.getCoinBalance(1001)).thenReturn(BigDecimal.valueOf(2));

        assertThrows(LinfengException.class, () -> agentUsageBillingService.ensureSufficientBudget(user, "reply_suggest", 320));
    }

    @Test
    public void estimateCoinUsesConfiguredSceneMinimum() {
        mockCoinModeConfig();
        mockEstimateSceneConfig();

        int coin = agentUsageBillingService.estimateCoin("reply_suggest", 320);

        assertEquals(3, coin);
    }

    @Test
    public void settleUsageSkipsChargeWhenSceneUsesLogicOnlyMode() {
        AppUserEntity user = vipUser(1001);
        when(businessConfigService.getString("agent.companion.billing.scene.reply_suggest.mode", "")).thenReturn("logic_only");

        agentUsageBillingService.settleUsage(user, 2002, "reply_suggest", 320, "session-1");

        verify(agentCompanionQuotaStore, never()).increment(anyString(), any());
        verify(accountService, never()).decreaseCoin(any(), any(Integer.class));
        verify(billService, never()).expend(any(), anyString(), anyString(), anyString(), any(Double.class), any(Double.class), anyString(), anyString(), any());
    }

    @Test
    public void estimateCoinReturnsZeroWhenDefaultModeUsesLogicOnly() {
        when(businessConfigService.getString("agent.companion.billing.scene.reply_suggest.mode", "")).thenReturn("");
        when(businessConfigService.getString("agent.companion.billing.mode.default", "coin")).thenReturn("logic_only");

        assertEquals(0, agentUsageBillingService.estimateCoin("reply_suggest", 320));
        assertEquals(0, agentUsageBillingService.getVipDailyFreeQuota());
    }

    @Test
    public void estimateCoinDelegatesToCustomPluginWhenSceneModeMatchesPluginCode() {
        AgentBillingPlugin coinPlugin = mock(AgentBillingPlugin.class);
        AgentBillingPlugin sponsorPlugin = mock(AgentBillingPlugin.class);
        when(coinPlugin.modeCode()).thenReturn("coin");
        when(sponsorPlugin.modeCode()).thenReturn("sponsor");
        when(businessConfigService.getString("agent.companion.billing.scene.reply_suggest.mode", "")).thenReturn("sponsor");
        when(sponsorPlugin.estimateCoin("reply_suggest", 320)).thenReturn(9);

        AgentUsageBillingService service = new AgentUsageBillingService(
                new AgentBillingPluginRegistryService(List.of(coinPlugin, sponsorPlugin)),
                businessConfigService
        );

        assertEquals(9, service.estimateCoin("reply_suggest", 320));
        assertEquals("sponsor", service.getBillingMode("reply_suggest"));
        assertSame(sponsorPlugin, new AgentBillingPluginRegistryService(List.of(coinPlugin, sponsorPlugin)).resolve("sponsor"));
    }

    private void mockBillingEnabledConfig() {
        when(businessConfigService.getBoolean("agent.companion.billing.enabled", true)).thenReturn(true);
        mockCoinModeConfig();
    }

    private void mockCoinModeConfig() {
        when(businessConfigService.getString("agent.companion.billing.scene.reply_suggest.mode", "")).thenReturn("");
        when(businessConfigService.getString("agent.companion.billing.mode.default", "coin")).thenReturn("coin");
    }

    private void mockVipFreeQuotaConfig() {
        when(businessConfigService.getInt("agent.companion.billing.vipDailyFreeCalls", 20)).thenReturn(2);
    }

    private void mockEstimateBillingConfig() {
        mockBillingEnabledConfig();
        mockVipFreeQuotaConfig();
        mockEstimateSceneConfig();
    }

    private void mockEstimateSceneConfig() {
        when(businessConfigService.getInt("agent.companion.billing.inputCoinPer1kTokens", 1)).thenReturn(0);
        when(businessConfigService.getInt("agent.companion.billing.outputCoinPer1kTokens", 4)).thenReturn(0);
        when(businessConfigService.getInt("agent.companion.billing.minCoinPerCall", 1)).thenReturn(1);
        when(businessConfigService.getInt("agent.companion.billing.scene.reply_suggest.outputTokens", 220)).thenReturn(220);
        when(businessConfigService.getInt("agent.companion.billing.scene.reply_suggest.minCoin", 1)).thenReturn(3);
    }

    private AppUserEntity vipUser(int uid) {
        AppUserEntity entity = new AppUserEntity();
        entity.setUid(uid);
        entity.setVip(Constant.VIP_USER);
        entity.setVipExpireTime(new Date(System.currentTimeMillis() + 86_400_000L));
        entity.setIntegral(20);
        return entity;
    }
}
