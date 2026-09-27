package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.persistence.*;

import java.util.Date;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 个人主页浏览记录
 * </p>
 *
 * @author lww
 * @since 2023-05-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(name="UserScans对象", description="个人主页浏览记录")
@Entity
@jakarta.persistence.Table(name = "user_scans")
@TableName("user_scans")
public class UserScans implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
      @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer uid;

    private Integer scanUid;

    private Integer scanNums;

    private String lookerAvatar;

      @TableField(fill = FieldFill.INSERT)
    private Date createTime;


}
