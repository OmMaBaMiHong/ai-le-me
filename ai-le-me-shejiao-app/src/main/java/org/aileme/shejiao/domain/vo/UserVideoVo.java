package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视频VO
 *
 * @author system
 * @date 2026-02-13
 */
@Data
@Schema(name = "UserVideoVo", description = "用户视频")
public class UserVideoVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "视频ID")
    private Integer id;

    @Schema(description = "视频标题")
    private String title;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "视频URL")
    private String videoUrl;

    @Schema(description = "视频时长(秒)")
    private Integer duration;

    @Schema(description = "视频类型：1-自我介绍 2-相亲名片")
    private Integer videoType;

    @Schema(description = "状态：0-生成中 1-待发布 2-已发布 3-失败")
    private Integer status;

    @Schema(description = "进度百分比")
    private Integer progress;

    @Schema(description = "关联帖子ID")
    private Integer postId;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "错误信息")
    private String errorMsg;
}
