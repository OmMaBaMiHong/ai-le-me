package org.aileme.shejiao.admin.service.group;

import org.aileme.system.service.ISysThirdPartyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.aileme.shejiao.domain.entity.admin.HongniangGroupTouchTaskEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangWechatGroupEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WecomGroupTouchProviderTest {

    @Mock
    private ISysThirdPartyService thirdPartyService;

    @Mock
    private WecomCustomerApiClient wecomCustomerApiClient;

    @InjectMocks
    private WecomGroupTouchProvider provider;

    @Test
    @SuppressWarnings("unchecked")
    public void syncGroupUsesOfficialGroupChatResponse() {
        when(thirdPartyService.getConfigValue("wecom_customer", "corpId")).thenReturn("ww-test");
        when(thirdPartyService.getConfigValue("wecom_customer", "corpSecret")).thenReturn("secret-test");
        when(wecomCustomerApiClient.fetchAccessToken("ww-test", "secret-test")).thenReturn("access-token-1");
        when(wecomCustomerApiClient.getGroupChat("access-token-1", "chat-group-001")).thenReturn(Map.of(
            "group_chat", Map.of(
                "name", "上海红娘A群",
                "owner", "hongniang_admin",
                "member_list", java.util.List.of(Map.of("userid", "uid-1"), Map.of("userid", "uid-2"))
            )
        ));

        HongniangWechatGroupEntity group = new HongniangWechatGroupEntity();
        group.setExternalGroupId("chat-group-001");

        ProviderResult result = provider.syncGroup(group);

        assertTrue(result.isSuccess());
        assertEquals("已同步企微客户群：上海红娘A群（2人）", result.getSummary());
        assertNotNull(result.getData());
        Map<String, Object> data = result.getData();
        assertEquals("上海红娘A群", data.get("groupName"));
        assertEquals("hongniang_admin", data.get("ownerWechat"));
        assertEquals(2, data.get("memberCount"));
    }

    @Test
    public void submitTaskCreatesRealWecomMassTask() {
        when(thirdPartyService.getConfigValue("wecom_customer", "corpId")).thenReturn("ww-test");
        when(thirdPartyService.getConfigValue("wecom_customer", "corpSecret")).thenReturn("secret-test");
        when(wecomCustomerApiClient.fetchAccessToken("ww-test", "secret-test")).thenReturn("access-token-2");
        when(wecomCustomerApiClient.addGroupMassMessage(
            org.mockito.ArgumentMatchers.eq("access-token-2"),
            org.mockito.ArgumentMatchers.argThat(payload ->
                "group".equals(payload.get("chat_type"))
                    && java.util.List.of("chat-group-002").equals(payload.get("chat_id_list"))
                    && "hongniang_owner".equals(payload.get("sender"))
            )
        )).thenReturn(Map.of("msgid", "msgid-778899"));

        HongniangWechatGroupEntity group = new HongniangWechatGroupEntity();
        group.setExternalGroupId("chat-group-002");
        group.setOwnerWechat("hongniang_owner");

        HongniangGroupTouchTaskEntity task = new HongniangGroupTouchTaskEntity();
        task.setTaskName("案例进群通知");
        task.setContentType(1);
        task.setContentPayload("今晚八点线上破冰，请准时参加。");

        ProviderResult result = provider.submitTask(group, task);

        assertTrue(result.isSuccess());
        assertEquals("msgid-778899", result.getProviderTaskId());
        assertEquals("企业微信群发任务创建成功", result.getSummary());
    }
}
