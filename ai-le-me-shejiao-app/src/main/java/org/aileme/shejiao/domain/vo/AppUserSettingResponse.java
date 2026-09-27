package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


@Data
@Schema(name = "AppUserSettingResponse", description = "用户隐私项响应体")
public class AppUserSettingResponse implements Serializable {
	private static final long serialVersionUID = 1L;

		@Schema(description = "用户id")
	private Integer uid;

		@Schema(description = "隐藏粉丝")
	private Boolean isFan;

		@Schema(description = "隐藏关注")
	private Boolean isWatch;

		@Schema(description = "隐藏作品")
	private Boolean isPost;
}
