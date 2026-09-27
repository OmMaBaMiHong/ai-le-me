package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@Schema(name = "AgentProgramRunVo", description = "对象级自治程序运行结果")
public class AgentProgramRunVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "工作流编码")
    private String workflowCode;

    @Schema(description = "关系阶段")
    private String stageCode;

    @Schema(description = "当前风格")
    private String masterStyleCode;

    @Schema(description = "下一步动作")
    private AgentProgramActionVo nextAction;

    @Schema(description = "是否已自动执行")
    private Boolean autoExecuted;

    @Schema(description = "动作执行结果")
    private AgentSuggestedActionExecuteVo execution;

    @Schema(description = "自动执行失败信息")
    private String executionErrorMessage;

    @Schema(description = "未来24小时计划")
    private List<String> next24hPlan;

    @Schema(description = "推理摘要")
    private String reasoningSummary;
}
