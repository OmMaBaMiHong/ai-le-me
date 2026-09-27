package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@Schema(name = "AgentRelationshipEvaluateVo", description = "关系状态评估结果")
public class AgentRelationshipEvaluateVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关系阶段")
    private String stageCode;

    @Schema(description = "热度分")
    private Double heatScore;

    @Schema(description = "信任分")
    private Double trustScore;

    @Schema(description = "语气温度")
    private Double toneWarmth;

    @Schema(description = "亲密度")
    private Double intimacyScore;

    @Schema(description = "推进度")
    private Double progressionScore;

    @Schema(description = "邀约就绪度")
    private Double dateReadyScore;

    @Schema(description = "微信交换就绪度")
    private Double wechatReadyScore;

    @Schema(description = "风险分")
    private Double riskScore;

    @Schema(description = "推荐恋爱大师风格")
    private String masterStyleCode;

    @Schema(description = "建议下一动作")
    private String recommendedNextAction;

    @Schema(description = "推理摘要")
    private String reasoningSummary;

    @Schema(description = "触发器提示")
    private List<String> triggerHints;
}
