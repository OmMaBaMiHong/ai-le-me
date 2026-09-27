package org.aileme.shejiao.domain.enums;

import lombok.Getter;

/**
 * 用户类型状态枚举
 * 
 * @author linfeng
 */
@Getter
public enum UserTypeStatus {
    /**
     * 真实用户
     */
    REAL(0, "真实用户"),
    /**
     * 虚拟用户/机器人
     */
    VIRTUALLY(1, "虚拟用户");

    private final Integer value;
    private final String desc;

    UserTypeStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
