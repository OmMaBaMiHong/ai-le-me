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
package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户举报
 * 
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-09-01 12:55:12
 */
@Data
@Entity
@Table(name = "report")
@TableName("report")
public class ReportEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 文件
	 */
	private String media;
	/**
	 * 描述
	 */
	private String content;
	/**
	 * 用户id
	 */
	private Integer uid;
	/**
	 * 类型1帖子 2评论 3用户 4圈子
	 */
	private Integer type;
	/**
	 * 状态0待审核 1已处理 2已驳回
	 */
	private Integer status;
	/**
	 * 平台反馈
	 */
	private String feedback;
	/**
	 * 关联id
	 */
	private Integer linkId;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 更新时间
	 */
	private Date updateTime;

}
