
package org.aileme.shejiao.domain.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 系统验证码
 *
 */
@Data
@TableName("sys_captcha")
@Entity
@Table(name = "sys_captcha")
public class SysCaptchaEntity {
    @Id
    @TableId(type = IdType.INPUT)
    private String uuid;
    /**
     * 验证码
     */
    private String code;
    /**
     * 过期时间
     */
    private Date expireTime;

}
