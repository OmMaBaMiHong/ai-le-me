/**
 * -----------------------------------
 * Copyright (c) 2021-2023
 * All rights reserved, Designed By my.hots.love
 * 
 * 商业版授权联系技术客服	 QQ:  3582996245
 * 严禁分享、盗用、转卖源码或非法牟利！
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 动态表
 * 
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-05 14:23:11
 */
@Data
@Entity
@Table(name = "tb_moment")
@TableName("tb_moment")
public class MomentEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 发送者id
	 */
	private Integer userId;
	/**
	 * 类型（TEXT：纯文本，IMAGE：图片，VIDEO：视频）
	 */
	private String type;
	/**
	 * 文本内容
	 */
	private String textContent;
	/**
	 * 活动id
	 */
	private Integer activityId;
	/**
	 * 点赞数量
	 */
	private Integer likeCount;
	/**
	 * 评论数量
	 */
	private Integer commentCount;
	/**
	 * 转发数量
	 */
	private Integer repostCount;
	/**
	 * 分享数量
	 */
	private Integer shareCount;
	/**
	 * 查看次数
	 */
	private Integer viewCount;
	/**
	 * 转发的动态id
	 */
	private Integer repostMomentId;
	/**
	 * 状态（0：已屏蔽，1：正常，2：已删除）
	 */
	private Integer status;
	/**
	 * 
	 */
	private Integer artistId;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 更新时间
	 */
	private Date updateTime;

}
