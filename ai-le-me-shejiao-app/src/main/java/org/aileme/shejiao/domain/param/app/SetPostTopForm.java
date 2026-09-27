
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "设置圈内置顶请求体")
public class SetPostTopForm {

    @Schema(description = "帖子id")
    private Integer postId;

    @Schema(description = "圈子id")
    private Integer topicId;

}
