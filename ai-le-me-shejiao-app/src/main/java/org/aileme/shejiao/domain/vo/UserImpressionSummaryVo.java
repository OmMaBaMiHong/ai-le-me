package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户印象标签聚合结果
 */
@Data
@Schema(description = "用户印象标签聚合结果")
public class UserImpressionSummaryVo {

    @Schema(description = "标签ID")
    private Integer id;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "被打标次数")
    private Integer count;
}
