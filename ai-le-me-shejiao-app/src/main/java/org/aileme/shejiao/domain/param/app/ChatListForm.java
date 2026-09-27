
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "私聊列表请求体")
public class ChatListForm {

    @Schema(description = "接收用户uid")
    private Integer uid;

    @Schema(description = "page")
    private Integer page;

}
