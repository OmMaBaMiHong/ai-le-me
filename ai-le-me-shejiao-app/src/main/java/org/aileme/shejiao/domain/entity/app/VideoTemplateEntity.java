package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 视频生成模板实体
 *
 * @author system
 * @date 2026-02-13
 */
@Data
@TableName("video_template")
@Schema(name = "VideoTemplateEntity", description = "视频生成模板")
public class VideoTemplateEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "模板ID")
    private Integer id;

    /**
     * 模板名称
     */
    @Schema(description = "模板名称")
    private String name;

    /**
     * 模板编码
     */
    @Schema(description = "模板编码")
    private String code;

    /**
     * 场景分类：self_intro/dating/confession/daily
     */
    @Schema(description = "场景分类")
    private String category;

    /**
     * 模板描述
     */
    @Schema(description = "模板描述")
    private String description;

    /**
     * 模板封面预览图
     */
    @Schema(description = "模板封面预览图")
    private String coverUrl;

    /**
     * 提示词模板，使用{字段名}作为占位符
     */
    @Schema(description = "提示词模板")
    private String promptTemplate;

    /**
     * 风格预设：natural/romantic/energetic/elegant
     */
    @Schema(description = "风格预设")
    private String stylePreset;

    /**
     * 时长范围(秒)，如 15-30
     */
    @Schema(description = "时长范围")
    private String durationRange;

    /**
     * 背景音乐URL
     */
    @Schema(description = "背景音乐URL")
    private String musicUrl;

    /**
     * 最大使用图片数
     */
    @Schema(description = "最大使用图片数")
    private Integer maxImages;

    /**
     * 最大文字长度
     */
    @Schema(description = "最大文字长度")
    private Integer maxTextLength;

    /**
     * 状态：0-禁用 1-启用
     */
    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    /**
     * 排序，越小越靠前
     */
    @Schema(description = "排序")
    private Integer sort;

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

    // ========== 状态常量 ==========
    
    /**
     * 状态：禁用
     */
    public static final int STATUS_DISABLED = 0;
    
    /**
     * 状态：启用
     */
    public static final int STATUS_ENABLED = 1;
}
