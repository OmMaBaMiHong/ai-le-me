package org.aileme.shejiao.app.service.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.param.app.AgentGiftPlanForm;
import org.aileme.shejiao.domain.vo.AgentGiftPlanVo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 礼物 Agent 兜底策划服务
 */
@Service
public class GiftPlanService {

    public static final String GIFT_CATALOG_CONFIG_KEY = "agent_gift_catalog";

    private static final List<GiftTemplate> DEFAULT_TEMPLATES = Arrays.asList(
            new GiftTemplate("milk_tea", "爱你的第一杯奶茶", "先把气氛变甜一点", "轻松破冰", "🧋", "tea", "tea_coupon", "tea_float", "bubble_pop", "", "", 1314, true),
            new GiftTemplate("rose_bloom", "今晚这束花", "把今天的好感认真递给你", "表达认真", "🌹", "rose", "rose_growth", "rose_bloom", "petal_burst", "", "", 5200, false),
            new GiftTemplate("starlight", "想你的星光", "把今晚的好感点亮", "升温试探", "✨", "star", "starlight_orb", "orb_spin", "starburst", "", "", 13140, false),
            new GiftTemplate("moonlight", "晚安月光", "把今晚的温柔和晚安一起送给你", "轻柔陪伴", "🌙", "night", "moonbox_reveal", "moon_drift", "moon_glow", "", "", 3340, false)
    );

    @Autowired
    private AgentSafetyService safetyService;

    @Autowired(required = false)
    private SysConfigService configService;

    public AgentGiftPlanVo catalog() {
        List<GiftTemplate> templates = resolveTemplates();
        List<AgentGiftPlanVo.GiftOption> gifts = new ArrayList<>();
        boolean hasRecommended = templates.stream().anyMatch(item -> item.recommended);
        for (int i = 0; i < templates.size(); i++) {
            GiftTemplate template = templates.get(i);
            gifts.add(toCatalogOption(template, hasRecommended ? template.recommended : i == 0));
        }
        return AgentGiftPlanVo.builder()
                .scene("gift_catalog")
                .provider(resolveCatalogProvider())
                .relationshipStage("catalog")
                .strategyNote("礼物目录由后台配置提供，发送时再生成对应礼物内容。")
                .wechatPrompt("")
                .gifts(gifts)
                .build();
    }

    public AgentGiftPlanVo suggest(AppUserEntity me,
                                   AppUserEntity target,
                                   AgentGiftPlanForm form,
                                   boolean isFriend,
                                   String personaSummary) {
        String stage = isFriend ? "warming" : "early";
        String interest = pickInterest(target, personaSummary);
        String city = firstNonBlank(target.getLocationCity(), target.getCity(), target.getAbodeCity(), "同城");
        List<Integer> budgets = normalizeBudgets(form.getBudgetOptions());
        List<GiftTemplate> templates = resolveTemplates();
        List<AgentGiftPlanVo.GiftOption> gifts = new ArrayList<>();

        for (int i = 0; i < templates.size(); i++) {
            GiftTemplate template = templates.get(i);
            int amount = budgets.get(Math.min(i, budgets.size() - 1));
            gifts.add(AgentGiftPlanVo.GiftOption.builder()
                    .code(template.code)
                    .name(template.name)
                    .desc(template.desc)
                    .scene(template.scene)
                    .icon(template.icon)
                    .theme(template.theme)
                    .templateCode(template.templateCode)
                    .animationPreset(template.animationPreset)
                    .revealEffect(template.revealEffect)
                    .soundEffectKey(template.soundEffectKey)
                    .soundEffectUrl(template.soundEffectUrl)
                    .amount(amount)
                    .displayAmount(formatMoney(amount))
                    .messageDraft(safetyService.sanitizeText(buildMessageDraft(template, target, interest, city, stage), 60))
                    .rationale(safetyService.sanitizeText(buildRationale(template, interest, stage), 40))
                    .visualPrompt(buildVisualPrompt(me, target, template, interest, city))
                    .motionPrompt(buildMotionPrompt(me, target, template, interest, city))
                    .nextAction("open_chat")
                    .riskLevel("low")
                    .recommended(i == 0)
                    .build());
        }

        return AgentGiftPlanVo.builder()
                .scene(StringUtils.defaultIfBlank(form.getScene(), "social_intent"))
                .provider("gift_plan_fallback")
                .relationshipStage(stage)
                .strategyNote(buildStrategyNote(stage, interest))
                .wechatPrompt(buildWechatPrompt(target, interest, stage))
                .gifts(gifts)
                .build();
    }

    private List<Integer> normalizeBudgets(List<Integer> budgetOptions) {
        Set<Integer> normalized = new LinkedHashSet<>();
        if (budgetOptions != null) {
            for (Integer budgetOption : budgetOptions) {
                int amount = budgetOption == null ? 0 : budgetOption;
                if (amount >= 100 && amount <= 999999) {
                    normalized.add(amount);
                }
            }
        }
        if (normalized.isEmpty()) {
            for (GiftTemplate template : resolveTemplates()) {
                normalized.add(template.defaultAmount);
            }
        }
        return new ArrayList<>(normalized);
    }

