package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 画像查看记录实体
 *
 * @author system
 * @date 2026-02-14
 */
@Data
@TableName("persona_view_record")
public class PersonaViewRecordEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 查看者ID
     */
    private Integer viewerId;

    /**
     * 被查看者ID
     */
    private Integer targetUserId;

    /**
     * 画像快照ID
     */
    private Integer snapshotId;

    /**
     * 支付方式：integral-积分 cash-现金
     */
    private String payType;

    /**
     * 支付金额（积分或现金）
     */
    private BigDecimal payAmount;

    /**
     * 查看时间
     */
    private LocalDateTime viewTime;

    /**
     * 过期时间（NULL表示永久）
     */
    private LocalDateTime expireTime;

    /**
     * 状态：0-已过期 1-有效
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
