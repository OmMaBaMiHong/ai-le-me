package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户标签可选项（用于印象标签选择等）
 */
@Data
@Schema(description = "用户标签可选项")
public class UserTagOptionVo {

    @Schema(description = "标签ID")
    private Integer id;

    @Schema(description = "标签名称")
    private String name;
}
