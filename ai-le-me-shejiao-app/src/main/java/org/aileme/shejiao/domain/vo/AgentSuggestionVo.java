package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Agent 建议项
 */
@Data
@Builder
@Schema(name = "AgentSuggestionVo", description = "Agent建议项")
public class AgentSuggestionVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "建议ID")
    private String suggestionId;

    @Schema(description = "建议文案")
    private String text;

    @Schema(description = "风格标签")
    private String styleTag;

    @Schema(description = "风险等级：low/medium/high")
    private String riskLevel;

    @Schema(description = "生成理由")
    private String reason;

    @Schema(description = "下一步动作")
    private String nextAction;
}
