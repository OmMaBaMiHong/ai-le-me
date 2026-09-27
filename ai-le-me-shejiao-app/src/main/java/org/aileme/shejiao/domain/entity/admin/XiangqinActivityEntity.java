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
 * 相亲局活动实体类
 *
 * @author system
 * @date 2026-01-27
 */
@Data
@Entity
@Table(name = "xiangqin_activity")
@TableName("xiangqin_activity")
@Schema(description = "相亲局活动实体")
public class XiangqinActivityEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 活动ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "活动ID")
    private Integer id;

    /**
     * 组织红娘ID
     */
    @TableField("hongniang_id")
    @Schema(description = "组织红娘ID")
    private Integer hongniangId;

    /**
     * 活动标题
     */
    @Schema(description = "活动标题")
    private String title;

    /**
     * 封面图片
     */
    @TableField("cover_img")
    @Schema(description = "封面图片")
    private String coverImg;

    /**
     * 活动描述
     */
    @Schema(description = "活动描述")
    private String description;

    /**
     * 活动类型：1-线下见面 2-线上交流 3-户外活动
     */
    @TableField("activity_type")
    @Schema(description = "活动类型：1-线下见面 2-线上交流 3-户外活动")
    private Integer activityType;

    /**
     * 活动地址
     */
    @Schema(description = "活动地址")
    private String address;

    /**
     * 经度
     */
    @Schema(description = "经度")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @Schema(description = "纬度")
    private BigDecimal latitude;

    /**
     * 活动开始时间
     */
    @TableField("start_time")
    @Schema(description = "活动开始时间")
    private Date startTime;

    /**
     * 活动结束时间
     */
    @TableField("end_time")
    @Schema(description = "活动结束时间")
    private Date endTime;

    /**
     * 报名开始时间
     */
    @TableField("enroll_start_time")
    @Schema(description = "报名开始时间")
    private Date enrollStartTime;

    /**
     * 报名结束时间
     */
    @TableField("enroll_end_time")
    @Schema(description = "报名结束时间")
    private Date enrollEndTime;

    /**
     * 最大参与人数（0-不限制）
     */
    @TableField("max_participants")
    @Schema(description = "最大参与人数")
    private Integer maxParticipants;

    /**
     * 男性报名数
     */
    @TableField("male_count")
    @Schema(description = "男性报名数")
    private Integer maleCount;

    /**
     * 女性报名数
     */
    @TableField("female_count")
    @Schema(description = "女性报名数")
    private Integer femaleCount;

    /**
     * 收费类型：0-免费 1-男生收费 2-女生收费 3-全员收费
     */
    @TableField("fee_type")
    @Schema(description = "收费类型：0-免费 1-男生收费 2-女生收费 3-全员收费")
    private Integer feeType;

    /**
     * 男生费用（元）
     */
    @TableField("male_fee")
    @Schema(description = "男生费用")
    private BigDecimal maleFee;

    /**
     * 女生费用（元）
     */
    @TableField("female_fee")
    @Schema(description = "女生费用")
    private BigDecimal femaleFee;

    /**
     * 联系电话
     */
    @TableField("contact_phone")
    @Schema(description = "联系电话")
    private String contactPhone;

    /**
     * 联系微信
     */
    @TableField("contact_wechat")
    @Schema(description = "联系微信")
    private String contactWechat;

    /**
     * 年龄限制-最小
     */
    @TableField("age_min")
    @Schema(description = "年龄限制-最小")
    private Integer ageMin;

    /**
     * 年龄限制-最大
     */
    @TableField("age_max")
    @Schema(description = "年龄限制-最大")
    private Integer ageMax;

    /**
     * 参与要求
     */
    @Schema(description = "参与要求")
    private String requirement;

    /**
     * 状态：0-草稿 1-报名中 2-报名结束 3-进行中 4-已结束 5-已取消
     */
    @Schema(description = "状态：0-草稿 1-报名中 2-报名结束 3-进行中 4-已结束 5-已取消")
    private Integer status;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    @Schema(description = "浏览次数")
    private Integer viewCount;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    @Schema(description = "更新时间")
    private Date updateTime;

    // ========== 以下为非数据库字段，用于关联查询 ==========

    /**
     * 红娘姓名（非数据库字段）
     */
    @Transient
    @TableField(exist = false)
    @Schema(description = "红娘姓名")
    private String hongniangName;

    /**
     * 红娘头像（非数据库字段）
     */
    @Transient
    @TableField(exist = false)
    @Schema(description = "红娘头像")
    private String hongniangAvatar;

    /**
     * 红娘等级（非数据库字段）
     */
    @Transient
    @TableField(exist = false)
    @Schema(description = "红娘等级")
    private Integer hongniangLevel;
}
