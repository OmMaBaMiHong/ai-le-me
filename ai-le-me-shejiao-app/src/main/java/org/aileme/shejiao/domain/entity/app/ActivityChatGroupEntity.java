package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("activity_chat_group")
public class ActivityChatGroupEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("activity_id")
    private Integer activityId;

    private String title;

    @TableField("cover_img")
    private String coverImg;

    @TableField("organizer_uid")
    private Integer organizerUid;

    private Integer status;

    @TableField("last_message")
    private String lastMessage;

    @TableField("last_message_time")
    private Date lastMessageTime;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
