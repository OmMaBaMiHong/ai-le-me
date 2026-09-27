package org.aileme.shejiao.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.aileme.shejiao.domain.entity.admin.UserInfo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


@Data
@Schema(title="AppUserResponse", description="用户信息响应体")
public class AppUserResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户id
	 */
	@Schema(title = "用户id")
	private Integer uid;
	/**
	 * 手机号
	 */
	@Schema(title = "手机号")
	private String mobile;
	/**
	 * 用户名
	 */
	@Schema(title = "用户名")
	private String username;

	/**
	 * 头像
	 */
	@Schema(title = "头像")
	private String avatar;
	/**
	 * 性别(0未知，1男，2女)
	 */
	@Schema(title = "性别(0未知，1男，2女)")
	private Integer gender;
	/**
	 * 省份
	 */
	@Schema(title = "省份")
	private String province;
	/**
	 * 城市
	 */
	@Schema(title = "城市")
	private String city;

	/**
	 * 个性签名
	 */
	@Schema(title = "个性签名")
	private String intro;
	/**
	 * 积分
	 */
	@Schema(title = "积分")
	private Integer integral;
	/**
	 * 余额
	 */
	@Schema(title = "余额")
	private BigDecimal money;
	/**
	 * 最后登录ip
	 */
	@Schema(title = "最后登录ip")
	private String lastLoginIp;
	/**
	 * 用户标签
	 */
	@Schema(title = "用户标签")
	private List<String> tagStr;
	/**
	 * 0为普通用户  1官方账号 2马甲虚拟用户
	 */
	@Schema(title = "0为普通用户  1官方账号 2马甲虚拟用户")
	private Integer type;
	/**
	 * 关注
	 */
	@Schema(title = "关注")
	private Integer follow;
	/**
	 * 粉丝
	 */
	@Schema(title = "粉丝")
	private Integer fans;
	/**
	 * 动态数
	 */
	@Schema(title = "动态数")
	private Long postNum;
	/**
	 * 邮箱
	 */
	@Schema(title = "邮箱")
	private String email;
	/**
	 * 会员过期时间
	 */
	@Schema(title = "会员过期时间")
	private Date vipExpireTime;
	/**
	 * 是否为会员 0普通用户 1会员
	 */
	@Schema(title = "是否为会员 0普通用户 1会员")
	private Integer vip;
	private Integer age;

	@Schema(title = "形象照片")
	private List<String> figureList;

	//最近的动态
	@Schema(title = "最近的动态图片")
	@TableField(exist = false)
	private List<String> medias;

	@Schema(title = "生日")
	private String birthday;

	@Schema(title = "身高")
	private Integer height;

	@Schema(title = "家乡城市")
	private String homeCity;

	@Schema(title = "居住城市")
	private String abodeCity;

	@Schema(title = "职业")
	private String job;

	@Schema(title = "婚姻状态(0,未婚，1离异，2，丧偶)")
	private Integer marryStatus;

	@Schema(title = "学历")
	private Integer education;

	@Schema(title = "毕业院校")
	private String school;

	@Schema(title = "学历认证码")
	private String eduCode;

	@Schema(title = "收入")
	private Integer income;

	private String info;
	private UserInfo userInfo;

	@Schema(title = "自我介绍")
	private String selfIntroduction;

	@Schema(title = "爱情观")
	private String loveDeclaration;

	@Schema(title = "兴趣爱好")
	private String interest;

	@Schema(title = "心仪之人")
	private String adminreHerart;

	@Schema(title = "审核状态")
	private Integer auditStatus;

	@Schema(title = "身份证")
	private String identyCode;

	@Schema(title = "身份认证状态：0，未认证，1认证")
	private Integer identyCertifStatus;

	@Schema(title = "学历认证状态：0未认证，1认证")
	private Integer eduCertifStatus;

	@Schema(title = "主理人ID")
	private Integer hongniangId;

}
