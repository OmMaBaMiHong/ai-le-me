package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "HongniangTopicInfoResponse", description = "主理人圈子关联主理人摘要")
public class HongniangTopicInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主理人ID")
    private Integer id;

    @Schema(description = "关联用户ID")
    private Integer userId;

    @Schema(description = "主理人姓名")
    private String hongniangName;

    @Schema(description = "主理人头像")
    private String avatar;

    @Schema(description = "主理人等级")
    private Integer level;

    @Schema(description = "所属机构")
    private String companyName;

    @Schema(description = "服务地区")
    private String serviceArea;
}
