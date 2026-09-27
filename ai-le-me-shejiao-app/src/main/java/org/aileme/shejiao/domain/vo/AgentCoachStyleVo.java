package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@Schema(name = "AgentCoachStyleVo", description = "恋爱大师风格选择结果")
public class AgentCoachStyleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "风格编码")
    private String styleCode;

    @Schema(description = "风格名称")
    private String styleName;

    @Schema(description = "语气提示")
    private String toneHint;

    @Schema(description = "开场提示")
    private String openingHint;

    @Schema(description = "不建议使用的风格")
    private List<String> doNotUseStyles;

    @Schema(description = "推理摘要")
    private String reasoningSummary;
}
