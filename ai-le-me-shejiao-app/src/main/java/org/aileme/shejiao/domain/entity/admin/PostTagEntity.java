package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 帖子话题关联表
 */
@Data
@TableName("post_tags")
public class PostTagEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 帖子ID
     */
    private Integer postId;

    /**
     * 话题ID
     */
    private Integer tagId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
