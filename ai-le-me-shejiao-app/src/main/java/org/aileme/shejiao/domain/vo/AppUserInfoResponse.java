package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.aileme.shejiao.domain.entity.admin.PostEntity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


@Data
@Schema(title="AppUserInfoResponse", description="用户主页信息响应体")
public class AppUserInfoResponse implements Serializable {
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
	 * 更新时间
	 */
	@Schema(title = "更新时间")
	private Date updateTime;

	/**
	 * 创建的圈子
	 */
	@Schema(title = "创建的圈子")
	private List<TopicListResponse> createTopicList;

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
	 * 是否关注
	 */
	@Schema(title = "是否关注")
	private Boolean isFollow;

	/**
	 * 是否为会员 0普通用户 1会员
	 */
	@Schema(title = "是否为会员 0普通用户 1会员")
	private Integer vip;

	/**
	 * 是否为好友
	 */
	@Schema(title = "是否为好友")
	private Boolean isFriend;


	@Schema(title = "隐藏粉丝")
	private Boolean isFan;

	@Schema(title = "隐藏关注")
	private Boolean isWatch;

	@Schema(title = "隐藏作品")
	private Boolean isPost;
	private Integer age;
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

	@Schema(title = "会话ID（与当前登录用户的聊天会话）")
	private String sessionId;

	@Schema(title = "形象照片")
	private List<String> figureList;

	@Schema(title = "最近的帖子")
	private List<PostEntity> postEntities;

	@Schema(title = "访客总数")
	private Integer visitorCount;

	@Schema(title = "最近访客预览（最近3人）")
	private List<ProfileVisitorVo> recentVisitors;

	@Schema(title = "契合度百分比（0-100）")
	private Integer matchScore;

	@Schema(title = "契合度维度明细")
	private List<MatchDetail> matchDetails;

	@Schema(title = "AI推荐标识")
	private Boolean aiRecommended;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class MatchDetail implements Serializable {
		private static final long serialVersionUID = 1L;
		/** 维度名称，如"兴趣相同" */
		private String label;
		/** 该维度得分(0-100) */
		private Integer score;
		/** 匹配描述，如"你们都喜欢旅行、美食" */
		private String desc;
	}
}
