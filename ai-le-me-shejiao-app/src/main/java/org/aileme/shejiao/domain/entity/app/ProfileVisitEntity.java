package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 个人主页访客埋点
 */
@Data
@Entity
@Table(name = "profile_visit")
@TableName("profile_visit")
public class ProfileVisitEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId
    private Integer id;

    /**
     * 访客uid
     */
    private Integer visitorUid;

    /**
     * 被访问者uid
     */
    private Integer targetUid;

    /**
     * 累计访问次数
     */
    private Integer visitCount;

    /**
     * 累计停留秒数
     */
    private Integer totalDuration;

    /**
     * 最近一次访问时间
     */
    private Date lastVisitTime;

    /**
     * 兴趣等级: 0普通浏览 1轻度兴趣 2中度兴趣 3高度兴趣
     */
    private Integer interestLevel;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
