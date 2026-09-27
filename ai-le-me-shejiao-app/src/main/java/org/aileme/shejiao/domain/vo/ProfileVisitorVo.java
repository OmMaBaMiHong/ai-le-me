package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 访客信息VO
 */
@Data
@Schema(title = "ProfileVisitorVo", description = "访客信息")
public class ProfileVisitorVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(title = "访客uid")
    private Integer visitorUid;

    @Schema(title = "用户名")
    private String username;

    @Schema(title = "头像")
    private String avatar;

    @Schema(title = "性别")
    private Integer gender;

    @Schema(title = "年龄")
    private Integer age;

    @Schema(title = "职业")
    private String job;

    @Schema(title = "累计访问次数")
    private Integer visitCount;

    @Schema(title = "累计停留秒数")
    private Integer totalDuration;

    @Schema(title = "兴趣等级: 0普通浏览 1轻度兴趣 2中度兴趣 3高度兴趣")
    private Integer interestLevel;

    @Schema(title = "最近访问时间")
    private Date lastVisitTime;

    @Schema(title = "是否VIP")
    private Integer vip;
}
