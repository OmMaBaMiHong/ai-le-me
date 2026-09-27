package org.aileme.shejiao.domain.vo;

import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


@Data
@Schema(title="AdminLuckdrawRecordResponse", description="管理端转盘抽奖记录响应体")
public class AdminLuckdrawRecordResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(title = "ID")
	private Integer id;
	/**
	 * 用户ID
	 */
	@Schema(title = "用户ID")
	private Integer userId;
	/**
	 * 奖品ID
	 */
	@Schema(title = "奖品ID")
	private Integer prizeId;
	/**
	 * 奖品类型
	 */
	@Schema(title = "奖品类型")
	private Integer prizeType;
	/**
	 * 奖品名称
	 */
	@Schema(title = "奖品名称")
	private String prizeName;
	/**
	 * 奖品图片
	 */
	@Schema(title = "奖品图片")
	private String prizeImage;
	/**
	 * 获得数量
	 */
	@Schema(title = "获得数量")
	private Integer number;
	/**
	 * 抽奖时间
	 */
	@Schema(title = "抽奖时间")
	private Date createTime;

	@Schema(title = "用户信息")
	private AppUserEntity user;
}
