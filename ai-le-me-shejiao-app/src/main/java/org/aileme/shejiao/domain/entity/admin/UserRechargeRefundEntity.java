package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "user_recharge_refund")
@TableName("user_recharge_refund")
public class UserRechargeRefundEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId
    private Integer id;

    private Integer rechargeId;

    private Integer uid;

    private String orderId;

    private Integer type;

    private String refundNo;

    private BigDecimal refundAmount;

    private Integer coinAmount;

    /**
     * 0处理中 1成功 2失败
     */
    private Integer refundStatus;

    private String reason;

    private String operatorName;

    private String transactionId;

    private Date addTime;

    private Date refundTime;
}
