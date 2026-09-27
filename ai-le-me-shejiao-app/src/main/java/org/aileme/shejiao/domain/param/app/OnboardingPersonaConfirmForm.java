package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "OnboardingPersonaConfirmForm", description = "首登关系画像确认请求")
public class OnboardingPersonaConfirmForm {

    @Schema(description = "是否应用画像生成的资料文案")
    private Boolean applyProfileDraft = Boolean.TRUE;

    @Schema(description = "是否应用候选标签")
    private Boolean applyTagCandidates = Boolean.TRUE;
}
