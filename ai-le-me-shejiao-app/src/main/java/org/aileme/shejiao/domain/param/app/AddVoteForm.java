package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;


@Data
@Schema(name = "发起投票帖请求体")
public class AddVoteForm implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 圈子id
     */
    @NotNull(message = "无参数圈子id")
    @Schema(description = "圈子id")
    private Integer topicId;

    /**
     * 地址名称
     */
    @Schema(description = "地址名称")
    private String address;

    /**
     * 内容
     */
    @Length(max = 400, message = "内容不能超过400个字符")
    @NotBlank(message = "参数有误")
    @Schema(description = "内容")
    private String content;

    /**
     * 标题
     */
    @Length(max = 20, message = "标题不能超过20个字符")
    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题")
    private String voteTitle;

    /**
     * 1单选2多选
     */
    @NotNull(message = "无参数type")
    @Schema(description = "1单选2多选")
    private Integer voteType;

    /**
     * 到期时间
     * 1:一天    2:7天   3:30天    4:90天
     */
    @Schema(description = "到期时间 1:一天 2:7天 3:30天 4:90天")
    private Integer expireTime;


    @Schema(description = "投票选项")
    private List<VoteOptionForm> voteOptions;


}
