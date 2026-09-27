package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@Schema(name = "AgentSuggestedActionExecuteVo", description = "执行 Agent 建议动作响应")
public class AgentSuggestedActionExecuteVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "动作类型")
    private String actionType;

    @Schema(description = "能力编码")
    private String capabilityCode;

    @Schema(description = "动作处理状态，如 succeeded / pending_approval / planned")
    private String status;

    @Schema(description = "是否需要审批")
    private Boolean approvalRequired;

    @Schema(description = "消息动作执行结果")
    private AgentMessageAutoSendVo messageAutoSend;

    @Schema(description = "礼物策划结果")
    private AgentGiftPlanVo giftPlan;
}
