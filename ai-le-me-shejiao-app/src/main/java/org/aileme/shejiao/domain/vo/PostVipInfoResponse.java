package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.math.BigDecimal;


@Data
@Schema(name = "付费帖子内容")
public class PostVipInfoResponse implements Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * 标题
	 */
	@Schema(description = "标题")
	private String title;


	/**
	 * 付费贴支付金额
	 */
	@Schema(description = "付费贴支付金额")
	private BigDecimal pay;


	/**
	 * 是否已购买
	 */
	@Schema(description = "是否已购买")
	private Boolean isBuy;


	/**
	 * 付费简介
	 */
	@Length(max = 250, message = "简介不能超过250个字符")
	@Schema(description = "付费简介")
	private String brief;


}
