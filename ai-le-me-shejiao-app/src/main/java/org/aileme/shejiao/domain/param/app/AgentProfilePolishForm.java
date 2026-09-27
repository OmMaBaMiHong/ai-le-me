package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * AI 文案润色请求
 */
@Data
@Schema(name = "AgentProfilePolishForm", description = "AI文案润色请求")
public class AgentProfilePolishForm implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "text不能为空")
    @Size(max = 300, message = "text长度不能超过300")
    @Schema(description = "原始文案", requiredMode = Schema.RequiredMode.REQUIRED)
    private String text;

    @Schema(description = "润色风格：polite/active/sincere", example = "polite")
    private String style = "polite";
}
