
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "话题列表请求")
public class DiscussListForm {

    @Schema(description = "page")
    private Integer page;

    @Schema(description = "topicId")
    private Integer topicId;

}
