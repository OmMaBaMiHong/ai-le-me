package org.aileme.shejiao.domain.vo;

import org.aileme.shejiao.domain.entity.admin.VoteOptionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


@Data
@Schema(name = "VoteInfoResponse对象", description = "投票信息响应对象")
public class VoteInfoResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Integer id;
	/**
	 * 标题
	 */
	@Schema(description = "标题")
	private String title;
	/**
	 * 1单选2多选
	 */
	@Schema(description = "1单选2多选")
	private Integer type;
	/**
	 * 到期时间
	 * 时间戳
	 */
	@Schema(description = "到期时间-时间戳")
	private Integer expireTime;
	/**
	 * 到期时间
	 *
	 */
	@Schema(description = "到期时间")
	private Date time;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createTime;

	@Schema(description = "选项列表")
	private List<VoteOptionEntity> options;

}
