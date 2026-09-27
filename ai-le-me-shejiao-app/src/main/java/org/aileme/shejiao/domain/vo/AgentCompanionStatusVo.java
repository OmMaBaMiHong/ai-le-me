package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@Schema(name = "AgentCompanionStatusVo", description = "智能恋爱助手状态摘要")
public class AgentCompanionStatusVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否可用")
    private Boolean available;

    @Schema(description = "是否要求VIP")
    private Boolean requireVip;

    @Schema(description = "当前是否为有效VIP")
    private Boolean vipActive;

    @Schema(description = "VIP过期时间")
    private Date vipExpireTime;

    @Schema(description = "恋爱助手总开关是否开启")
    private Boolean companionEnabled;

    @Schema(description = "自动代聊开关是否开启")
    private Boolean autoChatEnabled;

    @Schema(description = "是否启用扣费")
    private Boolean billingEnabled;

    @Schema(description = "当前计费模式")
    private String billingMode;

    @Schema(description = "当前爱情币余额")
    private Integer coinBalance;

    @Schema(description = "VIP每日免费调用次数")
    private Integer vipDailyFreeQuota;

    @Schema(description = "VIP今日剩余免费调用次数")
    private Integer vipDailyFreeRemaining;

    @Schema(description = "回复场景预估消耗爱情币")
    private Integer estimatedReplyCoin;

    @Schema(description = "不可用原因")
    private String pauseReason;
}
