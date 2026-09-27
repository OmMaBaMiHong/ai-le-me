package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @author linfeng
 * @date 2022/4/19 19:47
 */
@Data
@Schema(name = "充值表单")
public class RechargeForm  implements Serializable {

    @NotBlank(message = "充值参数有误")
    @Schema(description = "用户充值ID")
    private String recharId;

    @Schema(description = "来源")
    private String from;

    @Min(value = 0,message = "充值金额不能低于0")
    @Schema(description = "充值金额")
    private Double price;

    @Schema(description = "赠送金额")
    private Double paidPrice;

    @Schema(description = "充值单号")
    private String orderSn;

    @Schema(description = "支付渠道 wechat/alipay")
    private String payChannel;

    @Schema(description = "mock支付状态 success/fail/cancel")
    private String mockStatus;
}
