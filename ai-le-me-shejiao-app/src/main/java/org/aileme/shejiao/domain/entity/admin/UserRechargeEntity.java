package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户充值实体类
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-04-19 19:27:33
 */
@Data
@Entity
@Table(name = "pay_order")
@TableName("pay_order")
public class UserRechargeEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@TableId
	private Integer id;
	/**
	 * 充值用户UID
	 */
	private Integer uid;
	/**
	 *
	 */
	private String nickname;
	/**
	 * 订单号
	 */
	private String orderId;
	/**
	 * 订单标题
	 */
	private String title;
	/**
	 * 业务关联ID
	 */
	private String bizId;
	/**
	 * 充值金额/支付金额
	 */
	private BigDecimal price;
	/**
	 * 购买赠送金额
	 */
	private BigDecimal givePrice;
	/**
	 * 到账爱情币数量
	 */
	private Integer coinAmount;
	/**
	 * 支付端类型 weixin/h5/app
	 */
	private String rechargeType;
	/**
	 * 订单状态 0待支付 1已支付 2已退款 3已关闭
	 */
	private Integer status;
	/**
	 * 累计退款金额
	 */
	private BigDecimal refundAmount;
	/**
	 * 充值支付时间
	 */
	private Date payTime;
	/**
	 * 充值时间
	 */
	private Date addTime;
	/**
	 * 更新时间
	 */
	private Date updateTime;

	/**
	 * 支付生成的订单号
	 */
	private String transactionId;

	/**
	 * 订单支付编号
	 */
	private String outTradeNo;

	/**
	 * 订单类型 0钱包充值(旧逻辑) 1会员充值 2爱情币充值 3活动报名
	 */
	private Integer type;
	/**
	 * 充值渠道
	 */
	private String channel;
	/**
	 * 订单备注
	 */
	private String remark;
}
