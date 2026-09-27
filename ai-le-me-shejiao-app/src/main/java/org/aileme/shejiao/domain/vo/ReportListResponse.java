package org.aileme.shejiao.domain.vo;

import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
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
@Schema(name = "ReportListResponse", description = "举报列表响应体")
public class ReportListResponse implements Serializable {

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Integer id;
	/**
	 * 文件
	 */
	@Schema(description = "文件")
	private List<String> media;
	/**
	 * 描述
	 */
	@Schema(description = "描述")
	private String content;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Integer uid;
	/**
	 * 类型1帖子 2评论 3用户 4圈子
	 */
	@Schema(description = "类型1帖子 2评论 3用户 4圈子")
	private Integer type;
	/**
	 * 状态0待审核 1已处理 2已驳回
	 */
	@Schema(description = "状态0待审核 1已处理 2已驳回")
	private Integer status;
	/**
	 * 平台反馈
	 */
	@Schema(description = "平台反馈")
	private String feedback;
	/**
	 * 关联id
	 */
	@Schema(description = "关联id")
	private Integer linkId;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createTime;
	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private Date updateTime;

	/**
	 * 用户信息
	 */
	@Schema(description = "用户信息")
	private AppUserEntity userInfo;

}
