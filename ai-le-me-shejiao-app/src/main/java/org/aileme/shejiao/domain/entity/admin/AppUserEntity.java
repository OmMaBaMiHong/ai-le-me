package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-20 12:10:43
 */
@Data
@Entity
@Table(name = "user")
@TableName("user")
@JsonIgnoreProperties(value = {"password"})
public class AppUserEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId(value = "uid",type = IdType.AUTO)
	private Integer uid;
	/**
	 * 手机号
	 */
	private String mobile;
	/**
	 * 邮箱
	 */
	private String email;
	/**
	 * 用户名
	 */
	private String username;
	/**
	 * 密码
	 */
	private String password;
	/**
	 * 用户组
	 */
	private Integer groupId;
	/**
	 * 头像
	 */
	private String avatar;
	/**
	 * 性别(0未知，1男，2女)
	 */
	private Integer gender;
	/**
	 * 省份
	 */
	private String province;
	/**
	 * 城市
	 */
	private String city;
	/**
	 * 小程序openid
	 */
	private String openid;
	/**
	 * 公众号openid
	 */
	private String mpOpenid;
	/**
	 * unionid
	 */
	private String unionid;
	/**
	 * 状态
	 */
	private Integer status;
	/**
	 * 个性签名
	 */
	private String intro;
	/**
	 * 用户余额
	 */
	private BigDecimal money;
	/**
	 * 积分
	 */
	private Integer integral;
	/**
	 * 连续签到天数
	 */
	private Integer signNum;
	/**
	 * 最后登录ip
	 */
	private String lastLoginIp;
	/**
	 * 用户标签
	 */
	private String tagStr;
	/**
	 * 会员过期时间
	 */
	private Date vipExpireTime;
	/**
	 * 是否为会员 0普通用户 1会员
	 */
	private Integer vip;
	/**
	 * 0为普通用户  1官方账号 2马甲虚拟用户
	 */
	private Integer type;
	/**
	 * 更新时间
	 */
	private Date updateTime;
	/**
	 * 创建时间
	 */
	private Date createTime;


	@Schema(title = "身高")
	private String height;

	@Schema(title = "家乡城市")
	private String homeCity;

	@Schema(title = "居住城市")
	private String abodeCity;

	@Schema(title = "当前定位城市（IP自动解析）")
	private String locationCity;

	@Schema(title = "定位更新时间")
	private Date locationUpdateTime;

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
	private Integer age;
	//形象
	private String figur;

	@Schema(title = "身份证")
	private String identyCode;

	@Schema(title = "身份认证状态：0，未认证，1认证")
	private Integer identyCertifStatus;

	@Schema(title = "学历认证状态：0未认证，1认证")
	private Integer eduCertifStatus;

	@Schema(title = "生日")
	private String birthday;
	/**
	 * 用户等级
	 */
	private Integer level;

	/**
	 * 归属红娘ID（租户ID）
	 */
	@Schema(title = "归属红娘ID（租户ID）")
	private Integer hongniangId;
}
