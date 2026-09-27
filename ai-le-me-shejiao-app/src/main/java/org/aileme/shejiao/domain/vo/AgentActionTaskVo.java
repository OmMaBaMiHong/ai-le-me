package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@Schema(name = "AgentActionTaskVo", description = "智能恋爱助手动作任务响应")
public class AgentActionTaskVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long taskId;

    private String requestId;

    private String capabilityCode;

    private String sceneCode;

    private String status;

    private Boolean approvalRequired;

    private Long approvalId;

    private String approvalStatus;

    private Integer targetUserId;

    private Long businessId;

    private String taskMessage;

    private String resultJson;
}
