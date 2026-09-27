package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * 
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-26 13:15:30
 */
@Data
@Entity
@Table(name = "message")
@TableName("message")
public class MessageEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 消息id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer mId;
	/**
	 * 发送者uid
	 */
	private Integer fromUid;
	/**
	 * 接收者uid
	 */
	private Integer toUid;
	/**
	 * 帖子id
	 */
	private Integer postId;
	/**
	 * 推送标题
	 */
	private String title;
	/**
	 * 消息内容
	 */
	private String content;
	/**
	 * 0未读，1已读
	 */
	private Integer status;
	/**
	 * 1为点赞，2为评论  3为收藏 4为关注  5系统通知
	 * 注：V1.7开始本表不再存储私聊类型
	 * 私聊消息存储至lf_chat_message表
	 */
	private Integer type;
	/**
	 * 创建时间
	 */
	private Date createTime;

}
