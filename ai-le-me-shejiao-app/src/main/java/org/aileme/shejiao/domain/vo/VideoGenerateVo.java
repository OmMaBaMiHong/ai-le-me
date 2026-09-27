package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 视频生成结果VO
 *
 * @author system
 * @date 2026-02-13
 */
@Data
@Builder
@Schema(name = "VideoGenerateVo", description = "视频生成结果")
public class VideoGenerateVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "视频ID")
    private Integer videoId;

    @Schema(description = "预估生成时间(秒)")
    private Integer estimatedTime;

    @Schema(description = "生成的提示词")
    private String prompt;
}
