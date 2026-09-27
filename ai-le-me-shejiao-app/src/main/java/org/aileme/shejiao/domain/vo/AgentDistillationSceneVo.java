package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "AgentDistillationSceneVo", description = "人物蒸馏场景")
public class AgentDistillationSceneVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "场景编码")
    private String sceneType;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "默认关系标签")
    private String defaultRelationLabel;

    @Schema(description = "关系预设")
    private List<String> relationPresets = new ArrayList<>();

    @Schema(description = "素材提示")
    private List<String> materialHints = new ArrayList<>();
}
