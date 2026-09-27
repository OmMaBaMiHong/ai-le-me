package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Agent 礼物策划请求
 */
@Data
@Schema(name = "AgentGiftPlanForm", description = "Agent 礼物策划请求")
public class AgentGiftPlanForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "targetUid不能为空")
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetUid;

    @Schema(description = "场景：social_intent/profile/video_feed/hongniang", example = "social_intent")
    private String scene = "social_intent";

    @Schema(description = "关系推进目标：break_ice/get_reply/exchange_wechat", example = "break_ice")
    private String objective = "break_ice";

    @Schema(description = "会话ID，可为空")
    private String sessionId;

    @Schema(description = "关联帖子ID，可为空")
    private Integer postId;

    @Schema(description = "预算档位，单位分", example = "[1314,52100,66600,168800]")
    private List<Integer> budgetOptions = Arrays.asList(1314, 52100, 66600, 168800);
}
