package org.aileme.shejiao.common.enums;

import lombok.Getter;

@Getter
public enum BillDetailEnum {

	TYPE_1("recharge","充值"),
	TYPE_2("brokerage","付费贴收费"),
	TYPE_3("pay_post","付费贴"),
	TYPE_4("extract","提现"),
	TYPE_5("pay_product_refund","退款"),
	TYPE_6("system_add","系统添加"),
	TYPE_7("system_sub","系统减少"),
	TYPE_8("deduction","减去"),
	TYPE_9("gain","奖励"),
	TYPE_10("sign","签到"),
	TYPE_11("exchange","扣除积分兑换余额"),
	TYPE_12("exchange_add","积分兑换增加余额"),
	TYPE_13("luck_add_integral","抽奖消耗积分"),
	TYPE_14("luck_sub_integral","中奖获得积分"),
	TYPE_15("luck_add_money","中奖获得红包"),
	TYPE_16("vip_recharge","会员充值"),
	TYPE_17("money_exchange","扣除余额兑换积分"),
	TYPE_18("money_exchange_add","余额兑换增加积分"),
    TYPE_19("reward_integral_add","被打赏增加积分"),
    TYPE_20("reward_integral_sub","打赏帖子扣除积分"),
    TYPE_21("coin_recharge","爱情币充值"),
    TYPE_22("agent_companion_cost","智能恋爱助手消耗爱情币"),

    CATEGORY_1("now_money","金额"),
    CATEGORY_2("integral","积分");

	private final String value;
	private final String desc;

	BillDetailEnum(String value, String desc) {
		this.value = value;
		this.desc = desc;
	}


}
