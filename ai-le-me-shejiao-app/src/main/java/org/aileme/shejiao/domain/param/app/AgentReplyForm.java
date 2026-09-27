package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * AI 回复建议请求
 */
@Data
@Schema(name = "AgentReplyForm", description = "AI回复建议请求")
public class AgentReplyForm implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "会话ID", example = "1987654321123")
    private String sessionId;

    @NotNull(message = "targetUid不能为空")
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetUid;

    @Schema(description = "最近对话上下文")
    private List<String> lastMessages;

    @Schema(description = "偏好语气：polite/active/sincere", example = "sincere")
    private String tone = "sincere";
}
