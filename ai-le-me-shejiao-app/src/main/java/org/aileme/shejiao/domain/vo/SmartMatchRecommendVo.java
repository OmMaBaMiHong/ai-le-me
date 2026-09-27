package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "SmartMatchRecommendVo", description = "智能红娘推荐结果")
public class SmartMatchRecommendVo {

    private String provider;
    private String insight;
    private String settingSummary;
    @Builder.Default
    private List<String> selectedCities = new ArrayList<>();
    private Boolean usedRuntime;
    @Builder.Default
    private List<SmartMatchCandidateVo> data = new ArrayList<>();
}
