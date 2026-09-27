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
@Table(name = "pay_order_detail")
@TableName("pay_order_detail")
public class PayOrderDetailEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId
    private Integer id;

    private Integer orderRecordId;

    private String orderId;

    private Integer uid;

    private Integer orderType;

    private String eventType;

    private String title;

    private BigDecimal amount;

    private Integer coinAmount;

    private String bizId;

    private String transactionId;

    private String requestJson;

    private String responseJson;

    private String remark;

    private Integer status;

    private Date addTime;
}
