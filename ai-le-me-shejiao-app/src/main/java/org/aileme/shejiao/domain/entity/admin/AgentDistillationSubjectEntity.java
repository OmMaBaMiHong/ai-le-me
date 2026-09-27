package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_distillation_subject")
public class AgentDistillationSubjectEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String sceneType;

    private String subjectType;

    private String subjectName;

    private String relationLabel;

    private Integer lastSnapshotId;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
