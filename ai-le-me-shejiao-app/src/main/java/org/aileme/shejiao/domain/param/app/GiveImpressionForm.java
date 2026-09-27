package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 给用户打印象标签请求参数
 */
@Data
@Schema(name = "给用户打印象标签参数")
public class GiveImpressionForm {

    @Schema(description = "被打标签的用户ID")
    private Integer toUserId;

    @Schema(description = "标签ID列表")
    private List<Integer> tagIds;

    @Schema(description = "来源类型：PROFILE/CONTENT/COMMENT 等")
    private String sourceType;

    @Schema(description = "来源对象ID（帖子ID、评论ID等，可为空）")
    private Integer sourceId;
}
