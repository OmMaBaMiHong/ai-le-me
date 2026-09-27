package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * Agent 礼物执行请求
 */
@Data
@Schema(name = "AgentGiftExecuteForm", description = "Agent 礼物执行请求")
public class AgentGiftExecuteForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "targetUid不能为空")
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetUid;

    @Schema(description = "关联帖子ID，聊天场景可为空")
    private Integer postId;

    @NotNull(message = "amount不能为空")
    @Min(value = 1, message = "打赏金额必须大于0")
    @Max(value = 999999, message = "打赏金额不能超过999999")
    @Schema(description = "礼物金额，单位分/爱情币", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer amount;

    @NotBlank(message = "giftCode不能为空")
    @Schema(description = "礼物编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String giftCode;

    @NotBlank(message = "giftName不能为空")
    @Schema(description = "礼物名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String giftName;

    @Schema(description = "礼物图标")
    private String giftIcon;

    @Schema(description = "礼物场景标签")
    private String giftScene;

    @Schema(description = "礼物主题")
    private String giftTheme;

    @Schema(description = "前端礼物模板编码")
    private String templateCode;

    @Schema(description = "礼物动效预设")
    private String animationPreset;

    @Schema(description = "礼物拆盒特效预设")
    private String revealEffect;

    @Schema(description = "礼物音效键")
    private String soundEffectKey;

    @Schema(description = "礼物音效地址")
    private String soundEffectUrl;

    @Schema(description = "策略说明")
    private String strategyNote;

    @Schema(description = "关系阶段")
    private String relationshipStage;

    @Schema(description = "推荐理由")
    private String reason;

    @Schema(description = "送礼文案")
    private String note;

    @Schema(description = "配图提示词")
    private String visualPrompt;

    @Schema(description = "视频提示词")
    private String motionPrompt;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "规划提供方")
    private String provider;

    @Schema(description = "是否为用户主动触发，主动入口不受自动送礼开关限制")
    private Boolean manualTrigger = false;

    @Schema(description = "是否尝试立即生成礼物图")
    private Boolean generateImage = true;

    @Min(value = 1, message = "图片数量最少1张")
    @Max(value = 3, message = "图片数量最多3张")
    @Schema(description = "目标生成图片数量", example = "1")
    private Integer imageCount = 1;
}
