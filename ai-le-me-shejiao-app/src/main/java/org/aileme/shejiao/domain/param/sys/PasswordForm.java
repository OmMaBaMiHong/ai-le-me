package org.aileme.shejiao.domain.param.sys;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 密码修改表单
 *
 */
@Schema(name="密码修改表单", description="密码修改表单")
@Data
public class PasswordForm {

    @Schema(description = "原密码", required = true)
    private String password;

    @Schema(description = "新密码", required = true)
    private String newPassword;

}
