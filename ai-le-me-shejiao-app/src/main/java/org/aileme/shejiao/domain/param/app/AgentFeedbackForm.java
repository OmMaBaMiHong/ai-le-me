package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * Agent 反馈埋点请求
 */
@Data
@Schema(name = "AgentFeedbackForm", description = "Agent反馈埋点")
public class AgentFeedbackForm implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "scene不能为空")
    @Schema(description = "场景", requiredMode = Schema.RequiredMode.REQUIRED, example = "same_city")
    private String scene;

    @NotBlank(message = "suggestionId不能为空")
    @Schema(description = "建议ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String suggestionId;

    @NotBlank(message = "action不能为空")
    @Schema(description = "行为：expose/adopt/send/dislike", requiredMode = Schema.RequiredMode.REQUIRED)
    private String action;

    @Schema(description = "原因说明")
    private String reason;
}
