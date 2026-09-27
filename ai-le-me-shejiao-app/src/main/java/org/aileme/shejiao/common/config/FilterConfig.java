/**
 * -----------------------------------
 *  Copyright (c) 2021-2023
 *  All rights reserved, Designed By my.hots.love
 *
 *  商业版授权联系技术客服	  wx:  lwwmmzh
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.common.config;

import org.aileme.shejiao.common.xss.XssFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.DelegatingFilterProxy;

import jakarta.servlet.DispatcherType;
import java.util.Map;

/**
 * Filter配置
 *
 */
@Configuration
public class FilterConfig {



//    @Bean
//    public FilterRegistrationBean xssFilterRegistration() {
//        FilterRegistrationBean registration = new FilterRegistrationBean();
//        registration.setDispatcherTypes(DispatcherType.REQUEST);
//        registration.setFilter(new XssFilter());
//        registration.addUrlPatterns("/*");
//        registration.setName("xssFilter");
//        registration.setOrder(Integer.MAX_VALUE);
//        Map<String, String> initParameters = Maps.newHashMap();
//        initParameters.put("excludes", "/app/post/addArticle");
//        initParameters.put("isIncludeRichText", "false");
//        registration.setInitParameters(initParameters);
//        return registration;
//    }
}
