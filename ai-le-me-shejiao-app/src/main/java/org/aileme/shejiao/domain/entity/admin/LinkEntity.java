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
 * @date 2022-01-26 14:05:38
 */
@Data
@Entity
@Table(name = "link")
@TableName("link")
public class LinkEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
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
	 * 路径
	 */
	private String url;
	/**
	 * 图片
	 */
	private String img;
	/**
	 * 跳转类型
	 * 3 页面内   1外链
	 */
	private Integer type;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 分类
	 * 0 圈子页
	 * 1 个人页
	 */
	private Integer cateId;
}