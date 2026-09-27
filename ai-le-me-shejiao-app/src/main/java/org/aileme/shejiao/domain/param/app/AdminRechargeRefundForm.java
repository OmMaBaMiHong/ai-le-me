package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "管理端充值订单退款表单")
public class AdminRechargeRefundForm {

    @Schema(description = "平台订单号")
    private String orderId;

    @Schema(description = "退款金额，为空时默认全额退款")
    private BigDecimal refundAmount;

    @Schema(description = "退款原因")
    private String reason;
}
