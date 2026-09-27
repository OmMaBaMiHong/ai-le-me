package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 视频模板VO
 *
 * @author system
 * @date 2026-02-13
 */
@Data
@Schema(name = "VideoTemplateVo", description = "视频模板")
public class VideoTemplateVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Integer id;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模板编码")
    private String code;

    @Schema(description = "场景分类")
    private String category;

    @Schema(description = "场景分类名称")
    private String categoryName;

    @Schema(description = "模板描述")
    private String description;

    @Schema(description = "模板封面预览图")
    private String coverUrl;

    @Schema(description = "风格预设")
    private String stylePreset;

    @Schema(description = "时长范围(秒)")
    private String durationRange;

    @Schema(description = "最大使用图片数")
    private Integer maxImages;
}
