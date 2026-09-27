package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Agent 下一步策略响应
 */
@Data
@Builder
@Schema(name = "AgentNextStepVo", description = "Agent下一步策略响应")
public class AgentNextStepVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "模型提供商")
    private String provider;

    @Schema(description = "关系阶段")
    private String relationshipStage;

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

    @Schema(description = "下一步最佳动作")
    private String nextBestAction;

    @Schema(description = "未来24小时计划")
    private List<String> next24hPlan;

    @Schema(description = "护栏规则")
    private List<String> guardrails;

    @Schema(description = "结构化动作建议")
    private List<AgentNextStepActionVo> suggestedActions;

    @Schema(description = "是否建议自动执行")
    private Boolean shouldAutoExecute;
}
