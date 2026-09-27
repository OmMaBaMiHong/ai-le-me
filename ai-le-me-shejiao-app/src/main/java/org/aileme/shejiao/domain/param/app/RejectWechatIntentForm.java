package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "拒绝微信申请请求体")
public class RejectWechatIntentForm {

    @Schema(description = "申请唯一ID")
    private String requestId;

    @Schema(description = "申请发起人UID")
    private Integer requesterUid;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "拒绝理由")
    private String reason;
}
