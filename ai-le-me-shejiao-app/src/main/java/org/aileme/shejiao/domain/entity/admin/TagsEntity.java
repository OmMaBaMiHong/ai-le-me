package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 标签实体类
 *
 * @author system
 * @date 2026-01-27
 */
@Data
@Entity
@Table(name = "tags")
@TableName("tags")
@Schema(description = "标签实体")
public class TagsEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 标签ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "标签ID")
    private Integer id;

    /**
     * 标签名称
     */
    @Schema(description = "标签名称")
    private String tagName;

    /**
     * 标签分类：性格、兴趣、职业、外貌、生活习惯等
     */
    @Schema(description = "标签分类")
    private String tagCategory;

    /**
     * 标签类型：1-系统标签 2-自定义标签
     */
    @Schema(description = "标签类型：1-系统标签 2-自定义标签")
    private Integer tagType;

    /**
     * 标签图标
     */
    @Schema(description = "标签图标")
    private String icon;

    /**
     * 标签颜色
     */
    @Schema(description = "标签颜色")
    private String color;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 使用次数
     */
    @Schema(description = "使用次数")
    private Integer usageCount;

    /**
     * 关注人数
     */
    @Schema(description = "关注人数")
    private Integer followerCount;

    /**
     * 话题描述
     */
    @Schema(description = "话题描述")
    private String tagDesc;

    /**
     * 状态：0-禁用 1-启用
     */
    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

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
}
