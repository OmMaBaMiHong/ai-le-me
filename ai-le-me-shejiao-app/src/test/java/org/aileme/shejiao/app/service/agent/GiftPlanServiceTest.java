package org.aileme.shejiao.app.service.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.domain.vo.AgentGiftPlanVo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GiftPlanServiceTest {

    @Mock
    private AgentSafetyService safetyService;

    @Mock
    private SysConfigService configService;

    @InjectMocks
    private GiftPlanService giftPlanService;

    @Test
    public void catalogUsesConfiguredGiftOptions() {
        when(configService.getValue(GiftPlanService.GIFT_CATALOG_CONFIG_KEY)).thenReturn("""
                [
                  {
                    "code":"concert_ticket",
                    "name":"周末演出门票",
                    "desc":"先从共同兴趣开始靠近",
                    "scene":"兴趣邀约",
                    "icon":"🎫",
                    "theme":"show",
                    "templateCode":"dynamic_pet",
                    "animationPreset":"pet_idle",
                    "revealEffect":"pet_pop",
                    "soundEffectKey":"cat_meow",
                    "soundEffectUrl":"https://cdn.example.com/audio/cat-meow.mp3",
                    "amount":8800,
                    "recommended":true
                  },
                  {
                    "code":"dessert",
                    "name":"甜品小心意",
                    "desc":"轻一点也更自然",
                    "scene":"轻松破冰",
                    "icon":"🍰",
                    "theme":"sweet",
                    "amount":2200
                  }
                ]
                """);

        AgentGiftPlanVo result = giftPlanService.catalog();

        assertNotNull(result);
        assertEquals("gift_catalog_config", result.getProvider());
        assertEquals(2, result.getGifts().size());
        assertEquals("concert_ticket", result.getGifts().get(0).getCode());
        assertEquals(Integer.valueOf(8800), result.getGifts().get(0).getAmount());
        assertEquals("🎫", result.getGifts().get(0).getIcon());
        assertEquals("dynamic_pet", result.getGifts().get(0).getTemplateCode());
        assertEquals("pet_idle", result.getGifts().get(0).getAnimationPreset());
        assertEquals("pet_pop", result.getGifts().get(0).getRevealEffect());
        assertEquals("cat_meow", result.getGifts().get(0).getSoundEffectKey());
    }

    @Test
    public void catalogFallsBackToDefaultOptionsWhenConfigMissing() {
        when(configService.getValue(GiftPlanService.GIFT_CATALOG_CONFIG_KEY)).thenReturn("");

        AgentGiftPlanVo result = giftPlanService.catalog();

        assertNotNull(result);
        assertEquals("gift_catalog_default", result.getProvider());
        assertEquals(4, result.getGifts().size());
        assertEquals("milk_tea", result.getGifts().get(0).getCode());
        assertEquals(Integer.valueOf(1314), result.getGifts().get(0).getAmount());
        assertEquals("rose_bloom", result.getGifts().get(1).getCode());
        assertFalse(result.getGifts().isEmpty());
    }
}
