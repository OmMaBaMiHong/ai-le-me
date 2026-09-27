package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 视频状态VO
 *
 * @author system
 * @date 2026-02-13
 */
@Data
@Builder
@Schema(name = "VideoStatusVo", description = "视频状态")
public class VideoStatusVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态：0-生成中 1-待发布 2-已发布 3-失败")
    private Integer status;

    @Schema(description = "进度百分比")
    private Integer progress;

    @Schema(description = "视频URL（成功时返回）")
    private String videoUrl;

    @Schema(description = "封面URL")
    private String coverUrl;

    @Schema(description = "时长(秒)")
    private Integer duration;

    @Schema(description = "错误信息")
    private String errorMsg;
}
