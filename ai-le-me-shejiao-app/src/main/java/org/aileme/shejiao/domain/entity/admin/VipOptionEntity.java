package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 会员充值选项
 * 
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-09-28 14:26:17
 */
@Data
@Entity
@Table(name = "pay_product")
@TableName("pay_product")
public class VipOptionEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 商品类型
	 */
	private String productType;
	/**
	 * 名称
	 */
	private String name;
	/**
	 * 有效天数
	 */
	private Integer validDays;
	/**
	 * 价格
	 */
	private BigDecimal price;
	/**
	 * 描述
	 */
	private String remark;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 状态
	 */
	private Integer status;

}
