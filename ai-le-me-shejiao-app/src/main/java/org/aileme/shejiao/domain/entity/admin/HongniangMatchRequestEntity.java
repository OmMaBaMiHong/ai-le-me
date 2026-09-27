package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@Table(name = "hongniang_match_request")
@TableName("hongniang_match_request")
public class HongniangMatchRequestEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer caseId;

    private Integer hongniangId;

    private Integer fromUserId;

    private Integer toUserId;

    private Integer requestChannel;

    private Integer requestStatus;

    private String requestMessage;

    private String intentRequestId;

    private String wechatShareSnapshot;

    private Date expireTime;

    private Date createTime;

    private Date updateTime;
}
