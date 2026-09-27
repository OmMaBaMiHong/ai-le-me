/**
 * -----------------------------------
 *  Copyright (c) 2021-2023
 *
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.config;

import org.aileme.shejiao.app.interceptor.AuthorizationInterceptor;
import org.aileme.shejiao.app.resolver.LoginUserHandlerMethodArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC配置
 * 适配 Sa-Token，启用 @Login 和 @LoginUser 注解功能
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthorizationInterceptor authorizationInterceptor;

    @Autowired
    private LoginUserHandlerMethodArgumentResolver loginUserHandlerMethodArgumentResolver;

    @Value("${shejiao.linfeng.path.image:./upload/image/}")
    private String imagePath;

    @Value("${shejiao.linfeng.path.file:./upload/file/}")
    private String filePath;

    @Value("${shejiao.linfeng.path.video:./upload/video/}")
    private String videoPath;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 @Login 注解拦截器
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/app/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // 注册 @LoginUser 注解参数解析器
        argumentResolvers.add(loginUserHandlerMethodArgumentResolver);
    }

    /**
     * 图片和文件的物理地址
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 不使用默认的静态资源处理器，避免 /** 拦截所有请求
        registry.addResourceHandler("/resource/image/**").addResourceLocations("file:"+imagePath);
        registry.addResourceHandler("/resource/file/**").addResourceLocations("file:"+filePath);
        registry.addResourceHandler("/resource/video/**").addResourceLocations("file:"+videoPath);

        // Knife4j doc.html + Swagger UI 静态资源
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/swagger-ui/**").addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");

        // 不添加默认资源处理器
        registry.setOrder(Integer.MAX_VALUE);
    }

}