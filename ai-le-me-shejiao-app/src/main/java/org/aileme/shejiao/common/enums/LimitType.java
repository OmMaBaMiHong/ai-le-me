package org.aileme.shejiao.common.enums;
/**
 *
 * 限流类型
 * @author linfeng
 * @date 2022/10/13 19:53
 */

public enum LimitType {
    /**
     * 默认策略全局限流
     */
    DEFAULT,

    /**
     * 根据请求者IP进行限流
     */
    IP
}
