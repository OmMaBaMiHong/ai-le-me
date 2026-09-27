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
package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户举报
 * 
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-09-01 12:55:12
 */
@Data
@Schema(title="AppReportListResponse", description="用户举报帖子响应体")
public class AppReportListResponse implements Serializable {

	/**
	 * ID
	 */
	@Schema(title = "ID")
	private Integer id;
	/**
	 * 文件
	 */
	@Schema(title = "文件")
	private List<String> media;
	/**
	 * 描述
	 */
	@Schema(title = "描述")
	private String content;
	/**
	 * 用户id
	 */
	@Schema(title = "用户id")
	private Integer uid;
	/**
	 * 类型1帖子 2评论 3用户 4圈子
	 */
	@Schema(title = "类型1帖子 2评论 3用户 4圈子")
	private Integer type;
	/**
	 * 状态0待审核 1已处理 2已驳回
	 */
	@Schema(title = "状态0待审核 1已处理 2已驳回")
	private Integer status;
	/**
	 * 平台反馈
	 */
	@Schema(title = "平台反馈")
	private String feedback;
	/**
	 * 关联id
	 */
	@Schema(title = "关联id")
	private Integer linkId;
	/**
	 * 创建时间
	 */
	@Schema(title = "创建时间")
	private Date createTime;
	/**
	 * 更新时间
	 */
	@Schema(title = "更新时间")
	private Date updateTime;

}
