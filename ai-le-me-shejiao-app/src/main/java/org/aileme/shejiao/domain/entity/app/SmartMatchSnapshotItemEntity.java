package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("smart_match_snapshot_item")
public class SmartMatchSnapshotItemEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long snapshotId;

    private Integer ownerUserId;

    private Integer targetUserId;

    private Integer rankNo;

    private Integer matchScore;

    private String provider;

    private String candidateJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
