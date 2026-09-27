package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("activity_chat_member")
public class ActivityChatMemberEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("group_id")
    private Integer groupId;

    @TableField("activity_id")
    private Integer activityId;

    @TableField("user_id")
    private Integer userId;

    @TableField("enrollment_id")
    private Integer enrollmentId;

    private Integer role;

    private Integer status;

    @TableField("join_time")
    private Date joinTime;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
