
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录表单
 *
 */
@Data
@Schema(name = "圈子用户列表请求")
public class TopicUserForm {

    @Schema(description = "分页页码", required = true)
    @NotBlank(message="page不能为空")
    private Integer page;

    @Schema(description = "圈子id", required = true)
    @NotBlank(message="topicId不能为空")
    private Integer id;

}
