package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 圈子用户拉黑
 *
 */
@Data
@TableName("topic_block")
public class TopicBlockEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId
	private Integer id;
	/**
	 * 用户id
	 */
	private Integer uid;
	/**
	 * 操作用户id
	 */
	private Integer operateId;
	/**
	 * 圈子id
	 */
	private Integer topicId;
	/**
	 * 创建时间
	 */
	private Date createTime;

}
