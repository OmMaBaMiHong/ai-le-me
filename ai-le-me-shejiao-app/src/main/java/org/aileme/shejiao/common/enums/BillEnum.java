package org.aileme.shejiao.common.enums;

import java.util.stream.Stream;

public enum BillEnum {

	PM_0(0,"支出"),
	PM_1(1,"获得"),

	STATUS_0(0,"默认"),
	STATUS_1(1,"有效"),
	STATUS_2(2,"无效");

	private final Integer value;
	private final String desc;
	
	BillEnum(Integer value, String desc) {
		this.value = value;
		this.desc = desc;
	}
	
	public Integer getValue() {
		return value;
	}
	
	public String getDesc() {
		return desc;
	}

	public static BillEnum toType(int value) {
		return Stream.of(BillEnum.values())
				.filter(p -> p.value == value)
				.findAny()
				.orElse(null);
	}


}