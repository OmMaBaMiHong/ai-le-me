package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@Schema(name = "AgentActionLogVo", description = "智能恋爱助手动作日志响应")
public class AgentActionLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long taskId;

    private Long approvalId;

    private String capabilityCode;

    private String eventType;

    private String eventStatus;

    private String sourceType;

    private String riskLevel;

    private String message;

    private String payloadJson;

    private Date createTime;
}
