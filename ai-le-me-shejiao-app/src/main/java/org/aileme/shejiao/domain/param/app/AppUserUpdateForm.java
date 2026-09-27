
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * app用户信息修改
 *
 */
@Data
@Schema(name = "app用户信息修改")
public class AppUserUpdateForm {

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "形象")
    private List<String> figureList;

    @Schema(description = "性别")
    private Integer gender;

    @Schema(description = "标签")
    private List<String> tagStr;

    @Schema(description = "个性签名")
    @Length(max = 100, message = "个性签名不能超过100个字符")
    private String intro;

    @Schema(description = "用户名")
    @Length(max = 12, message = "用户名不能超过12个字符")
    private String username;

    @Schema(description = "邮箱")
    private String email;


    @Schema(description = "身高")
    private String height;

    @Schema(description = "家乡城市")
    private String homeCity;

    @Schema(description = "居住城市")
    private String abodeCity;

    @Schema(description = "职业")
    private String job;

    @Schema(description = "婚姻状态(0,未婚，1离异，2，丧偶)")
    private Integer marryStatus;

    @Schema(description = "学历")
    private Integer education;

    @Schema(description = "毕业院校")
    private String school;

    @Schema(description = "学历认证码")
    private String eduCode;

    @Schema(description = "收入")
    private Integer income;

    private String info;

    @Schema(description = "自我介绍")
    private String selfIntroduction;

    @Schema(description = "爱情观")
    private String loveDeclaration;

    @Schema(description = "兴趣爱好")
    private String interest;

    @Schema(description = "心仪之人")
    private String adminreHerart;

    @Schema(description = "身份证")
    private String identyCode;
    @Schema(description = "生日")
    private String birthday;
}
