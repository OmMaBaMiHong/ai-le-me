
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "支付付费贴请求体")
public class PayVipPostForm {

    @Schema(description = "帖子id")
    private Integer postId;

}
