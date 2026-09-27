
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "帖子点赞")
public class AddCollectionForm {

    @Schema(description = "帖子id")
    private Integer id;

    @Schema(description = "用户id")
    private Integer uid;

}
