package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "AgentSuggestedActionExecuteForm", description = "执行 Agent 建议动作请求")
public class AgentSuggestedActionExecuteForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "actionType不能为空")
    @Schema(description = "动作类型，如 message_auto_send / gift_plan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String actionType;

    @NotNull(message = "targetUid不能为空")
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetUid;

    @Schema(description = "透传的建议载荷 JSON")
    private String payloadJson;

    @Schema(description = "当前会话ID，可用于补充建议载荷里的 sessionId")
    private String sessionId;
}
