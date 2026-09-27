
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "已读")
public class ClearChatMessageUnreadForm {

    @Schema(description = "我的用户id")
    private String myId;

    @Schema(description = "好友用户id")
    private String friendId;

}
