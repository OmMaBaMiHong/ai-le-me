/**
 * -----------------------------------
 *  Copyright (c) 2021-2023
 *  All rights reserved, Designed By my.hots.love
 *  
 *  商业版授权联系技术客服	 QQ:  3582996245
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.resolver;

import java.time.Duration;

import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.app.interceptor.AuthorizationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 有@LoginUser注解的方法参数，注入当前登录用户
 */
@Component
public class LoginUserHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {


    @Autowired
    private AppUserService userService;
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().isAssignableFrom(AppUserEntity.class) && parameter.hasParameterAnnotation(LoginUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                  NativeWebRequest request, WebDataBinderFactory factory) throws Exception {
        //获取用户ID
        Object object = request.getAttribute(AuthorizationInterceptor.USER_KEY, RequestAttributes.SCOPE_REQUEST);
        if (object == null) {
            return null;
        }

        Integer userId;
        if (object instanceof Number number) {
            userId = number.intValue();
        } else {
            userId = Integer.parseInt(String.valueOf(object));
        }
        
        // 从 Redis 缓存中获取用户信息
        String cacheKey = "userId:" + userId;
        Object cachedObject = org.aileme.common.redis.utils.RedisUtils.getCacheObject(cacheKey);
        
        // 检查缓存对象的类型
        if (cachedObject != null) {
            // 如果是 AppUserEntity 类型，直接返回
            if (cachedObject instanceof AppUserEntity) {
                return cachedObject;
            }
            // 如果是其他类型（如旧的 String 类型），删除旧缓存
            org.aileme.common.redis.utils.RedisUtils.deleteObject(cacheKey);
        }
        
        // 缓存未命中或类型错误，从数据库重新获取用户信息
        AppUserEntity user = userService.getById(userId);
        if (user != null) {
            // 缓存用户对象（而不是字符串）
            org.aileme.common.redis.utils.RedisUtils.setCacheObject(cacheKey, user, Duration.ofSeconds(7200));
        }
        return user;
    }
}
