
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信登录表单
 *
 *                  code: loginCode,
 * 					username: userInfo.nickName,
 * 					avatar: userInfo.avatarUrl,
 * 					province: userInfo.province,
 * 					city: userInfo.city,
 * 					gender: userInfo.gender
 */
@Data
@Schema(name = "微信登录表单")
public class WxLoginForm {

    @Schema(description = "code", required = true)
    @NotBlank(message="code不能为空")
    private String code;

    @Schema(description = "用户名", required = true)
//    @NotBlank(message="用户名不能为空")
    private String username;

    @Schema(description = "头像", required = true)
//    @NotBlank(message="头像不能为空")
    private String avatar;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "性别")
    private Integer gender;
    @Schema(description = "渠道")
    private String channel;
}
