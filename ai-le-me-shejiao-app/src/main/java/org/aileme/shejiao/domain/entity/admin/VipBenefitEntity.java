package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * VIP会员权益设置
 * 
 * @author Wade
 * @date 2026-02-10
 */
@Data
@Entity
@Table(name = "vip_benefit")
@TableName("vip_benefit")
public class VipBenefitEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	
	/**
	 * 权益标题
	 */
	private String title;
	
	/**
	 * 权益描述
	 */
	private String describes;
	
	/**
	 * 图标URL
	 */
	private String icon;
	
	/**
	 * 状态 0-有效 1-无效
	 */
	private Integer status;
	
	/**
	 * 排序（数值越大越靠前）
	 */
	private Integer sort;
	
	/**
	 * 创建时间
	 */
	private Date createTime;
	
	/**
	 * 更新时间
	 */
	private Date updateTime;

}
