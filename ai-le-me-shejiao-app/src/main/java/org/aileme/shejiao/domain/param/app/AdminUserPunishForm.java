
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "处罚用户内容请求体")
public class AdminUserPunishForm {

    @Schema(description = "用户id")
    private Integer uid;

    @Schema(description = "0不处理1全部下架2全部删除")
    private Integer resetPost;

    @Schema(description = "0不处理1重置处理")
    private Integer resetAvatar;

    @Schema(description = "0不处理1重置处理")
    private Integer resetIntro;

    @Schema(description = "0不处理1重置处理")
    private Integer resetUsername;


}
