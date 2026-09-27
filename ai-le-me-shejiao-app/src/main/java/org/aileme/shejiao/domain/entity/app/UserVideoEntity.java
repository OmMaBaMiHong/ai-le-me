package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户AI生成视频实体
 *
 * @author system
 * @date 2026-02-13
 */
@Data
@TableName("user_video")
@Schema(name = "UserVideoEntity", description = "用户AI生成视频")
public class UserVideoEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 视频ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "视频ID")
    private Integer id;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Integer uid;

    /**
     * 关联的帖子ID（发布后填充）
     */
    @Schema(description = "关联的帖子ID")
    private Integer postId;

    /**
     * 视频类型：1-自我介绍 2-相亲名片
     */
    @Schema(description = "视频类型：1-自我介绍 2-相亲名片")
    private Integer videoType;

    /**
     * 视频标题
     */
    @Schema(description = "视频标题")
    private String title;

    /**
     * 使用的模板编码
     */
    @Schema(description = "使用的模板编码")
    private String templateCode;

    /**
     * 视频URL
     */
    @Schema(description = "视频URL")
    private String videoUrl;

    /**
     * 封面图URL
     */
    @Schema(description = "封面图URL")
    private String coverUrl;

    /**
     * 视频时长(秒)
     */
    @Schema(description = "视频时长(秒)")
    private Integer duration;

    /**
     * 生成的提示词
     */
    @Schema(description = "生成的提示词")
    private String promptText;

    /**
     * 素材图片列表JSON
     */
    @Schema(description = "素材图片列表JSON")
    private String sourceImages;

    /**
     * 素材文字JSON
     */
    @Schema(description = "素材文字JSON")
    private String sourceText;

    /**
     * AI路由标识：provider 或 provider#profile
     */
    @Schema(description = "AI路由标识")
    private String aiModel;

    /**
     * 状态：0-生成中 1-待发布 2-已发布 3-失败
     */
    @Schema(description = "状态：0-生成中 1-待发布 2-已发布 3-失败")
    private Integer status;

    /**
     * 第三方任务ID
     */
    @Schema(description = "第三方任务ID")
    private String taskId;

    /**
     * 生成进度百分比
     */
    @Schema(description = "生成进度百分比")
    private Integer progress;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMsg;

    /**
     * 重试次数
     */
    @Schema(description = "重试次数")
    private Integer retryCount;

    /**
     * 生成后自动发布：0-否 1-是
     */
    @Schema(description = "生成后自动发布")
    private Integer autoPublish;

    /**
     * 发布时的文案内容
     */
    @Schema(description = "发布时的文案内容")
    private String postContent;

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
     * 状态：生成中
     */
    public static final int STATUS_GENERATING = 0;
    
    /**
     * 状态：待发布
     */
    public static final int STATUS_READY = 1;
    
    /**
     * 状态：已发布
     */
    public static final int STATUS_PUBLISHED = 2;
    
    /**
     * 状态：失败
     */
    public static final int STATUS_FAILED = 3;

    /**
     * 视频类型：自我介绍
     */
    public static final int TYPE_SELF_INTRO = 1;

    /**
     * 视频类型：相亲名片
     */
    public static final int TYPE_DATING_CARD = 2;
}
