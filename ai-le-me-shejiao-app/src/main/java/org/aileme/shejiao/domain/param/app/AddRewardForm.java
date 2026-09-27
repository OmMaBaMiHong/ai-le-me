
package org.aileme.shejiao.domain.param.app;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "打赏积分请求体")
public class AddRewardForm {

    @Schema(name = "打赏数量")
    private Integer rewardCount;

    @Schema(name = "帖子id")
    private Integer postId;
}
