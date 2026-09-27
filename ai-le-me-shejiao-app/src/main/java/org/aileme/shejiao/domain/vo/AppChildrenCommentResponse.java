package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 
 * 
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-25 18:51:22
 */
@Data
@Schema(title="AppChildrenCommentResponse", description="子评论内容响应体")
public class AppChildrenCommentResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(title = "ID")
	private Integer id;
	/**
	 * 父级id
	 */
	@Schema(title = "父级id")
	private Integer pid;
	/**
	 * 评论类型:1帖子
	 */
	@Schema(title = "评论类型:1帖子")
	private Integer type;
	/**
	 * 评论作者ID
	 */
	@Schema(title = "评论作者ID")
	private Integer uid;
	/**
	 * 被回复用户ID
	 */
	@Schema(title = "被回复用户ID")
	private Integer toUid;
	/**
	 * 评论帖子ID
	 */
	@Schema(title = "评论帖子ID")
	private Integer postId;
	/**
	 * 评论内容
	 */
	@Schema(title = "评论内容")
	private String content;
	/**
	 * 评论配图
	 */
	@Schema(title = "评论配图")
	private List<String> medias;
	/**
	 * 评论状态
	 */
	@Schema(title = "评论状态")
	private Integer status;
	/**
	 * 创建时间
	 */
	@Schema(title = "创建时间")
	private Date createTime;

	/**
     * 被回复用户信息
	 */
	@Schema(title = "被回复用户信息")
	private AppUserShortInfoResponse toUser;

	/**
	 * 评论用户信息
	 */
	@Schema(title = "评论用户信息")
	private AppUserShortInfoResponse userInfo;

	/**
	 * 点赞数
	 */
	@Schema(title = "点赞数")
	private Integer thumbs;

	/**
	 * 评论是否点赞
	 */
	@Schema(title = "评论是否点赞")
	private Boolean isThumbs;
}
