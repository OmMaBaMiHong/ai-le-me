package org.aileme.shejiao.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.common.utils.R;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppSystemConfigControllerTest {

    @Test
    void shouldDefaultIsOpenToEnabledWhenConfigMissing() {
        SysConfigService configService = mock(SysConfigService.class);
        when(configService.getValue(Constant.IS_OPEN)).thenReturn("");

        AppSystemConfigController controller = new AppSystemConfigController();
        ReflectionTestUtils.setField(controller, "configService", configService);

        R result = controller.getSysConfig(Constant.IS_OPEN);

        assertEquals("0", result.get("result"));
    }

    @Test
    void shouldMapMiniAppFilingAliasToIsOpen() {
        SysConfigService configService = mock(SysConfigService.class);
        when(configService.getValue(Constant.IS_OPEN)).thenReturn("1");

        AppSystemConfigController controller = new AppSystemConfigController();
        ReflectionTestUtils.setField(controller, "configService", configService);

        R result = controller.getSysConfig("miniapp.filing.paymentEnabled");

        assertEquals("1", result.get("result"));
        verify(configService).getValue(Constant.IS_OPEN);
    }
}
