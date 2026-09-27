package org.aileme.shejiao.app.biz;

import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.WxMpTemplateMsgService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.aileme.shejiao.api.service.WechatRuntimeConfigService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchRequestEntity;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HongniangMpNoticeServiceTest {

    @Mock
    private WxMpService wxMpService;

    @Mock
    private WxMpTemplateMsgService wxMpTemplateMsgService;

    @Mock
    private WechatRuntimeConfigService wechatRuntimeConfigService;

    @InjectMocks
    private HongniangMpNoticeService hongniangMpNoticeService;

    @Test
    public void sendMatchRequestNoticeThrowsWhenTargetUserMissingMpOpenid() {
        AppUserEntity targetUser = new AppUserEntity();
        targetUser.setUid(2002);
        targetUser.setUsername("林小满");

        LinfengException exception = assertThrows(LinfengException.class, () ->
            hongniangMpNoticeService.sendMatchRequestNotice(targetUser, requester(), hongniang(), request())
        );

        assertEquals("目标用户未绑定公众号，无法发送牵线通知", exception.getMsg());
    }

    @Test
    public void sendMatchRequestNoticeUsesWechatTemplateMessage() throws Exception {
        when(wechatRuntimeConfigService.getMpMatchRequestTemplateId()).thenReturn("TPL_MATCH_001");
        when(wechatRuntimeConfigService.getMpMatchRequestDetailUrl()).thenReturn("https://h5.ai-ni.store/match/detail");
        when(wxMpService.getTemplateMsgService()).thenReturn(wxMpTemplateMsgService);

        hongniangMpNoticeService.sendMatchRequestNotice(targetUser(), requester(), hongniang(), request());

        ArgumentCaptor<WxMpTemplateMessage> captor = ArgumentCaptor.forClass(WxMpTemplateMessage.class);
        verify(wxMpTemplateMsgService).sendTemplateMsg(captor.capture());
        WxMpTemplateMessage message = captor.getValue();
        assertEquals("TPL_MATCH_001", message.getTemplateId());
        assertEquals("mp-openid-2002", message.getToUser());
        assertEquals(true, message.getUrl().contains("requestId=req_123456"));
        assertEquals(4, message.getData().size());
    }

    private AppUserEntity requester() {
        AppUserEntity user = new AppUserEntity();
        user.setUid(1001);
        user.setUsername("程一诺");
        user.setMobile("13800138000");
        return user;
    }

    private AppUserEntity targetUser() {
        AppUserEntity user = new AppUserEntity();
        user.setUid(2002);
        user.setUsername("林小满");
        user.setMpOpenid("mp-openid-2002");
        return user;
    }

    private HongniangInfoEntity hongniang() {
        HongniangInfoEntity entity = new HongniangInfoEntity();
        entity.setId(19);
        entity.setHongniangName("苏老师");
        return entity;
    }

    private HongniangMatchRequestEntity request() {
        HongniangMatchRequestEntity entity = new HongniangMatchRequestEntity();
        entity.setId(88);
        entity.setCaseId(66);
        entity.setIntentRequestId("req_123456");
        entity.setRequestMessage("想认真认识你，如果你愿意我们可以先从了解开始。");
        entity.setExpireTime(new Date(1760000000000L));
        return entity;
    }
}
