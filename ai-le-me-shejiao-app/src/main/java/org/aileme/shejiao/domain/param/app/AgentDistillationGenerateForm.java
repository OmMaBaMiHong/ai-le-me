package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "AgentDistillationGenerateForm", description = "人物蒸馏生成请求")
public class AgentDistillationGenerateForm implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "场景类型")
    private String sceneType;

    @Schema(description = "主体类型")
    private String subjectType;

    @Schema(description = "关系标签")
    private String relationLabel;

    @Schema(description = "对象代号")
    private String subjectName;

    @Schema(description = "分析目标")
    private String analysisGoal;

    @Schema(description = "结构化问答")
    private List<AnswerItem> answers = new ArrayList<>();

    @Schema(description = "素材列表")
    private List<MaterialItem> materials = new ArrayList<>();

    @Data
    @Schema(name = "AgentDistillationAnswerItem", description = "人物蒸馏回答项")
    public static class AnswerItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "问题编码")
        private String questionCode;

        @Schema(description = "问题标题")
        private String questionLabel;

        @Schema(description = "回答内容")
        private String answerText;
    }

    @Data
    @Schema(name = "AgentDistillationMaterialItem", description = "人物蒸馏素材项")
    public static class MaterialItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "素材类型")
        private String materialType;

        @Schema(description = "素材标签")
        private String label;

        @Schema(description = "文本内容")
        private String content;

        @Schema(description = "文件地址")
        private String fileUrl;
    }
}
