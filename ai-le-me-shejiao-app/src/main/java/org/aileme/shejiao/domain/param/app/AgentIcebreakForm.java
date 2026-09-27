package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * AI 破冰建议请求
 */
@Data
@Schema(name = "AgentIcebreakForm", description = "AI破冰建议请求")
public class AgentIcebreakForm implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "targetUid不能为空")
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetUid;

    @Schema(description = "场景：same_city/visitors/fans/session", example = "same_city")
    private String scene = "same_city";

    @Min(value = 1, message = "limit最小为1")
    @Max(value = 5, message = "limit最大为5")
    @Schema(description = "返回数量，1-5", example = "3")
    private Integer limit = 3;
}
