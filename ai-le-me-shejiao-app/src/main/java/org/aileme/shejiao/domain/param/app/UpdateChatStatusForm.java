
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "更新私聊消息状态请求体")
public class UpdateChatStatusForm {

    @Schema(description = "用户id")
    private Integer uid;

}
