
package org.aileme.shejiao.domain.param.sys;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(name="登录表单", description="登录表单")
public class SysLoginForm {

    @Schema(description = "用户名", required = true)
    private String username;

    @Schema(description = "密码", required = true)
    private String password;

    @Schema(description = "验证码", required = true)
    private String captcha;

    @Schema(description = "UUID", required = true)
    private String uuid;
    
    @Schema(description = "租户ID", required = false)
    private Long tenantId;

}
