
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "投票选项")
public class VoteOptionForm {

    @Schema(description = "值")
    private String value;

}
