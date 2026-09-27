package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "AgentPermissionSaveForm", description = "智能恋爱助手权限保存请求")
public class AgentPermissionSaveForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "capabilityCode不能为空")
    @Schema(description = "能力编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String capabilityCode;

    @NotNull(message = "enabled不能为空")
    @Schema(description = "是否启用:0否 1是", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer enabled;

    @Schema(description = "授权模式")
    private String authorizeMode;

    @Schema(description = "目标范围")
    private String targetScope;

    @Schema(description = "风险级别")
    private String riskLevel;

    @Schema(description = "单次金额上限")
    private Integer maxAmountPerAction;

    @Schema(description = "单日金额上限")
    private Integer maxAmountPerDay;

    @Schema(description = "单日动作次数上限")
    private Integer maxActionsPerDay;

    @Schema(description = "静默时段JSON")
    private String quietHoursJson;

    @Schema(description = "额外策略JSON")
    private String policyJson;

    @Schema(description = "协议版本")
    private String consentVersion;
}
