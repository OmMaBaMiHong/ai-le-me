package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Agent 内容创作请求
 */
@Data
@Schema(name = "AgentContentForm", description = "Agent 内容创作请求")
public class AgentContentForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "内容类型：image_post/dynamic_image_post/ai_video", example = "image_post")
    private String contentType;

    @Schema(description = "创作轨道，兼容字段：image_post/dynamic_image_post/ai_video", example = "ai_video")
    private String railType;

    @Schema(description = "创作目标或补充要求", example = "想做一条更真诚自然的脱单动态")
    private String goal;

    @Schema(description = "创意大类或母题分类，可为空", example = "cinematic_portrait")
    private String category;

    @Schema(description = "指定模板编码，兼容字段，主流程可为空")
    private String templateCode;

    @Schema(description = "创意母题编码，可为空", example = "hanfu_dynasty")
    private String motifCode;

    @Schema(description = "策划分支ID，可为空", example = "branch_1")
    private String branchId;

    @Schema(description = "圈子ID，可为空；图文直接创建时默认取1")
    private Integer topicId;

    @Schema(description = "关联活动ID，可为空")
    private Integer activityId;

    @Schema(description = "已上传媒体列表，可为空；为空时自动从头像和生活照里选")
    private List<String> media;

    @Schema(description = "Prompt 结构化草稿")
    private PromptDraft promptDraft;

    @Schema(description = "策划换一批时的刷新序号", example = "0")
    private Integer refreshIndex = 0;

    @Schema(description = "自动发布：仅 AI 视频生效，0-否 1-是", example = "0")
    private Integer autoPublish = 0;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "AgentContentPromptDraft", description = "结构化 Prompt 草稿")
    public static class PromptDraft implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "人物设定")
        private String subject;

        @Schema(description = "风格氛围")
        private String style;

        @Schema(description = "场景/时代")
        private String scene;

        @Schema(description = "镜头/动作")
        private String shot;

        @Schema(description = "限制词/避雷词")
        private String negativePrompt;
    }
}
