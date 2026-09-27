package org.aileme.shejiao.domain.enums;

import lombok.Getter;

/**
 * 评论状态枚举
 * 
 * @author linfeng
 */
@Getter
public enum CommentStatus {
    /**
     * 待审核
     */
    AUDIT(0, "待审核"),
    /**
     * 正常
     */
    NORMAL(1, "正常"),
    /**
     * 已关闭/已删除
     */
    OFF(2, "已关闭");

    private final Integer value;
    private final String desc;

    CommentStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
