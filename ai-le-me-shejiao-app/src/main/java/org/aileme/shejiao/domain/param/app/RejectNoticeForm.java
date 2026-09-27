
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "拒绝通知")
public class RejectNoticeForm {

    @Schema(description = "通知id")
    private Integer id;

    @Schema(description = "发送方")
    private String senderName;

    @Schema(description = "对方头像")
    private String senderAvatar;

}
