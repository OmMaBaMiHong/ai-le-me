package org.aileme.common.encrypt.config;

import jakarta.servlet.DispatcherType;
import org.aileme.common.encrypt.filter.CryptoFilter;
import org.aileme.common.encrypt.properties.ApiDecryptProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * api 解密自动配置
 *
 * @author wdhcr
 */
@AutoConfiguration
@EnableConfigurationProperties(ApiDecryptProperties.class)
@ConditionalOnProperty(value = "api-decrypt.enabled", havingValue = "true")
public class ApiDecryptAutoConfiguration {

    @Bean
    public CryptoFilter cryptoFilter(ApiDecryptProperties properties) {
        return new CryptoFilter(properties);
    }

    @Bean
    public FilterRegistrationBean<CryptoFilter> cryptoFilterRegistration(CryptoFilter cryptoFilter) {
        FilterRegistrationBean<CryptoFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(cryptoFilter);
        registration.setName("cryptoFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        return registration;
    }

}