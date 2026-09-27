
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "用户加入圈子请求体")
public class UserJoinTopicForm {

    @Schema(description = "topicClassId")
    private Integer classId;

    @Schema(description = "page")
    private Integer page;

    @Schema(description = "uid")
    private Integer uid;

}
