package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * AI 标签推荐项
 */
@Data
@Builder
@Schema(name = "AgentTagSuggestionVO", description = "AI标签推荐项")
public class AgentTagSuggestionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "建议ID")
    private String suggestionId;

    @Schema(description = "推荐标签")
    private String tag;

    @Schema(description = "推荐理由")
    private String reason;

    @Schema(description = "置信度(0~1)")
    private Double confidence;
}
