package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 导航栏模块
 * 
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2023-02-04 22:14:59
 */
@Data
@Entity
@Table(name = "navigation")
@TableName("navigation")
public class NavigationEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 标题
	 */
	private String title;
	/**
	 * 图片
	 */
	private String img;
	/**
	 * 跳转路径
	 */
	private String url;
	/**
	 * 跳转类型0页面1外链
	 */
	private Integer type;
	/**
	 * 状态0正常1禁用
	 */
	private Integer status;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 更新时间
	 */
	private Date updateTime;
}