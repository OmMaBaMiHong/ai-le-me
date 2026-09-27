package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * App 端埋点事件 DTO
 */
@Data
@Schema(name = "TrackEventDTO", description = "App埋点事件")
public class TrackEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "eventId不能为空")
    @Schema(description = "事件幂等ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String eventId;

    @NotBlank(message = "eventName不能为空")
    @Schema(description = "事件名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String eventName;

    @Schema(description = "业务模块")
    private String module;

    @Schema(description = "页面标识")
    private String page;

    @Schema(description = "用户ID（可空，后端会补当前登录用户）")
    private Integer uid;

    @Schema(description = "目标用户ID")
    private Integer targetUid;

    @Schema(description = "业务ID，例如tagId/postId")
    private String bizId;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "客户端时间戳（毫秒）")
    private Long clientTs;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "扩展属性")
    private Map<String, Object> props;
}
