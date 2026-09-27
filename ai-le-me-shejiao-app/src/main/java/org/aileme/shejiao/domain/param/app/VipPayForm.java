package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "支付dto")
public class VipPayForm {

    @Schema(description = "订单id")
    private String orderId;

    @Schema(description = "会员充值来源")
    private String payType;

    @Schema(description = "支付渠道 wechat/alipay")
    private String payChannel;

}
