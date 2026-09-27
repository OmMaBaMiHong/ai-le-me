package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Agent 内容草稿/创建结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AgentContentDraftVo", description = "Agent 内容草稿")
public class AgentContentDraftVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "内容类型：image_post/dynamic_image_post/ai_video")
    private String contentType;

    @Schema(description = "当前 Agent 提供商")
    private String provider;

    @Schema(description = "是否使用了 LLM 规划")
    private Boolean agentApplied;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "总策划摘要")
    private String intentSummary;

    @Schema(description = "策划模式")
    private String plannerMode;

    @Schema(description = "建议使用的媒体列表")
    private List<String> media;

    @Schema(description = "图像处理模式：profile_select/img2img_reserved")
    private String imageGenerationMode;

    @Schema(description = "当前是否支持真正图生图")
    private Boolean imageGenerationSupported;

    @Schema(description = "视频模板编码")
    private String templateCode;

    @Schema(description = "视频模板名称")
    private String templateName;

    @Schema(description = "视频场景分类")
    private String category;

    @Schema(description = "最终提示词，兼容旧字段")
    private String prompt;

    @Schema(description = "已选择的策划分支ID")
    private String selectedBranchId;

    @Schema(description = "创意母题编码")
    private String motifCode;

    @Schema(description = "创意母题名称")
    private String motifName;

    @Schema(description = "人物摘要")
    private String subjectProfile;

    @Schema(description = "风格方向")
    private String styleDirection;

    @Schema(description = "场景方案")
    private String scenePlan;

    @Schema(description = "镜头方案")
    private String shotPlan;

    @Schema(description = "限制词/避雷词")
    private String negativePrompt;

    @Schema(description = "最终 Prompt")
    private String finalPrompt;

    @Schema(description = "可编辑 Prompt 草稿")
    private PromptDraftVo editablePromptDraft;

    @Schema(description = "执行预设")
    private ExecutionPresetVo executionPreset;

    @Schema(description = "创意分支列表")
    private List<BranchVo> branchList;

    @Schema(description = "创建出的帖子ID")
    private Integer postId;

    @Schema(description = "创建出的AI视频ID")
    private Integer videoId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "AgentContentPromptDraftVo", description = "结构化 Prompt 草稿")
    public static class PromptDraftVo implements Serializable {

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "AgentContentExecutionPresetVo", description = "执行预设")
    public static class ExecutionPresetVo implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "执行模式：image_story/image_story_motion/image_to_video")
        private String mode;

        @Schema(description = "模板编码")
        private String templateCode;

        @Schema(description = "模板名称")
        private String templateName;

        @Schema(description = "创意分类")
        private String category;

        @Schema(description = "风格预设")
        private String stylePreset;

        @Schema(description = "建议图片张数")
        private Integer imageCount;

        @Schema(description = "预估消耗")
        private Integer estimatedCost;

        @Schema(description = "是否需要动态封面")
        private Boolean dynamicCover;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "AgentContentBranchVo", description = "创意分支")
    public static class BranchVo implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "分支ID")
        private String branchId;

        @Schema(description = "创作轨道")
        private String contentType;

        @Schema(description = "创意母题编码")
        private String motifCode;

        @Schema(description = "创意母题名称")
        private String motifName;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "正文")
        private String content;

        @Schema(description = "策划摘要")
        private String intentSummary;

        @Schema(description = "人物摘要")
        private String subjectProfile;

        @Schema(description = "风格方向")
        private String styleDirection;

        @Schema(description = "场景方案")
        private String scenePlan;

        @Schema(description = "镜头方案")
        private String shotPlan;

        @Schema(description = "限制词/避雷词")
        private String negativePrompt;

        @Schema(description = "最终 Prompt")
        private String finalPrompt;

        @Schema(description = "可编辑 Prompt 草稿")
        private PromptDraftVo editablePromptDraft;

        @Schema(description = "执行预设")
        private ExecutionPresetVo executionPreset;
    }
}
