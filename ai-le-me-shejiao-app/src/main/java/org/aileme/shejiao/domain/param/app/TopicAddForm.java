package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Schema(name = "圈子新增请求体")
public class TopicAddForm implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 分类id
	 */
	@NotNull(message="cateId不能为空")
	@Schema(description = "分类id")
	private Integer cateId;
	/**
	 * 圈子名称
	 */
	@NotBlank(message="topicName不能为空")
	@Length(max = 10, message = "名称不能超过10个字符")
	@Schema(description = "圈子名称")
	private String topicName;
	/**
	 * 描述
	 */
	@NotBlank(message="description不能为空")
	@Length(max = 30, message = "描述不能超过30个字符")
	@Schema(description = "描述")
	private String description;
	/**
	 * 圈子头像
	 */
	@NotBlank(message="coverImage不能为空")
	@Schema(description = "圈子头像")
	private String coverImage;
	/**
	 * 背景图
	 */
	@NotBlank(message="bgImage不能为空")
	@Schema(description = "背景图")
	private String bgImage;

	@Schema(description = "进圈条件false无限制true答题并审核")
	private Boolean rest;

	@Schema(description = "问题内容设置")
	private String question;

	@Schema(description = "是否私密：0公开 1私密")
	private Integer isPrivacy;

}
