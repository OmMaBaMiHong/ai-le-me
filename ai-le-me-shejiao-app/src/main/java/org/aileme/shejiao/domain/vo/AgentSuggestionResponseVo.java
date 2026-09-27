package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Agent 建议响应
 */
@Data
@Builder
@Schema(name = "AgentSuggestionResponseVo", description = "Agent建议响应")
public class AgentSuggestionResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "场景")
    private String scene;

    @Schema(description = "模型提供商")
    private String provider;

    @Schema(description = "建议列表")
    private List<AgentSuggestionVo> suggestions;
}
