
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "消息已读")
public class MessageReadForm {

    @Schema(description = "帖子id")
    private Integer postId;

    @Schema(description = "消息id")
    private Integer mid;

}
