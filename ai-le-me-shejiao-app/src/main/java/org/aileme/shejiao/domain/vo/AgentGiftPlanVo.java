package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Agent 礼物策划响应
 */
@Data
@Builder
@Schema(name = "AgentGiftPlanVo", description = "Agent礼物策划响应")
public class AgentGiftPlanVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "场景")
    private String scene;

    @Schema(description = "模型提供商")
    private String provider;

    @Schema(description = "关系阶段")
    private String relationshipStage;

    @Schema(description = "礼物策略说明")
    private String strategyNote;

    @Schema(description = "申请微信时的建议文案")
    private String wechatPrompt;

    @Schema(description = "礼物选项")
    private List<GiftOption> gifts;

    @Data
    @Builder
    @Schema(name = "AgentGiftOptionVo", description = "Agent礼物选项")
    public static class GiftOption implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "礼物编码")
        private String code;

        @Schema(description = "礼物名称")
        private String name;

        @Schema(description = "礼物描述")
        private String desc;

        @Schema(description = "礼物场景标签")
        private String scene;

        @Schema(description = "礼物图标")
        private String icon;

        @Schema(description = "礼物主题")
        private String theme;

        @Schema(description = "前端模板编码，如 tea_coupon / rose_growth / dynamic_pet")
        private String templateCode;

        @Schema(description = "动效预设")
        private String animationPreset;

        @Schema(description = "拆礼物特效预设")
        private String revealEffect;

        @Schema(description = "音效键")
        private String soundEffectKey;

        @Schema(description = "音效地址")
        private String soundEffectUrl;

        @Schema(description = "金额，单位分")
        private Integer amount;

        @Schema(description = "格式化金额")
        private String displayAmount;

        @Schema(description = "赠送时的建议文案")
        private String messageDraft;

        @Schema(description = "推荐理由")
        private String rationale;

        @Schema(description = "配图提示词")
        private String visualPrompt;

        @Schema(description = "视频提示词")
        private String motionPrompt;

        @Schema(description = "下一步动作")
        private String nextAction;

        @Schema(description = "风险等级")
        private String riskLevel;

        @Schema(description = "是否推荐")
        private Boolean recommended;
    }
}
