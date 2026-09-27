
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "更新系统通知消息状态请求体")
public class UpdateSystemNoticeStatusForm {

    @Schema(description = "帖子id")
    private Integer id;

}
