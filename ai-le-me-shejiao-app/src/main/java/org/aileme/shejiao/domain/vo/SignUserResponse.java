package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


@Data
@Schema(name = "SignUserResponse", description = "签到用户响应体")
public class SignUserResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Integer uid;
	/**
	 * 手机号
	 */
	@Schema(description = "手机号")
	private String mobile;
	/**
	 * 用户名
	 */
	@Schema(description = "用户名")
	private String username;
	/**
	 * 头像
	 */
	@Schema(description = "头像")
	private String avatar;
	/**
	 * 积分
	 */
	@Schema(description = "积分")
	private Integer integral;
	/**
	 * 总的签到天数
	 */
	@Schema(description = "总的签到天数")
	private Integer sumSignDay;
	/**
	 * 当天是否签到
	 */
	@Schema(description = "当天是否签到")
	private Boolean isDaySign;
	/**
	 * 昨天是否签到
	 */
	@Schema(description = "昨天是否签到")
	private Boolean isYestDaySign;
	/**
	 * 连续签到天数
	 */
	@Schema(description = "连续签到天数")
	private Integer signNum;


}
