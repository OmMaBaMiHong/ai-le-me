
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "用户信息请求体")
public class AppUserInfoForm {

    @Schema(description = "用户id")
    private Integer uid;

}
