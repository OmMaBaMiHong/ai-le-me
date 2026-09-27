package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_distillation_snapshot")
public class AgentDistillationSnapshotEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer subjectId;

    private String sceneType;

    private String summary;

    private String analysisGoal;

    private String previewJson;

    private Integer materialCount;

    private String source;

    private Integer status;

    private LocalDateTime generatedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
