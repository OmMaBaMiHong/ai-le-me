package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "删除指定好友")
public class DeleteFriendForm {

    @Schema(description = "好友 ID")
    private Integer id;

}
