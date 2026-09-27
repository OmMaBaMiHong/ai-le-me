package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "AgentMessageAutoSendForm", description = "Agent 自动私聊发送请求")
public class AgentMessageAutoSendForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "targetUid不能为空")
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetUid;

    @Schema(description = "会话ID，可为空，服务端会按双方关系自动确定")
    private String sessionId;

    @NotBlank(message = "content不能为空")
    @Size(max = 255, message = "消息内容不能超过255个字符")
    @Schema(description = "待发送的私聊文案", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "消息来源，如 reply_suggestion / next_step / manual")
    private String source;
}
