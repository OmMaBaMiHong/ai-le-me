
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录表单
 *
 */
@Data
@Schema(name = "登录表单")
public class LoginForm {
    @Schema(description = "手机号", required = true)
    @NotBlank(message="手机号不能为空")
    private String mobile;

    @Schema(description = "密码", required = true)
    @NotBlank(message="密码不能为空")
    private String password;

}
