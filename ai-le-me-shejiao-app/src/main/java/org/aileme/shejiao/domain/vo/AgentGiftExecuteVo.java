package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Agent 礼物执行响应
 */
@Data
@Builder
@Schema(name = "AgentGiftExecuteVo", description = "Agent礼物执行响应")
public class AgentGiftExecuteVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "礼物任务ID")
    private Long taskId;

    @Schema(description = "礼物请求标识")
    private String requestId;

    @Schema(description = "金额")
    private Integer amount;

    @Schema(description = "礼物名称")
    private String giftName;

    @Schema(description = "礼物模板编码")
    private String templateCode;

    @Schema(description = "礼物动效预设")
    private String animationPreset;

    @Schema(description = "礼物拆盒特效预设")
    private String revealEffect;

    @Schema(description = "礼物音效键")
    private String soundEffectKey;

    @Schema(description = "礼物音效地址")
    private String soundEffectUrl;

    @Schema(description = "私聊会话ID")
    private String sessionId;

    @Schema(description = "礼物业务状态 pending_accept/accepted/rejected/expired")
    private String businessStatus;

    @Schema(description = "任务状态 pending/success/failed")
    private String taskStatus;

    @Schema(description = "任务说明")
    private String taskMessage;

    @Schema(description = "生成的礼物图片")
    private List<String> assetUrls;

    @Schema(description = "当前剩余爱情币")
    private Integer currentBalance;

    @Schema(description = "是否已立即生成成功")
    private Boolean generated;

    @Schema(description = "是否需要审批")
    private Boolean approvalRequired;

    @Schema(description = "治理任务ID")
    private Long governanceTaskId;

    @Schema(description = "审批ID")
    private Long approvalId;

    @Schema(description = "审批状态 pending/approved/rejected")
    private String approvalStatus;
}
