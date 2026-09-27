package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * AI 标签推荐请求
 */
@Data
@Schema(name = "AgentTagSuggestionForm", description = "AI标签推荐请求")
public class AgentTagSuggestionForm implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标用户ID，默认当前登录用户")
    private Integer targetUid;

    @Min(value = 1, message = "limit最小为1")
    @Max(value = 10, message = "limit最大为10")
    @Schema(description = "返回推荐数量，1-10")
    private Integer limit = 6;
}
