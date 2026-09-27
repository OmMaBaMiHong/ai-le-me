package org.aileme.shejiao.common.annotation;

import org.aileme.shejiao.common.enums.LimitType;

import java.lang.annotation.*;

/**
 * 接口限流自定义注解
 * @author linfeng
 * @date 2022/10/13 19:52
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Limit {

    /**
     * 限流key前缀
     */
    String prefix() default "limit:";

    /**
     * 限流时间,单位秒
     */
    int time() default 60;

    /**
     * 限流次数
     */
    int count() default 10;

    /**
     * 限流类型
     */
    LimitType type() default LimitType.IP;
}