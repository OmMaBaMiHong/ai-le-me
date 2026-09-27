package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * AI 求爱视频脚本请求
 */
@Data
@Schema(name = "AgentVideoScriptForm", description = "AI视频脚本请求")
public class AgentVideoScriptForm implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "targetUid不能为空")
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetUid;

    @Schema(description = "视频模板编码", example = "dating_invite")
    private String templateCode = "self_intro";
}
