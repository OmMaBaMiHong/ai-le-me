package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "申请成为好友")
public class ApplyFriendForm {

    @Schema(description = "申请消息内容")
    private Object data;

}
