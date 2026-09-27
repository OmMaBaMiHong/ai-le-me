package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(name = "AgentRelationshipEvaluateForm", description = "对象级关系状态评估请求")
public class AgentRelationshipEvaluateForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会话ID", example = "1987654321123")
    private String sessionId;

    @NotNull(message = "targetUid不能为空")
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetUid;

    @Schema(description = "最近对话上下文")
    private List<String> lastMessages;
}
