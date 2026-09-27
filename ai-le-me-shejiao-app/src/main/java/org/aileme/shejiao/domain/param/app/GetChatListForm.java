
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "IM私聊列表")
public class GetChatListForm {

    @Schema(description = "标识")
    private String sessionId;

    @Schema(description = "最后一条消息id")
    private String lastMessageId;

    @Schema(description = "页数")
    private Integer pageNum;

    @Schema(description = "页码")
    private Integer pageSize;



}
