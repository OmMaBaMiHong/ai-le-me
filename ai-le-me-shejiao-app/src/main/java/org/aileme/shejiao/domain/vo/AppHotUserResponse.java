package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


@Data
@Schema(title="AppReportListResponse", description="热门博主响应体")
public class AppHotUserResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户id
	 */
	@Schema(title = "用户id")
	private Integer uid;
	/**
	 * 用户名
	 */
	@Schema(title = "用户名")
	private String username;

	/**
	 * 头像
	 */
	@Schema(title = "头像")
	private String avatar;
	/**
	 * 粉丝
	 */
	@Schema(title = "粉丝")
	private Integer fans;
	/**
	 * 动态数
	 */
	@Schema(title = "动态数")
	private Long postNum;

	/**
	 * 是否关注
	 */
	@Schema(title = "是否关注")
	private Boolean isFollow;

	/**
	 * 是否为会员 0普通用户 1会员
	 */
	@Schema(title = "是否为会员 0普通用户 1会员")
	private Integer vip;

}
