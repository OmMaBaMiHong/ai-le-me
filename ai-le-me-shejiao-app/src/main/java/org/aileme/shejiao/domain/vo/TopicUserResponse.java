package org.aileme.shejiao.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * 
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-20 12:10:43
 */
@Data
@JsonIgnoreProperties(value = {"password"})
@Schema(name = "TopicPostResponse对象", description = "圈内帖子响应对象")
public class TopicUserResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Integer uid;
	/**
	 * 手机号
	 */
	@Schema(description = "手机号")
	private String mobile;
	/**
	 * 用户名
	 */
	@Schema(description = "用户名")
	private String username;
	/**
	 * 密码
	 */
	@Schema(description = "密码")
	private String password;
	/**
	 * 用户组
	 */
	@Schema(description = "用户组")
	private Integer groupId;
	/**
	 * 头像
	 */
	@Schema(description = "头像")
	private String avatar;
	/**
	 * 性别(0未知，1男，2女)
	 */
	@Schema(description = "性别(0未知，1男，2女)")
	private Integer gender;
	/**
	 * 省份
	 */
	@Schema(description = "省份")
	private String province;
	/**
	 * 城市
	 */
	@Schema(description = "城市")
	private String city;
	/**
	 * 小程序openid
	 */
	@Schema(description = "小程序openid")
	private String openid;
	/**
	 * 公众号openid
	 */
	@Schema(description = "公众号openid")
	private String mpOpenid;
	/**
	 * unionid
	 */
	@Schema(description = "unionid")
	private String unionid;
	/**
	 * 状态
	 */
	@Schema(description = "状态")
	private Integer status;
	/**
	 * 个性签名
	 */
	@Schema(description = "个性签名")
	private String intro;
	/**
	 * 积分
	 */
	@Schema(description = "积分")
	private Integer integral;
	/**
	 * 最后登录ip
	 */
	@Schema(description = "最后登录ip")
	private String lastLoginIp;
	/**
	 * 用户标签
	 */
	@Schema(description = "用户标签")
	private String tagStr;
	/**
	 * 0为普通用户  1官方账号 2马甲虚拟用户
	 */
	@Schema(description = "0为普通用户  1官方账号 2马甲虚拟用户")
	private Integer type;
	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private Date updateTime;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createTime;

	/**
	 * 是否为圈主或管理员
	 */
	@Schema(description = "是否为圈主或管理员")
	private Boolean isAdmin;

	/**
	 * TO_FOLLOW  (0,"关注"),默认，啥也不是
	 *     MUTUAL_FOLLOW  (1,"互相关注"),
	 *     FOLLOWED (2,"已关注"),
	 *     NO_LOVE(3,"不喜欢"),
	 * @see FollowEnums
	 */
	@Schema(description = "是否关注")
	private Integer hasFollow;
}
