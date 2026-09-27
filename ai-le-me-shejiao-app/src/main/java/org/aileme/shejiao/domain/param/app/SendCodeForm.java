
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "登录验证码")
public class SendCodeForm {

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "email")
    private String email;


}
