package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


@Data
@Schema(name = "PrizeListResponse", description = "奖品列表响应体")
public class PrizeListResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Integer id;
	/**
	 * 奖品类型[1积分2谢谢惠顾3红包]
	 */
	@Schema(description = "奖品类型[1积分2谢谢惠顾3红包]")
	private Integer type;
	/**
	 * 名称
	 */
	@Schema(description = "名称")
	private String name;
	/**
	 * 图片
	 */
	@Schema(description = "图片")
	private String url;
	/**
	 * 奖品数量
	 */
	@Schema(description = "奖品数量")
	private Integer number;

}
