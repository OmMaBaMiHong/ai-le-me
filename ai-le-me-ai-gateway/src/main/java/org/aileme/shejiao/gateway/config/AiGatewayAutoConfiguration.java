package org.aileme.shejiao.gateway.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * AI网关自动装配
 */
@AutoConfiguration
@EnableConfigurationProperties(AiGatewayProperties.class)
@ComponentScan(basePackages = "org.aileme.shejiao.gateway")
public class AiGatewayAutoConfiguration {
}
