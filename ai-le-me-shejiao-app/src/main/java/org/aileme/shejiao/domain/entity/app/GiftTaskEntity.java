package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "gift_task")
@TableName("gift_task")
public class GiftTaskEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String requestId;

    private Integer senderUid;

    private Integer targetUid;

    private Integer postId;

    private String sessionId;

    private Integer rewardAmount;

    private String giftCode;

    private String giftName;

    private String giftIcon;

    private String giftScene;

    private String giftTheme;

    private String templateCode;

    private String animationPreset;

    private String revealEffect;

    private String soundEffectKey;

    private String soundEffectUrl;

    private String relationshipStage;

    private String provider;

    private String strategyNote;

    private String reason;

    private String note;

    @Lob
    private String visualPrompt;

    @Lob
    private String motionPrompt;

    @Lob
    private String referenceMedia;

    @Lob
    private String generatedMedia;

    private Integer giftStatus;

    private Integer taskStatus;

    private String statusNote;

    private Date createTime;

    private Date updateTime;
}
