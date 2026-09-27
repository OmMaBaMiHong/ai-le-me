package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;


@Data
@Schema(title="AppCashInfoResponse", description="提现基本信息")
public class AppCashInfoResponse implements Serializable {
	private static final long serialVersionUID = 1L;



	@Schema(title = "账户余额")
	private BigDecimal nowMoney;

	@Schema(title = "能否申请提现")
	private boolean canSubmit;

	@Schema(title = "系统提现按钮是否开启")
	private boolean cashOpen;

}
