package org.aileme.shejiao.app.service.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.aileme.shejiao.api.service.AccountService;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.BillService;
import org.aileme.shejiao.api.service.FriendService;
import org.aileme.shejiao.api.service.PostService;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.api.service.UserLevelService;
import org.aileme.shejiao.app.dao.GiftTaskDao;
import org.aileme.shejiao.app.service.impl.SocialIntentMessageService;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.GiftTaskEntity;
import org.aileme.shejiao.domain.param.app.AgentGiftExecuteForm;
import org.aileme.shejiao.domain.vo.AgentGiftExecuteVo;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GiftExecuteServiceTest {

    @Mock
    private AppUserService appUserService;

    @Mock
    private PostService postService;

    @Mock
    private BillService billService;

    @Mock
    private AccountService accountService;

    @Mock
    private FriendService friendService;

    @Mock
    private GiftTaskDao giftTaskDao;

    @Mock
    private SocialIntentMessageService socialIntentMessageService;

    @Mock
    private UserLevelService userLevelService;

    @Mock
    private SysConfigService configService;

    @InjectMocks
    private GiftExecuteService giftExecuteService;

    @Test
    public void executeAllowsChatGiftWithoutPostId() {
      AppUserEntity currentUser = user(1001, "我");
      AppUserEntity targetUser = user(2002, "TA");
      AgentGiftExecuteForm form = new AgentGiftExecuteForm();
      form.setTargetUid(2002);
      form.setAmount(1314);
      form.setGiftCode("milk_tea");
      form.setGiftName("爱你的第一杯奶茶");
      form.setGiftIcon("🧋");
      form.setGiftScene("轻松破冰");
      form.setGiftTheme("tea");
      form.setTemplateCode("tea_coupon");
      form.setAnimationPreset("tea_float");
      form.setRevealEffect("bubble_pop");
      form.setSoundEffectKey("bubble_pop_fx");
      form.setSoundEffectUrl("https://cdn.example.com/audio/bubble-pop.mp3");
      form.setReason("想自然升温");
      form.setNote("这杯奶茶先替我问个好。");

      when(appUserService.getById(1001)).thenReturn(currentUser);
      when(appUserService.getById(2002)).thenReturn(targetUser);
      when(friendService.getOrCreateSession(1001, 2002)).thenReturn(5566L);
      when(accountService.getCoinBalance(1001)).thenReturn(BigDecimal.valueOf(5200));
      doAnswer(invocation -> {
          GiftTaskEntity entity = invocation.getArgument(0);
          entity.setId(7788L);
          return 1;
      }).when(giftTaskDao).insert(any(GiftTaskEntity.class));

      AgentGiftExecuteVo result = giftExecuteService.execute(currentUser, form);

      assertEquals("5566", form.getSessionId());
      assertEquals(7788L, result.getTaskId());
      assertEquals("pending_accept", result.getBusinessStatus());
      assertEquals("pending", result.getTaskStatus());
      verify(postService, never()).getById(any());
      verify(accountService).freezeCoin(1001, 1314);

      ArgumentCaptor<GiftTaskEntity> taskCaptor = ArgumentCaptor.forClass(GiftTaskEntity.class);
      verify(giftTaskDao).insert(taskCaptor.capture());
      assertNull(taskCaptor.getValue().getPostId());
      assertEquals("5566", taskCaptor.getValue().getSessionId());
      assertEquals("tea_coupon", taskCaptor.getValue().getTemplateCode());
      assertEquals("tea_float", taskCaptor.getValue().getAnimationPreset());
      assertEquals("bubble_pop", taskCaptor.getValue().getRevealEffect());
      assertEquals("bubble_pop_fx", taskCaptor.getValue().getSoundEffectKey());

      ArgumentCaptor<java.util.Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(java.util.Map.class);
      verify(socialIntentMessageService).sendStructuredMessage(
              eq("5566"),
              eq(1001),
              eq(2002),
              payloadCaptor.capture()
      );
      assertEquals("tea_coupon", payloadCaptor.getValue().get("tpl"));
      assertEquals("bubble_pop", payloadCaptor.getValue().get("reveal"));
      assertSame("爱你的第一杯奶茶", result.getGiftName());
      assertEquals("tea_coupon", result.getTemplateCode());
      assertEquals("tea_float", result.getAnimationPreset());
      assertEquals("bubble_pop", result.getRevealEffect());
      assertTrue(String.valueOf(result.getSoundEffectUrl()).contains("bubble-pop"));
    }

    private AppUserEntity user(int uid, String name) {
      AppUserEntity entity = new AppUserEntity();
      entity.setUid(uid);
      entity.setUsername(name);
      return entity;
    }
}
