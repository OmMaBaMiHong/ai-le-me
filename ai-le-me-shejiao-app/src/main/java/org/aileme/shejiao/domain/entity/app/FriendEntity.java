package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * 好友表
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
@Table(name = "friend")
@TableName("friend")
public class FriendEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 本人id
	 */
	private Integer myId;
	/**
	 * 朋友id
	 */
	private Integer friendId;
	/**
	 * 备注
	 */
	private String notation;
	/**
	 * 用户标识
	 */
	private Long sessionId;
	/**
	 * 最后一条消息
	 */
	private String lastMessage;
	/**
	 * 未读消息数
	 * 0 未读  1已读
	 */
	private Integer unread;
	/**
	 * 是否隐藏
	 */
	private Boolean isHidden;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 更新时间
	 */
	private Date updateTime;

}
