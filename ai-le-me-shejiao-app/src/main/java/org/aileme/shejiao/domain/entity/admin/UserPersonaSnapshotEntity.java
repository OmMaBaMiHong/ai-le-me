package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户画像快照实体
 *
 * @author system
 * @date 2026-02-14
 */
@Data
@TableName("user_persona_snapshot")
public class UserPersonaSnapshotEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 画像快照ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 画像结构化数据（JSON格式）
     */
    private String personaJson;

    /**
     * 画像图片URL
     */
    private String imageUrl;

    /**
     * 一句话总结
     */
    private String summary;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 生成来源：manual_generate/from_ai_video
     */
    private String source;

    /**
     * 是否对本人可见：0-否 1-是
     */
    private Integer visibleToSelf;

    /**
     * 是否对他人可见：0-否 1-是
     */
    private Integer visibleToOthers;

    /**
     * 状态：0-已删除 1-正常
     */
    private Integer status;

    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