    private AgentGiftPlanVo.GiftOption toCatalogOption(GiftTemplate template, boolean recommended) {
        String safeDesc = StringUtils.defaultIfBlank(template.desc, "先把诚意表达出来");
        return AgentGiftPlanVo.GiftOption.builder()
                .code(template.code)
                .name(template.name)
                .desc(safeDesc)
                .scene(StringUtils.defaultIfBlank(template.scene, "心动时刻"))
                .icon(StringUtils.defaultIfBlank(template.icon, "🎁"))
                .theme(StringUtils.defaultIfBlank(template.theme, "sunset"))
                .templateCode(StringUtils.defaultIfBlank(template.templateCode, "gift_box"))
                .animationPreset(StringUtils.defaultIfBlank(template.animationPreset, "gift_idle"))
                .revealEffect(StringUtils.defaultIfBlank(template.revealEffect, "gift_open"))
                .soundEffectKey(StringUtils.defaultIfBlank(template.soundEffectKey, ""))
                .soundEffectUrl(StringUtils.defaultIfBlank(template.soundEffectUrl, ""))
                .amount(template.defaultAmount)
                .displayAmount(formatMoney(template.defaultAmount))
                .messageDraft(safeDesc)
                .rationale(safeDesc)
                .visualPrompt("")
                .motionPrompt("")
                .nextAction("open_chat")
                .riskLevel("low")
                .recommended(recommended)
                .build();
    }

    private String resolveCatalogProvider() {
        return loadConfiguredTemplates().isEmpty() ? "gift_catalog_default" : "gift_catalog_config";
    }

    private List<GiftTemplate> resolveTemplates() {
        List<GiftTemplate> configured = loadConfiguredTemplates();
        return configured.isEmpty() ? DEFAULT_TEMPLATES : configured;
    }

    private List<GiftTemplate> loadConfiguredTemplates() {
        if (configService == null) {
            return java.util.Collections.emptyList();
        }
        String raw = configService.getValue(GIFT_CATALOG_CONFIG_KEY);
        if (StringUtils.isBlank(raw)) {
            return java.util.Collections.emptyList();
        }
        try {
            JSONArray array = JSON.parseArray(raw);
            if (array == null || array.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            List<GiftTemplate> result = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                GiftTemplate template = parseTemplate(item, i == 0);
                if (template != null) {
                    result.add(template);
                }
            }
            return result;
        } catch (Exception ex) {
            return java.util.Collections.emptyList();
        }
    }

    private GiftTemplate parseTemplate(JSONObject item, boolean fallbackRecommended) {
        if (item == null) {
            return null;
        }
        String code = StringUtils.trimToEmpty(item.getString("code"));
        String name = StringUtils.trimToEmpty(item.getString("name"));
        if (StringUtils.isBlank(code) || StringUtils.isBlank(name)) {
            return null;
        }
        int amount = item.getIntValue("amount");
        if (amount < 100 || amount > 999999) {
            return null;
        }
        return new GiftTemplate(
                code,
                name,
                StringUtils.defaultIfBlank(item.getString("desc"), name),
                StringUtils.defaultIfBlank(item.getString("scene"), "心动时刻"),
                StringUtils.defaultIfBlank(item.getString("icon"), "🎁"),
                StringUtils.defaultIfBlank(item.getString("theme"), "sunset"),
                StringUtils.defaultIfBlank(item.getString("templateCode"), mapTemplateCode(item.getString("theme"), code)),
                StringUtils.defaultIfBlank(item.getString("animationPreset"), mapAnimationPreset(item.getString("theme"))),
                StringUtils.defaultIfBlank(item.getString("revealEffect"), mapRevealEffect(item.getString("theme"))),
                StringUtils.defaultIfBlank(item.getString("soundEffectKey"), ""),
                StringUtils.defaultIfBlank(item.getString("soundEffectUrl"), ""),
                amount,
                item.containsKey("recommended") ? item.getBooleanValue("recommended") : fallbackRecommended
        );
    }

    private String mapTemplateCode(String theme, String code) {
        String normalizedTheme = StringUtils.defaultIfBlank(theme, "").trim().toLowerCase();
        String normalizedCode = StringUtils.defaultIfBlank(code, "").trim().toLowerCase();
        if (normalizedTheme.contains("tea") || normalizedCode.contains("tea")) {
            return "tea_coupon";
        }
        if (normalizedTheme.contains("rose") || normalizedCode.contains("rose")) {
            return "rose_growth";
        }
        if (normalizedTheme.contains("star") || normalizedCode.contains("star")) {
            return "starlight_orb";
        }
        if (normalizedTheme.contains("night") || normalizedTheme.contains("moon") || normalizedCode.contains("moon")) {
            return "moonbox_reveal";
        }
        return "gift_box";
    }

    private String mapAnimationPreset(String theme) {
        String normalizedTheme = StringUtils.defaultIfBlank(theme, "").trim().toLowerCase();
        if (normalizedTheme.contains("tea")) return "tea_float";
        if (normalizedTheme.contains("rose")) return "rose_bloom";
        if (normalizedTheme.contains("star")) return "orb_spin";
        if (normalizedTheme.contains("night") || normalizedTheme.contains("moon")) return "moon_drift";
        return "gift_idle";
    }

