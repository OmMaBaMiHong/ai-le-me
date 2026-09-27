/**
 * -----------------------------------
 *  Copyright (c) 2021-2023
 *  All rights reserved, Designed By my.hots.love
 *  
 *  商业版授权联系技术客服	 QQ:  3582996245
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 提现
 * 
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2023-02-01 11:43:29
 */
@Data
@Entity
@Table(name = "cash_out")
@TableName("cash_out")
public class CashOutEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 用户ID
	 */
	private Integer uid;
	/**
	 * 申请提现金额
	 */
	private BigDecimal moneyNumber;
	/**
	 * 收款码
	 */
	private String url;
	/**
	 * 审核反馈
	 */
	private String feedback;
	/**
	 * 状态 0待审核 1已完成 2已驳回
	 */
	private Integer status;
	/**
	 * 打款类型 0支付宝1微信
	 */
	private Integer type;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 修改时间
	 */
	private Date updateTime;

}
