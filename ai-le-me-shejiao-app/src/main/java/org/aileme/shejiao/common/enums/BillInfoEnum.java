package org.aileme.shejiao.common.enums;

import java.util.stream.Stream;

/**
 *
 * 账单相关枚举
 */
public enum BillInfoEnum {

	DEAFUL_ALL(0,"所有"),
	PAY(1,"消费"),
	RECHAREGE(2,"充值"),
	BROKERAGE(3,"付费贴收费"),
	EXTRACT(4,"提现"),
	SIGN_INTEGRAL(5,"签到积分"),
	PAY_PRODUCT_REFUND(6,"退款"),
	SYSTEM_ADD(7,"系统添加"),
	SYSTEM_SUB(8,"系统减少");

	private final Integer value;
	private final String desc;
	
	BillInfoEnum(Integer value, String desc) {
		this.value = value;
		this.desc = desc;
	}
	
	public Integer getValue() {
		return value;
	}
	
	public String getDesc() {
		return desc;
	}

	public static BillInfoEnum toType(int value) {
		return Stream.of(BillInfoEnum.values())
				.filter(p -> p.value == value)
				.findAny()
				.orElse(null);
	}



}
