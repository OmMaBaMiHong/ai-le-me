package org.aileme.common.doc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置
 */
//@AutoConfiguration(before = SpringDocConfiguration.class)
@Configuration
public class SpringDocConfig {

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI openApi() {
        // Sa-Token 认证方案：通过 Authorization 请求头直接传 token（不带 Bearer 前缀）
        SecurityScheme tokenScheme = new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .name("Authorization")
            .in(SecurityScheme.In.HEADER)
            .description("Sa-Token 认证，直接填入 token 值");

        // clientid 请求头
        SecurityScheme clientIdScheme = new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .name("clientid")
            .in(SecurityScheme.In.HEADER)
            .description("客户端ID");

        return new OpenAPI()
            .info(new Info()
                .title("爱了么 API 文档")
                .description("基于 Spring Boot 3.5 + Sa-Token + SpringDoc OpenAPI 3 的接口文档")
                .version("1.0.1")
                .contact(new Contact()
                    .name("全城热恋·卡颜部落")
                    .url("https://wo.ai-ni.store")
                    .email("1651055684@qq.com")))
            .components(new Components()
                .addSecuritySchemes("Authorization", tokenScheme)
                .addSecuritySchemes("clientid", clientIdScheme))
            .addSecurityItem(new SecurityRequirement().addList("Authorization").addList("clientid"));
    }

    /**
     * 通用模块
     */
    @Bean
    public GroupedOpenApi webApi() {
        return GroupedOpenApi.builder()
            .group("1.通用模块")
            .packagesToScan("org.aileme.web")
            .build();
    }

    /**
     * 系统模块
     */
    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
            .group("2.系统模块")
            .packagesToScan("org.aileme.system")
            .build();
    }

    /**
     * App接口
     */
    @Bean
    public GroupedOpenApi appApi() {
        return GroupedOpenApi.builder()
            .group("3.App接口")
            .packagesToScan("org.aileme.shejiao.app")
            .build();
    }

    /**
     * 后台管理接口（社交业务）
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("4.后台管理")
            .packagesToScan("org.aileme.shejiao.admin")
            .build();
    }
}
