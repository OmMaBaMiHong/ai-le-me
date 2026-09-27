package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "MockPayConfirmForm", description = "mock支付确认表单")
public class MockPayConfirmForm {

    @Schema(description = "订单号")
    private String orderId;

    @Schema(description = "支付渠道 wechat/alipay")
    private String payChannel;

    @Schema(description = "mock支付状态 success/fail/cancel")
    private String status;
}
