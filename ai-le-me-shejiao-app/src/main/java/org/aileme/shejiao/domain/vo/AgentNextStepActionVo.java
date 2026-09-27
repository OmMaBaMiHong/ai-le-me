package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@Schema(name = "AgentNextStepActionVo", description = "Agent下一步动作建议")
public class AgentNextStepActionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "动作类型，如 message_auto_send / gift_plan")
    private String actionType;

    @Schema(description = "对应能力编码")
    private String capabilityCode;

    @Schema(description = "动作标题")
    private String title;

    @Schema(description = "动作说明")
    private String summary;

    @Schema(description = "建议载荷 JSON")
    private String payloadJson;

    @Schema(description = "风险等级")
    private String riskLevel;

    @Schema(description = "建议执行模式 draft_only / request_approval / auto_if_permitted")
    private String executeMode;

    @Schema(description = "是否建议先审批")
    private Boolean requiresApproval;
}
