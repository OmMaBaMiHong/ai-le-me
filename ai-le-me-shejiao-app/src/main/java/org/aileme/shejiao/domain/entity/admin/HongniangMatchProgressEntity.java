package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "hongniang_match_progress")
@TableName("hongniang_match_progress")
@Schema(description = "红娘牵线案件进度")
public class HongniangMatchProgressEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer caseId;

    private Integer progressType;

    private Integer stageBefore;

    private Integer stageAfter;

    private String content;

    private Date plannedFollowTime;

    private Date actualFollowTime;

    private Long operatorId;

    private String attachments;

    private Date createTime;
}
