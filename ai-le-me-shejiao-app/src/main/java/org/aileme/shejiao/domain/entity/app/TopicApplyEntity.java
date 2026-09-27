package org.aileme.shejiao.domain.entity.app;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
@Data
@Entity
@Table(name = "topic_apply")
@TableName("topic_apply")
public class TopicApplyEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer topicId;
    private String answer;
    private String question;
    private Integer uid;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
