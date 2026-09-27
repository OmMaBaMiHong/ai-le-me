
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "用户提交提现申请表单")
public class AddCashOutForm {

    @Schema(description = "打款类型0支付宝1微信")
    private Integer type;

    @Schema(description = "申请提现金额")
    private BigDecimal moneyNumber;

    @Schema(description = "收款码路径")
    private String url;
}
