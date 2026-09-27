package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "拒绝礼物申请请求体")
public class RejectGiftIntentForm {

    @Schema(description = "礼物请求唯一ID")
    private String requestId;

    @Schema(description = "礼物发起人UID")
    private Integer requesterUid;

    @Schema(description = "会话ID")
    private String sessionId;
}
