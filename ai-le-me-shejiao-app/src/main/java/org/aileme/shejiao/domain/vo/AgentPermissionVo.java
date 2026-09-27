package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@Schema(name = "AgentPermissionVo", description = "智能恋爱助手权限响应")
public class AgentPermissionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String capabilityCode;

    private Integer enabled;

    private String authorizeMode;

    private String targetScope;

    private String riskLevel;

    private Integer maxAmountPerAction;

    private Integer maxAmountPerDay;

    private Integer maxActionsPerDay;

    private String quietHoursJson;

    private String policyJson;

    private String consentVersion;
}
