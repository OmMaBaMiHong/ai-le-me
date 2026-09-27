package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;

@Data
@Schema(name = "圈子更新")
public class TopicUpdateForm implements Serializable {
	private static final long serialVersionUID = 1L;


	@Schema(description = "圈子id")
	private Integer id;
	/**
	 * 圈子名称
	 */
	@Length(max = 10, message = "名称不能超过10个字符")
	@Schema(description = "圈子名称")
	private String topicName;
	/**
	 * 描述
	 */
	@Length(max = 30, message = "描述不能超过30个字符")
	@Schema(description = "描述")
	private String description;
	/**
	 * 圈子头像
	 */
	@Schema(description = "圈子头像")
	private String coverImage;
	/**
	 * 背景图
	 */
	@Schema(description = "背景图")
	private String bgImage;

	@Schema(description = "进圈条件false无限制true答题并审核")
	private Boolean rest;

	@Schema(description = "问题内容设置")
	private String question;

	@Schema(description = "是否私密：0公开 1私密")
	private Integer isPrivacy;

}
