package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "OnboardingPersonaAssessForm", description = "首登关系画像蒸馏请求")
public class OnboardingPersonaAssessForm {

    @Schema(description = "回答列表")
    private List<OnboardingAnswerForm> answers = new ArrayList<>();
}
