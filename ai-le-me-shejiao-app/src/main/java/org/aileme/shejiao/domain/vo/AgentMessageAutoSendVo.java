package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@Schema(name = "AgentMessageAutoSendVo", description = "Agent自动私聊发送响应")
public class AgentMessageAutoSendVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消息ID")
    private Integer messageId;

    @Schema(description = "请求标识")
    private String requestId;

    @Schema(description = "目标用户ID")
    private Integer targetUid;

    @Schema(description = "实际会话ID")
    private String sessionId;

    @Schema(description = "发送内容")
    private String content;

    @Schema(description = "动作来源")
    private String source;

    @Schema(description = "任务状态")
    private String taskStatus;

    @Schema(description = "任务说明")
    private String taskMessage;

    @Schema(description = "是否需要审批")
    private Boolean approvalRequired;

    @Schema(description = "治理任务ID")
    private Long governanceTaskId;

    @Schema(description = "审批ID")
    private Long approvalId;

    @Schema(description = "审批状态 pending/approved/rejected")
    private String approvalStatus;
}
