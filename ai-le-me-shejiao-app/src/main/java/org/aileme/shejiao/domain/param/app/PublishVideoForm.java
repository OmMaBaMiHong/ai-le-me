package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 发布视频请求表单
 *
 * @author system
 * @date 2026-02-13
 */
@Data
@Schema(name = "PublishVideoForm", description = "发布视频请求")
public class PublishVideoForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 圈子ID（可选，不传使用默认）
     */
    @Schema(description = "圈子ID（可选）")
    private Integer topicId;

    /**
     * 发布文案（可选）
     */
    @Schema(description = "发布文案")
    private String content;
}
