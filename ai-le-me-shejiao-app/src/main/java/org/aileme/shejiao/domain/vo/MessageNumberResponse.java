package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;


@Data
@Schema(name = "MessageNumberResponse", description = "消息响应体")
public class MessageNumberResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 总消息数量
	 */
	@Schema(description = "总消息数量")
	private Integer allCount;
	/**
	 * 评论数
	 */
	@Schema(description = "评论数")
	private Integer comment;
	/**
	 * 关注数
	 */
	@Schema(description = "关注数")
	private Integer follow;
	/**
	 * 帖子评论点赞数
	 */
	@Schema(description = "帖子评论点赞数")
	private Integer thumbCount;
	/**
	 * 系统通知未读数
	 */
	@Schema(description = "系统通知未读数")
	private Integer systemUnreadCount;

	/**
	 * 推送通知消息
	 */
	@Schema(description = "推送通知消息")
	private List<MessageNoticeResponse> articleMsgList;

	@Schema(description = "私信消息列表")
	private List<ChatMessageListResponse> chatMsgList;


}
