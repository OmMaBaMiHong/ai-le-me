package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * IM通知表
 * 
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-11-16 15:04:19
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode
@Entity
@Table(name = "notice")
@TableName("notice")
public class NoticeEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 发送者id
	 */
	private Integer senderId;
	/**
	 * 接收者id
	 */
	private Integer receiverId;
	/**
	 * 类型
	 */
	private String type;
	/**
	 * 消息内容
	 */
	private String information;
	/**
	 * 是否已读
	 * 0未读  1已读
	 */
	private Boolean isRead;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 更新时间
	 */
	private Date updateTime;

}
