package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "预充值dto")
public class VipRechargeForm {

    @Schema(description = "会员充值选项id")
    private Integer vipId;

    @Schema(description = "会员充值来源")
    private String payType;

    @Schema(description = "支付渠道 wechat/alipay")
    private String payChannel;

}
