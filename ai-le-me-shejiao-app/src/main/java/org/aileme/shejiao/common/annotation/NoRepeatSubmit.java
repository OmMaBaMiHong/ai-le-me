/**
 * -----------------------------------
 *  Copyright (c) 2021org.aileme.shejiao2023
 *  All rights reserved, Designed By my.hots.love
 *  
 *  商业版授权联系技术客服	 QQ:  3582996245
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.common.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 防止重复请求注解
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface NoRepeatSubmit {
    /**
     * 默认时间  默认1.5秒钟
     *
     */
    int lockTime() default 1500;

}