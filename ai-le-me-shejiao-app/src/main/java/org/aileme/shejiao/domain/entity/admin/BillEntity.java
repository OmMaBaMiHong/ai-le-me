package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * 用户账单
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-04-20 20:46:48
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Entity
@Table(name = "account_bill")
@TableName("account_bill")
public class BillEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户账单id
	 */
	@Id
	@TableId
	private Integer id;
	/**
	 * 用户uid
	 */
	private Integer uid;
    /*打赏者 id，充值 id*/
    private Integer tipUserId;
	/**
	 * 关联id：打赏类型 id，postId
	 */
	private String linkId;
	/**
	 * 关联订单号
	 */
	private String orderId;
	/**
	 * 0 = 支出 1 = 获得
	 */
	private Integer pm;
	/**
	 * 账单标题
	 */
	private String title;
	/**
	 * 明细种类
	 */
	private String category;
	/**
	 * 明细类型
	 */
	private String type;
	/**
	 * 明细数字
	 */
	private BigDecimal number;
	/**
	 * 剩余
	 */
	private BigDecimal balance;
	/**
	 * 备注
	 */
	private String mark;
	/**
	 * 添加时间
	 */
	private Date addTime;
	/**
	 * 0待确定 1有效 -1无效
	 */
	private Integer status;

}
