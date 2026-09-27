package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 标签分类选项（按分类分组，用于前端标签选择页面）
 */
@Data
@Schema(description = "标签分类选项")
public class TagCategoryVo {

    @Schema(description = "分类名称")
    private String category;

    @Schema(description = "该分类下的标签列表")
    private List<UserTagOptionVo> tags;
}
