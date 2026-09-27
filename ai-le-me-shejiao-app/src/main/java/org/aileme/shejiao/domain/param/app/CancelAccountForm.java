package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "账号注销")
public class CancelAccountForm {

    @NotBlank(message = "确认文案不能为空")
    @Schema(description = "确认注销文案")
    private String confirmText;

    @NotNull(message = "请先确认已阅读注销说明")
    @Schema(description = "是否已确认阅读注销说明")
    private Boolean acknowledged;

    @Schema(description = "注销原因")
    private String reason;
}
