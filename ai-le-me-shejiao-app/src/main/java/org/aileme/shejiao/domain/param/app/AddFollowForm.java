
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "关注")
public class AddFollowForm {

    @Schema(description = "用户id")
    private Integer id;

}
