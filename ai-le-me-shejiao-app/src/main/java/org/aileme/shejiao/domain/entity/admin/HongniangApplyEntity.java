package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 红娘申请实体类
 *
 * @author system
 * @date 2026-02-10
 */
@Data
@Entity
@Table(name = "hongniang_apply")
@TableName("hongniang_apply")
@Schema(description = "红娘申请实体")
public class HongniangApplyEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 申请ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "申请ID")
    private Integer id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    @Schema(description = "用户ID")
    private Integer userId;

    /**
     * 真实姓名
     */
    @TableField("real_name")
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
     * 身份证号
     */
    @TableField("id_card")
    @Schema(description = "身份证号")
    private String idCard;

    /**
     * 所在城市
     */
    @Schema(description = "所在城市")
    private String city;

    /**
     * 个人简介
     */
    @Schema(description = "个人简介")
    private String introduction;

    /**
     * 擅长领域（多选，逗号分隔）
     */
    @TableField("skill_tags")
    @Schema(description = "擅长领域")
    private String skillTags;

    /**
     * 申请理由
     */
    @TableField("apply_reason")
    @Schema(description = "申请理由")
    private String applyReason;

    /**
     * 申请状态：0-待审核 1-已通过 2-已拒绝
     */
    @Schema(description = "申请状态：0-待审核 1-已通过 2-已拒绝")
    private Integer status;

    /**
     * 审核备注
     */
    @TableField("audit_remark")
    @Schema(description = "审核备注")
    private String auditRemark;

    /**
     * 审核时间
     */
    @TableField("audit_time")
    @Schema(description = "审核时间")
    private Date auditTime;

    /**
     * 申请时间
     */
    @TableField("create_time")
    @Schema(description = "申请时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    @Schema(description = "更新时间")
    private Date updateTime;
}
