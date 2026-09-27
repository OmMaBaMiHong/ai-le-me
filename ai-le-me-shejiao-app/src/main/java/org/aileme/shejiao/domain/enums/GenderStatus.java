package org.aileme.shejiao.domain.enums;

import lombok.Getter;

/**
 * 性别状态枚举
 * 
 * @author linfeng
 */
@Getter
public enum GenderStatus {
    /**
     * 未知
     */
    UNKNOWN(0, "未知"),
    /**
     * 男
     */
    MALE(1, "男"),
    /**
     * 女
     */
    FEMALE(2, "女");

    private final Integer value;
    private final String desc;

    GenderStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
