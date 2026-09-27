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
package org.aileme.shejiao.app.interceptor;


import cn.dev33.satoken.stp.StpUtil;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.app.annotation.Login;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aileme.shejiao.common.utils.JwtUtils;

/**
 * 权限(Token)验证
 * 适配 Sa-Token，处理 @Login 注解
 */
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtils jwtUtils;

    public static final String USER_KEY = "userId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Login annotation;
        if(handler instanceof HandlerMethod) {
            annotation = ((HandlerMethod) handler).getMethodAnnotation(Login.class);
        }else{
            return true;
        }

        if(annotation == null){
            return true;
        }

        // 使用 Sa-Token 验证登录状态
        try {
            // 检查是否已登录
            if (!StpUtil.isLogin()) {
                throw new LinfengException("token不存在或已失效，请重新登录", HttpStatus.UNAUTHORIZED.value());
            }
            
            // 从 Sa-Token 获取 loginId（即 userId）
            Object loginId = StpUtil.getLoginId();
            if (loginId == null) {
                throw new LinfengException("token失效，请重新登录", HttpStatus.UNAUTHORIZED.value());
            }
            
            // 设置 userId 到 request 属性，后续 LoginUserHandlerMethodArgumentResolver 使用
            Integer userId = Integer.parseInt(String.valueOf(loginId));
            request.setAttribute(USER_KEY, userId);
            
            return true;
            
        } catch (Exception e) {
            // Sa-Token 抛出的异常转换为 LinfengException
            throw new LinfengException("token验证失败: " + e.getMessage(), HttpStatus.UNAUTHORIZED.value());
        }
    }
}
