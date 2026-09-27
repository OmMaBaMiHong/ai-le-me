package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户菜单
 * 
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-07-22 09:33:30
 */
@Data
@Entity
@Table(name = "user_menu")
@TableName("user_menu")
public class UserMenuEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 跳转路径
	 */
	private String url;
	/**
	 * 图片地址
	 */
	private String img;
	/**
	 * 名称
	 */
	private String name;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 状态 0正常 1下架
	 */
	private Integer status;

}
