package org.aileme.shejiao.common.config;

import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * Shejiao模块自动配置
 * 
 * <p>功能说明：
 * <ul>
 *   <li>通过 shejiao.enabled 控制模块启用/禁用（默认启用）</li>
 *   <li>自动扫描 org.aileme.shejiao 包下的所有组件</li>
 *   <li>所有配置参数在 application-*.yml 中以 shejiao.* 前缀定义</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * # application.yml
 * shejiao:
 *   enabled: true  # 启用模块（默认值）
 *   wx:
 *     appid: your-appid
 *     secret: your-secret
 * </pre>
 * 
 * @author system
 * @date 2026-02-04
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "shejiao",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true  // 如果没有配置，默认启用
)
@ComponentScan(basePackages = {
    "org.aileme.shejiao.admin",      // 管理端Controller
    "org.aileme.shejiao.app",        // 移动端Controller
    "org.aileme.shejiao.api",        // Service接口和实现
    "org.aileme.shejiao.config",     // 配置类
    "org.aileme.shejiao.common",     // 公共工具类
    "org.aileme.shejiao.sys",        // 系统管理
    "org.aileme.shejiao.oss"         // 对象存储
})
@MapperScan(basePackages = {
    "org.aileme.shejiao.admin.dao",      // shejiao管理端Mapper
    "org.aileme.shejiao.app.dao",        // shejiao移动端Mapper
    "org.aileme.shejiao.sys.mapper",     // shejiao系统Mapper
    "org.aileme.shejiao.oss.dao",        // shejiao OSS Mapper
    "org.aileme.system.mapper" // ai-le-me系统Mapper
})
public class ShejiaoAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(ShejiaoAutoConfiguration.class);
    
    @Value("${shejiao.enabled:true}")
    private boolean enabled;
    
    @PostConstruct
    public void init() {
        log.info("==============================================");
        log.info("   Shejiao模块自动配置已加载");
        log.info("   扫描包: org.aileme.shejiao.*");
        log.info("   Mapper扫描: org.aileme.shejiao.*.dao");
        log.info("   配置方式: application-*.yml (shejiao.*)");
        log.info("   数据源: 默认切换到 master，当前已收敛为单库模式");
        log.info("   模块状态: {}", enabled ? "已启用" : "已禁用");
        log.info("==============================================");
    }
}
