package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "OnboardingAnswerForm", description = "首登画像单题回答")
public class OnboardingAnswerForm {

    @Schema(description = "题目编码")
    private String questionCode;

    @Schema(description = "选项编码")
    private String optionCode;

    @Schema(description = "选项文案")
    private String optionLabel;

    @Schema(description = "补充文本")
    private String freeText;
}
