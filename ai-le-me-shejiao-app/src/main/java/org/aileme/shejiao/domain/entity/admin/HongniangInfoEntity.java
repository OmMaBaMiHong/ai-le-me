package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 红娘实体类
 *
 * @author system
 * @date 2026-01-27
 */
@Data
@Entity
@Table(name = "hongniang_info")
@TableName("hongniang_info")
@Schema(description = "红娘实体")
public class HongniangInfoEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 红娘ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "红娘ID")
    private Integer id;

    /**
     * 关联系统用户ID（可为空，纯红娘账号）
     */
    @Schema(description = "关联系统用户ID")
    private Integer userId;

    /**
     * 红娘姓名
     */
    @Schema(description = "红娘姓名")
    private String hongniangName;

    /**
     * 红娘头像
     */
    @Schema(description = "红娘头像")
    private String avatar;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 微信号
     */
    @Schema(description = "微信号")
    private String wechat;

    /**
     * 所属机构/公司
     */
    @Schema(description = "所属机构/公司")
    private String companyName;

    /**
     * 认证状态：0-未认证 1-已认证 2-认证失败
     */
    @Schema(description = "认证状态：0-未认证 1-已认证 2-认证失败")
    private Integer certificationStatus;

    /**
     * 认证资质图片（逗号分隔）
     */
    @Schema(description = "认证资质图片")
    private String certificationImg;

    /**
     * 个人简介
     */
    @Schema(description = "个人简介")
    private String intro;

    /**
     * 服务地区
     */
    @Schema(description = "服务地区")
    private String serviceArea;

    /**
     * 红娘等级：1-普通 2-高级 3-金牌
     */
    @Schema(description = "红娘等级：1-普通 2-高级 3-金牌")
    private Integer level;

    /**
     * 累计管理用户数
     */
    @Schema(description = "累计管理用户数")
    private Integer totalUsers;

    /**
     * 累计组织活动数
     */
    @Schema(description = "累计组织活动数")
    private Integer totalActivities;

    /**
     * 成功牵线数
     */
    @Schema(description = "成功牵线数")
    private Integer successCount;

    /**
     * 状态：0-禁用 1-启用
     */
    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    /**
     * 租户ID（多租户字段）
     */
    @Schema(description = "租户ID")
    private String tenantId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateTime;

    /**
     * 是否已关注（临时字段，不映射到数据库）
     */
    @Transient
    @TableField(exist = false)
    @Schema(description = "是否已关注")
    private Boolean isFollowed;
}
