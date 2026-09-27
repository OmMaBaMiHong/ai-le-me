package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 标签画像聚合视图
 */
@Data
@Schema(name = "TagProfileAggregateVO", description = "标签画像聚合")
public class TagProfileAggregateVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Integer userId;

    @Schema(description = "用户自选标签")
    private List<String> selfTags = new ArrayList<>();

    @Schema(description = "他人印象Top标签")
    private List<String> impressionTop = new ArrayList<>();

    @Schema(description = "关注话题标签")
    private List<String> followTopicTags = new ArrayList<>();

    @Schema(description = "行为标签")
    private List<String> behaviorTags = new ArrayList<>();

    @Schema(description = "印象标签明细")
    private List<TagCountVO> impressionDetail = new ArrayList<>();

    @Data
    @Schema(name = "TagCountVO", description = "标签计数")
    public static class TagCountVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "标签")
        private String tag;

        @Schema(description = "次数")
        private Integer count;
    }
}
