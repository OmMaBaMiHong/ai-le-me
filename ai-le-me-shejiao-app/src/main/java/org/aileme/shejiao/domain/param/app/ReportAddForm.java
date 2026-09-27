package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 用户举报
 * 
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-09-01 12:55:12
 */
@Data
@Schema(name = "用户举报请求体")
public class ReportAddForm implements Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * 文件
	 */
	@Schema(description = "举报图片路径列表", required = true)
	private List<String> media;
	/**
	 * 描述
	 */
	@Length(max = 400, message = "内容不能超过400个字符")
	@NotBlank(message = "参数不能为空")
	@Schema(description = "举报内容描述", required = true)
	private String content;

	/**
	 * 类型1帖子 2评论 3用户 4圈子
	 */
	@NotNull(message = "无分类参数")
	@Schema(description = "举报分类id:1帖子 2评论 3用户 4圈子", required = true)
	private Integer cateId;

	/**
	 * 关联id
	 */
	@Schema(description = "关联id")
	private Integer linkId;


}
