package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


@Data
@Schema(name = "LuckdrawRecordResponse对象", description = "转盘记录响应对象")
public class LuckdrawRecordResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "ID")
	private Integer id;

	@Schema(description = "描述")
	private String text;

	@Schema(description = "创建时间")
	private Date createTime;

}
