
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "设置管理员请求")
public class SetAdminForm {

    @Schema(description = "用户id")
    private Integer uid;

    @Schema(description = "topicId")
    private Integer topicId;

}
