package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "同意好友请求")
public class AgreePersonForm {

    @Schema(description = "申请人 UID")
    private Integer id;

}
