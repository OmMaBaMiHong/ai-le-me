package org.aileme.shejiao.admin.service.impl;

import org.junit.jupiter.api.Test;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchRequestEntity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HongniangMatchCaseServiceImplTest {

    @Test
    public void matchesUserKeywordSupportsHongniangUserNoLookup() throws Exception {
        HongniangMatchCaseServiceImpl service = new HongniangMatchCaseServiceImpl(
            null, null, null, null, null, null, null, null, null, null, null, null
        );
        AppUserEntity user = new AppUserEntity();
        user.setUid(441);
        user.setUsername("林夏");
        user.setMobile("13800138000");

        Method method = HongniangMatchCaseServiceImpl.class.getDeclaredMethod(
            "matchesUserKeyword", AppUserEntity.class, String.class, Integer.class
        );
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(service, user, "12", 3124));
        assertTrue((Boolean) method.invoke(service, user, "1380013", 3124));
        assertTrue((Boolean) method.invoke(service, user, "林", 3124));
        assertFalse((Boolean) method.invoke(service, user, "9999", 3124));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void buildRequestRowUsesMpChannelLabel() throws Exception {
        AppUserService appUserService = mock(AppUserService.class);
        AppUserEntity fromUser = new AppUserEntity();
        fromUser.setUid(441);
        fromUser.setUsername("林夏");
        AppUserEntity toUser = new AppUserEntity();
        toUser.setUid(552);
        toUser.setUsername("苏晚");
        when(appUserService.listByIds(List.of(441, 552))).thenReturn(List.of(fromUser, toUser));

        HongniangMatchCaseServiceImpl service = new HongniangMatchCaseServiceImpl(
            null, null, null, null, appUserService, null, null, null, null, null, null, null
        );
        HongniangMatchRequestEntity request = new HongniangMatchRequestEntity();
        request.setCaseId(88);
        request.setFromUserId(441);
        request.setToUserId(552);
        request.setRequestChannel(3);

        Method method = HongniangMatchCaseServiceImpl.class.getDeclaredMethod(
            "buildRequestRow", HongniangMatchRequestEntity.class
        );
        method.setAccessible(true);

        Map<String, Object> row = (Map<String, Object>) method.invoke(service, request);
        assertEquals("公众号通知", row.get("requestChannelLabel"));
    }
}
