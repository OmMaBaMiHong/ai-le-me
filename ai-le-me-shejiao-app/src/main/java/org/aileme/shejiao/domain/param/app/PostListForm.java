
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "帖子列表分页")
public class PostListForm {

    @Schema(description = "topicId")
    private Integer topicId;

    @Schema(description = "page")
    private Integer page;

    @Schema(description = "order排序")
    private String order;

    @Schema(description = "uid")
    private Integer uid;

    @Schema(description = "disId")
    private Integer disId;

    @Schema(description = "当前查询用户的uid")
    private Integer myUid;

    @Schema(description = "话题排序")
    private String sort;

    @Schema(description = "关联活动id")
    private Integer activityId;
}
