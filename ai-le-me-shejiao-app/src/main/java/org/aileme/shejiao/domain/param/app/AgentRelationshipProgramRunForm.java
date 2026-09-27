package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(name = "AgentRelationshipProgramRunForm", description = "对象级自治程序运行请求")
public class AgentRelationshipProgramRunForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会话ID", example = "1987654321123")
    private String sessionId;

    @NotNull(message = "targetUid不能为空")
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetUid;

    @Schema(description = "触发器编码，如 incoming_message / hongniang_recommendation")
    private String triggerCode = "manual_check";

    @Schema(description = "最近对话上下文")
    private List<String> lastMessages;

    @Schema(description = "是否自动执行 runtime 返回的可执行动作")
    private Boolean autoExecute = false;

    @Schema(description = "红娘匹配分", example = "91")
    private Integer matchScore;

    @Schema(description = "起聊就绪度", example = "0.86")
    private Double openingReadinessScore;

    @Schema(description = "匹配标签")
    private List<String> fitTags;

    @Schema(description = "推荐破冰文案")
    private List<String> icebreakOpeners;

    @Schema(description = "推荐动作", example = "open_chat")
    private String recommendedAction;

    @Schema(description = "暂不建议起聊的原因")
    private String doNotOpenReason;
}
