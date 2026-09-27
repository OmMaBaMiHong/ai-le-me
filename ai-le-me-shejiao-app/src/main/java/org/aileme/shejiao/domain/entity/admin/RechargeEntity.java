package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * 
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-04-21 17:05:58
 */
@Data
@Entity
@Table(name = "pay_product")
@TableName("pay_product")
public class RechargeEntity implements Serializable {
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
	 * 商品名称
	 */
	private String name;
	/**
	 * 充值金额
	 */
	private BigDecimal price;
	/**
	 * 赠送金额
	 */
	private BigDecimal givePrice;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 状态
	 */
	private Integer status;
	/**
	 * 备注
	 */
	private String remark;

}
