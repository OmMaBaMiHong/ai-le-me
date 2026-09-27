package org.aileme.shejiao.app.runtime.compliance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.aileme.shejiao.api.service.PlatformBusinessConfigService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MiniAppFilingFeatureServiceTest {

    private PlatformBusinessConfigService businessConfigService;
    private MiniAppFilingFeatureService featureService;

    @BeforeEach
    void setUp() {
        businessConfigService = mock(PlatformBusinessConfigService.class);
        featureService = new MiniAppFilingFeatureService(businessConfigService);
    }

    @Test
    void shouldEnableFeatureWhenConfigMissing() {
        when(businessConfigService.getString(MiniAppFilingFeatureService.KEY_ENABLED, "0")).thenReturn("0");

        assertTrue(featureService.isFeatureEnabledForPlatform("mp-weixin", MiniAppFilingFeatureService.KEY_PAYMENT));
    }

    @Test
    void shouldEnableFeatureWhenIsOpenIsZero() {
        when(businessConfigService.getString(MiniAppFilingFeatureService.KEY_ENABLED, "0")).thenReturn("0");

        assertTrue(featureService.isFeatureEnabledForPlatform("mp-weixin", MiniAppFilingFeatureService.KEY_PAYMENT));
        assertTrue(featureService.isFeatureEnabledForPlatform("h5", MiniAppFilingFeatureService.KEY_PAYMENT));
    }

    @Test
    void shouldDisableFeatureWhenIsOpenIsOne() {
        when(businessConfigService.getString(MiniAppFilingFeatureService.KEY_ENABLED, "0")).thenReturn("1");

        org.junit.jupiter.api.Assertions.assertFalse(featureService.isFeatureEnabledForPlatform("h5", MiniAppFilingFeatureService.KEY_PAYMENT));
        org.junit.jupiter.api.Assertions.assertFalse(featureService.isFeatureEnabledForPlatform("app-plus", MiniAppFilingFeatureService.KEY_PAYMENT));
    }
}
