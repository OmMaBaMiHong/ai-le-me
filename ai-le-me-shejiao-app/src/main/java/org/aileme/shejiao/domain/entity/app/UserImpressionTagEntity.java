package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户印象标签记录
 */
@Data
@Entity
@Table(name = "user_impression_tag")
@TableName("user_impression_tag")
public class UserImpressionTagEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 打标签人用户ID
     */
    private Integer fromUserId;

    /**
     * 被打标签人用户ID
     */
    private Integer toUserId;

    /**
     * 标签ID
     */
    private Integer tagId;

    /**
     * 来源类型：PROFILE/CONTENT/COMMENT 等
     */
    private String sourceType;

    /**
     * 来源对象ID（帖子ID、评论ID等，可为空）
     */
    private Integer sourceId;

    /**
     * 创建时间
     */
    private Date createTime;
}
