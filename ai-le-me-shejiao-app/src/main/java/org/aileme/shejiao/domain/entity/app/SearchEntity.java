package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户历史搜索信息表
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2023-04-26 20:13:08
 */
@Data
@TableName("search")
public class SearchEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 搜索id
	 */
	@TableId
	private Integer searchId;
	/**
	 * 客户id
	 */
	private Integer uid;
	/**
	 * 搜索内容
	 */
	private String content;
	/**
	 * 更新时间
	 */
	private Date updateTime;

}
