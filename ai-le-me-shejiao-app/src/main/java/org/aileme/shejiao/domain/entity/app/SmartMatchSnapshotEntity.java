package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("smart_match_snapshot")
public class SmartMatchSnapshotEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer userId;

    private String featureHash;

    private String provider;

    private String insight;

    private String selectedCitiesJson;

    private Integer usedRuntime;

    private Integer candidateCount;

    private Integer versionNo;

    private Integer status;

    private LocalDateTime generatedAt;

    private LocalDateTime expireAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
