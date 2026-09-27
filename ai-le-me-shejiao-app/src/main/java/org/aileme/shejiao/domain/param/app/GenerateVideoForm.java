package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * 生成视频请求表单
 *
 * @author system
 * @date 2026-02-13
 */
@Data
@Schema(name = "GenerateVideoForm", description = "生成视频请求")
public class GenerateVideoForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 模板编码
     */
    @NotBlank(message = "请选择视频模板")
    @Schema(description = "模板编码", example = "self_intro_fresh")
    private String templateCode;

    /**
     * 自定义提示词（可选，覆盖模板）
     */
    @Schema(description = "自定义提示词（可选）")
    private String customPrompt;

    /**
     * 发布文案（可选，发布时使用）
     */
    @Schema(description = "发布文案（可选）")
    private String postContent;

    /**
     * 自动发布：0-否 1-是
     */
    @Schema(description = "自动发布：0-否 1-是", example = "0")
    private Integer autoPublish = 0;

    /**
     * 指定生成参考图，优先使用用户显式选择的头像/生活照
     */
    @Schema(description = "指定生成参考图，优先使用用户显式选择的头像/生活照")
    private List<String> media;
}
