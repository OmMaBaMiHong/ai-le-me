/**
 * -----------------------------------
 *  Copyright (c) 2021-2023

 * 版权所有 ，侵权必究！
 * -----------------------------------
 */

package org.aileme.shejiao.app.annotation;

import java.lang.annotation.*;

/**
 * app登录效验
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Login {
}
