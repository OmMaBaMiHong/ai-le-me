/**
 * -----------------------------------
 *  Copyright (c) 2021-2024
 *  All rights reserved, Designed By www.linfengtech.cn
 *  林风社交论坛商业版本请务必保留此注释头信息
 *  商业版授权联系技术客服	 QQ:  3582996245
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.domain.vo;

import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


@Data
@Schema(name = "TopicListAdminResponse", description = "后台圈子列表响应体")
public class TopicListAdminResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 圈子id
	 */
	@Schema(description = "圈子id")
	private Integer id;
	/**
	 * 创建用户id
	 */
	@Schema(description = "创建用户id")
	private Integer uid;
	/**
	 * 分类id
	 */
	@Schema(description = "分类id")
	private Integer cateId;
	/**
	 * 圈子名称
	 */
	@Schema(description = "圈子名称")
	private String topicName;
	/**
	 * 描述
	 */
	@Schema(description = "描述")
	private String description;
	/**
	 * logo
	 */
	@Schema(description = "logo")
	private String coverImage;
	/**
	 * 背景图
	 */
	@Schema(description = "背景图")
	private String bgImage;
	/**
	 * 推荐类型：0 不推荐， 1首页推荐， 2圈子页推荐 
	 */
	@Schema(description = "推荐类型：0 不推荐， 1首页推荐， 2圈子页推荐 ")
	private Integer topType;
	/**
	 * 圈子状态：0 正常 ，1禁用
	 */
	@Schema(description = "圈子状态：0 正常 ，1禁用")
	private Integer status;
	/**
	 * 是否首页推荐圈子内容
	 */
	@Schema(description = "是否首页推荐圈子内容")
	private Integer indexRecommend;
	/**
	 * 加入人数
	 */
	@Schema(description = "加入人数")
	private Integer userNum;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createTime;
	/**
	 * 创建用户信息
	 */
	@Schema(description = "创建用户信息")
	private AppUserEntity userInfo;
	/**
	 * 用户头像
	 */
	@Schema(description = "用户头像")
	private String avatar;
	/**
	 * 分类名称
	 */
	@Schema(description = "分类名称")
	private String cateName;
	/**
	 * 进圈条件0无限制1答题并审核
	 */
	@Schema(description = "进圈条件0无限制1答题并审核")
	private Integer rest;
	/**
	 * 问题内容设置
	 */
	@Schema(description = "问题内容设置")
	private String question;

	/**
	 * 是否私密：0公开 1私密
	 */
	@Schema(description = "是否私密：0公开 1私密")
	private Integer isPrivacy;

}
