package org.aileme.shejiao.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 自定义配置（适配 Shiro Token 认证）
 * 
 * 注意：已被主框架的 SpringDocConfig 替代，这里禁用避免 Bean 冲突
 */
//@Configuration
public class SpringDocConfig {

    /**
     * 配置 API 文档的 Token 认证参数（Swagger UI 中可输入 Token）
     */
    //@Bean
    public OpenAPI customOpenAPI() {
        // 定义 Token 认证方案
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name("Authorization")
                .in(SecurityScheme.In.HEADER);

        // 添加全局 Token 认证
        return new OpenAPI()
                .info(new Info()
                        .title("三维矩阵世界 API 文档")
                        .description("基于 Spring Boot 3.2 + SpringDoc OpenAPI 3 的接口文档")
                        .version("1.0.1")
                        .contact(new Contact()
                                .name("全城热恋·卡颜部落")
                                .url("https://my.hots.love")
                                .email(" 1651055684@qq.com")))
                .components(new Components().addSecuritySchemes("BearerAuth", securityScheme))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}