package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ReccomentLoveForm implements Serializable{

        private static final long serialVersionUID = 1L;

        @Schema(description = "id")
        private Integer id;

        @Schema(description = "用户id")
        private Integer uid;

        @Schema(description = "最新被推荐的用户id")
        private Integer recommendUid;

        @Schema(description = "推城市")
        private List<String> cityidList;

        @Schema(description = "推学历范围")
        private List<String> eduList;

        @Schema(description = "推身高范围")
        private List<String> heightList;

        @Schema(description = "推年龄段")
        private List<String> ageList;

        @Schema(description = "优先推关注我的")
        private Integer recFollowus;

        @Schema(description = "优先推我给喜欢的人")
        private Integer recBefollowus;

        @Schema(description = "只推我给喜欢的人")
        private Integer recOnlyBefollowus;

}
