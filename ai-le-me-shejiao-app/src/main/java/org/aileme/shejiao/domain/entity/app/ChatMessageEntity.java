package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 *
 * 私聊消息表
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-11-16 14:05:06
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode
@Entity
@Table(name = "chat_message")
@TableName("chat_message")
public class ChatMessageEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 私聊ID
	 */
	@Id
	@TableId
	private Integer id;
	/**
	 * 一对一私聊
	 * 用户之间会话唯一id
	 */
	private String sessionId;
	/**
	 * 发送者id
	 */
	private String senderId;
	/**
	 * 接收者id
	 */
	private String receiverId;
	/**
	 * 发送时间
	 */
	private String sendTime;
	/**
	 * 内容
	 */
	private String content;
	/**
	 * 类型
	 */
	private String messageType;
	/**
	 * 是否撤回
	 * 0 未撤回（默认）
	 * 1 撤回
	 */
	private Integer isWithdrawn;
	/**
	 * 更新时间
	 */
	private Date updateTime;

}
