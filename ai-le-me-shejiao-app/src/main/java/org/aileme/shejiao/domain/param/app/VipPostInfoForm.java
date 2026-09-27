package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author linfeng
 * @date 2022/4/27 15:41
 */
@Data
@Schema(name = "用户付费贴确认请求")
public class VipPostInfoForm {

    @Schema(description = "用户id", required = true)
    private Integer uid;

    @Schema(description = "帖子id", required = true)
    private Integer postId;

}
