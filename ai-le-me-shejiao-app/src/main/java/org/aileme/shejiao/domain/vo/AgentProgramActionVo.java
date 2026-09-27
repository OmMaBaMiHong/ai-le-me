package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@Schema(name = "AgentProgramActionVo", description = "关系程序建议动作")
public class AgentProgramActionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "动作类型")
    private String actionType;

    @Schema(description = "建议内容")
    private String content;

    @Schema(description = "目标用户ID")
    private Integer targetUserId;

    @Schema(description = "风险等级")
    private String riskLevel;

    @Schema(description = "是否需要审批")
    private Boolean requiresApproval;

    @Schema(description = "恋爱大师风格编码")
    private String masterStyleCode;

    @Schema(description = "执行模式")
    private String executeMode;

    @Schema(description = "透传载荷")
    private String payloadJson;
}