    private String mapRevealEffect(String theme) {
        String normalizedTheme = StringUtils.defaultIfBlank(theme, "").trim().toLowerCase();
        if (normalizedTheme.contains("tea")) return "bubble_pop";
        if (normalizedTheme.contains("rose")) return "petal_burst";
        if (normalizedTheme.contains("star")) return "starburst";
        if (normalizedTheme.contains("night") || normalizedTheme.contains("moon")) return "moon_glow";
        return "gift_open";
    }

    private String pickInterest(AppUserEntity target, String personaSummary) {
        String interest = firstNonBlank(target.getInterest(), target.getLoveDeclaration(), personaSummary, target.getIntro());
        interest = safetyService.sanitizeText(interest, 20);
        return StringUtils.defaultIfBlank(interest, "最近的生活");
    }

    private String buildMessageDraft(GiftTemplate template,
                                     AppUserEntity target,
                                     String interest,
                                     String city,
                                     String stage) {
        String targetName = safetyService.sanitizeText(firstNonBlank(target.getUsername(), "你"), 12);
        if ("warming".equals(stage)) {
            return "看到你最近关于" + interest + "的分享，想把这份" + template.name + "送给" + targetName + "，继续把这份好感认真一点。";
        }
        return "刷到你时一下就记住了" + city + "和" + interest + "这点，先把这份" + template.name + "送给你，想自然地和你聊聊。";
    }

    private String buildRationale(GiftTemplate template, String interest, String stage) {
        if ("warming".equals(stage)) {
            return "关系已经有一点熟悉感，适合用" + template.name + "把回应感拉高。";
        }
        return "先用轻量礼物承接" + interest + "这个话题，比直接硬聊更自然。";
    }

    private String buildStrategyNote(String stage, String interest) {
        if ("warming".equals(stage)) {
            return "当前更适合送一份有记忆点但不过分夸张的礼物，把“我记得你在意" + interest + "”这件事传递出来。";
        }
        return "当前关系还在早期，礼物应该轻一点、具体一点，重点不是砸金额，而是让对方感受到你是带着观察和诚意来的。";
    }

    private String buildWechatPrompt(AppUserEntity target, String interest, String stage) {
        String targetName = safetyService.sanitizeText(firstNonBlank(target.getUsername(), "你"), 12);
        if ("warming".equals(stage)) {
            return "和" + targetName + "聊到" + interest + "这件事时，可以顺势说一句：如果你也觉得聊得舒服，我们再交换微信。";
        }
        return "先围绕" + interest + "把聊天接起来，等" + targetName + "连续有回应后，再自然申请微信会更顺。";
    }

    private String buildVisualPrompt(AppUserEntity me,
                                     AppUserEntity target,
                                     GiftTemplate template,
                                     String interest,
                                     String city) {
        return safetyService.sanitizeText(
                firstNonBlank(me.getUsername(), "男生") + "在" + city + "的轻社交场景里，把" + template.name +
                        "递给" + firstNonBlank(target.getUsername(), "女生") + "，氛围和" + interest + "相关，真实生活感，东亚年轻人肖像。",
                160
        );
    }

    private String buildMotionPrompt(AppUserEntity me,
                                     AppUserEntity target,
                                     GiftTemplate template,
                                     String interest,
                                     String city) {
        return safetyService.sanitizeText(
                firstNonBlank(me.getUsername(), "男生") + "走向" + firstNonBlank(target.getUsername(), "女生") +
                        "，在" + city + "的夜晚或生活化场景里送出" + template.name + "，镜头自然推进，突出" + interest + "氛围和真诚互动。",
                160
        );
    }

    private String formatMoney(int amount) {
        BigDecimal value = BigDecimal.valueOf(Math.max(amount, 0L), 2).stripTrailingZeros();
        return value.scale() < 0 ? value.setScale(0).toPlainString() : value.toPlainString();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private static class GiftTemplate {
        private final String code;
        private final String name;
        private final String desc;
        private final String scene;
        private final String icon;
        private final String theme;
        private final String templateCode;
        private final String animationPreset;
        private final String revealEffect;
        private final String soundEffectKey;
        private final String soundEffectUrl;
        private final int defaultAmount;
        private final boolean recommended;

        private GiftTemplate(String code,
                             String name,
                             String desc,
                             String scene,
                             String icon,
                             String theme,
                             String templateCode,
                             String animationPreset,
                             String revealEffect,
                             String soundEffectKey,
                             String soundEffectUrl,
                             int defaultAmount,
                             boolean recommended) {
            this.code = code;
            this.name = name;
            this.desc = desc;
            this.scene = scene;
            this.icon = icon;
            this.theme = theme;
            this.templateCode = templateCode;
            this.animationPreset = animationPreset;
            this.revealEffect = revealEffect;
            this.soundEffectKey = soundEffectKey;
            this.soundEffectUrl = soundEffectUrl;
            this.defaultAmount = defaultAmount;
            this.recommended = recommended;
        }
    }
}
