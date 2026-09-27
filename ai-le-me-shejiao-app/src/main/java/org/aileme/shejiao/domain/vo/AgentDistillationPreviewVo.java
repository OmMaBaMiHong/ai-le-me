package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Schema(name = "AgentDistillationPreviewVo", description = "人物蒸馏结果预览")
public class AgentDistillationPreviewVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "场景编码")
    private String sceneType;

    @Schema(description = "对象名称")
    private String subjectName;

    @Schema(description = "关系标签")
    private String relationLabel;

    @Schema(description = "快照ID")
    private Integer snapshotId;

    @Schema(description = "是否已保存")
    private Boolean saved;

    @Schema(description = "素材数量")
    private Integer materialCount;

    @Schema(description = "生成时间")
    private String generatedAt;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "核心洞察")
    private List<String> coreInsights = new ArrayList<>();

    @Schema(description = "互动建议")
    private List<String> interactionGuidance = new ArrayList<>();

    @Schema(description = "风险提醒")
    private List<String> riskFlags = new ArrayList<>();

    @Schema(description = "证据卡片")
    private List<EvidenceCard> evidenceCards = new ArrayList<>();

    @Schema(description = "置信说明")
    private List<String> confidenceNotes = new ArrayList<>();

    @Schema(description = "画像内核")
    private Map<String, Object> personaKernel = new LinkedHashMap<>();

    @Schema(description = "服务挂钩")
    private ServiceHooks serviceHooks = new ServiceHooks();

    @Data
    @Schema(name = "AgentDistillationEvidenceCard", description = "人物蒸馏证据卡")
    public static class EvidenceCard implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "卡片类型")
        private String kind;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "详情")
        private String detail;

        @Schema(description = "来源类型")
        private List<String> sourceTypes = new ArrayList<>();
    }

    @Data
    @Schema(name = "AgentDistillationServiceHooks", description = "人物蒸馏服务挂钩")
    public static class ServiceHooks implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "推荐开场")
        private String recommendedOpeningStyle;

        @Schema(description = "红娘风格提示")
        private String matchmakerStyleHint;

        @Schema(description = "助手护栏")
        private List<String> assistantGuardrails = new ArrayList<>();

        @Schema(description = "资料文案提示")
        private String profileCopyHint;
    }
}
