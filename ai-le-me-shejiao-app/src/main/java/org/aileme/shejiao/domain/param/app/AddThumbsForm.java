
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "评论点赞")
public class AddThumbsForm {

    @Schema(description = "评论id")
    private Integer id;

}
