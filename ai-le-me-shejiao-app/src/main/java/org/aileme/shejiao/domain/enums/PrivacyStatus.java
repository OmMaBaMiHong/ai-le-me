package org.aileme.shejiao.domain.enums;

import lombok.Getter;

/**
 * 隐私状态枚举
 * 
 * @author linfeng
 */
@Getter
public enum PrivacyStatus {
    /**
     * 公开
     */
    PUBLIC(0, "公开"),
    /**
     * 私密
     */
    PRIVACY(1, "私密");

    private final Integer value;
    private final String desc;

    PrivacyStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
