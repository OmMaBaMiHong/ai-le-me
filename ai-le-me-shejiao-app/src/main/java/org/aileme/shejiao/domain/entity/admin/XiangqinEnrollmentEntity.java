package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 相亲局报名实体类
 *
 * @author system
 * @date 2026-01-27
 */
@Data
@Entity
@Table(name = "xiangqin_enrollment")
@TableName("xiangqin_enrollment")
@Schema(description = "相亲局报名实体")
public class XiangqinEnrollmentEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 报名ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "报名ID")
    private Integer id;

    /**
     * 活动ID
     */
    @Schema(description = "活动ID")
    private Integer activityId;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Integer userId;

    /**
     * 真实姓名
     */
    @Schema(description = "真实姓名")
    private String realName;

    /**
     * 联系电话
     */
    @Schema(description = "联系电话")
    private String phone;

    /**
     * 微信号
     */
    @Schema(description = "微信号")
    private String wechat;

    /**
     * 报名留言
     */
    @Schema(description = "报名留言")
    private String enrollRemark;

    /**
     * 支付状态：0-未支付 1-已支付 2-已退款
     */
    @Schema(description = "支付状态：0-未支付 1-已支付 2-已退款")
    private Integer paymentStatus;

    /**
     * 支付金额
     */
    @Schema(description = "支付金额")
    private BigDecimal paymentAmount;

    /**
     * 支付时间
     */
    @Schema(description = "支付时间")
    private Date paymentTime;

    /**
     * 签到状态：0-未签到 1-已签到
     */
    @Schema(description = "签到状态：0-未签到 1-已签到")
    private Integer checkInStatus;

    /**
     * 签到时间
     */
    @Schema(description = "签到时间")
    private Date checkInTime;

    /**
     * 报名状态：0-待审核 1-已通过 2-已拒绝 3-已取消
     */
    @Schema(description = "报名状态：0-待审核 1-已通过 2-已拒绝 3-已取消")
    private Integer status;

    /**
     * 审核备注
     */
    @Schema(description = "审核备注")
    private String auditRemark;

    /**
     * 报名时间
     */
    @Schema(description = "报名时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateTime;

    /**
     * 用户昵称（非数据库字段）
     */
    @Transient
    @TableField(exist = false)
    @Schema(description = "用户昵称")
    private String username;

    /**
     * 用户头像（非数据库字段）
     */
    @Transient
    @TableField(exist = false)
    @Schema(description = "用户头像")
    private String avatar;

    /**
     * 用户性别（非数据库字段）
     */
    @Transient
    @TableField(exist = false)
    @Schema(description = "用户性别")
    private Integer gender;

    /**
     * 用户年龄（非数据库字段）
     */
    @Transient
    @TableField(exist = false)
    @Schema(description = "用户年龄")
    private Integer age;
}
