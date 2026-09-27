package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * AI 关系推进策略请求
 */
@Data
@Schema(name = "AgentNextStepForm", description = "AI关系推进策略请求")
public class AgentNextStepForm implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "会话ID", example = "1987654321123")
    private String sessionId;

    @NotNull(message = "targetUid不能为空")
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetUid;

    @Schema(description = "最近对话上下文")
    private List<String> lastMessages;

    @Schema(description = "策略目标", example = "move_relationship_forward")
    private String objective = "move_relationship_forward";
}
