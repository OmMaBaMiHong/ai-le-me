package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 画像报告
 */
@Data
@Schema(name = "PersonaReportVO", description = "用户画像下载报告")
public class PersonaReportVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "报告ID")
    private String reportId;

    @Schema(description = "用户ID")
    private Integer userId;

    @Schema(description = "聊过人数")
    private Integer chattedUserCount;

    @Schema(description = "发送消息数")
    private Integer sentMessageCount;

    @Schema(description = "接收消息数")
    private Integer receivedMessageCount;

    @Schema(description = "活跃聊天天数")
    private Integer activeChatDays;

    @Schema(description = "性格倾向")
    private String personality;

    @Schema(description = "兴趣爱好")
    private List<String> hobbies = new ArrayList<>();

    @Schema(description = "特长优势")
    private List<String> strengths = new ArrayList<>();

    @Schema(description = "单身时长")
    private String singleDuration;

    @Schema(description = "报告摘要")
    private String summary;

    @Schema(description = "完整报告文本")
    private String reportText;

    @Schema(description = "生成时间")
    private String generatedAt;
}
