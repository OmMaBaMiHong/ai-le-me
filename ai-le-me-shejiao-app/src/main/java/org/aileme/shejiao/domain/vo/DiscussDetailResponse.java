package org.aileme.shejiao.domain.vo;

import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


@Data
@Schema(name = "DiscussDetailResponse", description = "话题详情响应体")
public class DiscussDetailResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 话题标签id
	 */
	@Schema(description = "话题标签id")
	private Integer id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Integer uid;
	/**
	 * 圈子id
	 */
	@Schema(description = "圈子id")
	private Integer topicId;
	/**
	 * 标题
	 */
	@Schema(description = "标题")
	private String title;
	/**
	 * 描述
	 */
	@Schema(description = "描述")
	private String introduce;
	/**
	 * 浏览量
	 */
	@Schema(description = "浏览量")
	private Integer readCount;
	/**
	 * 推荐位置：0 不推荐  1 首页推荐
	 */
	@Schema(description = "推荐位置：0 不推荐  1 首页推荐")
	private Integer topType;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createTime;

	@Schema(description = "用户信息")
	private AppUserEntity userInfo;

	/**
	 * 帖子数量
	 */
	@Schema(description = "帖子数量")
	private Integer postCount;

}
