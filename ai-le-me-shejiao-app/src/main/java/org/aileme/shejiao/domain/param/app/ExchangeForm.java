
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "兑换")
public class ExchangeForm {

    @Schema(description = "需兑换金额")
    private double rechargeValue;

}
