
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "已读通知")
public class ReadNoticeForm {

    @Schema(description = "通知id")
    private Integer id;

}
