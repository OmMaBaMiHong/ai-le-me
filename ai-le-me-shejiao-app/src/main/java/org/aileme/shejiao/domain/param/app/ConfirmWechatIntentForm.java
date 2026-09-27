package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "确认微信申请请求体")
public class ConfirmWechatIntentForm {

    @Schema(description = "申请唯一ID")
    private String requestId;

    @Schema(description = "申请发起人UID")
    private Integer requesterUid;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "诚意积分")
    private Integer amount;

    @Schema(description = "接收方微信号")
    private String wechatNo;
}
