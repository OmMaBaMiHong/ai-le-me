package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "AgentApprovalDecisionForm", description = "智能恋爱助手审批处理请求")
public class AgentApprovalDecisionForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "审批备注")
    private String note;
}
