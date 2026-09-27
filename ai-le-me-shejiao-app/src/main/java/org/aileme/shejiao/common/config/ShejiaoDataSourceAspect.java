package org.aileme.shejiao.common.config;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Shejiao 模块数据源切面
 * 
 * <p>自动将 org.aileme.shejiao 包下所有 DAO/Mapper 的数据源切换到 master 数据源。
 * 
 * <p>优先级：
 * <ul>
 *   <li>如果 DAO 上有 @DS 注解，优先使用注解指定的数据源</li>
 *   <li>如果没有 @DS 注解，自动使用 master 数据源</li>
 * </ul>
 * 
 * @author system
 * @date 2026-02-04
 */
@Aspect
@Component
@Order(1)  // 优先级最高，在事务切面之前执行
public class ShejiaoDataSourceAspect {
    
    private static final Logger log = LoggerFactory.getLogger(ShejiaoDataSourceAspect.class);
    
    private static final String DEFAULT_DATASOURCE = "master";
    
    /**
     * 切点：拦截 org.aileme.shejiao 包下所有 DAO/Mapper 接口的方法
     */
    @Pointcut("execution(* org.aileme.shejiao..dao..*(..)) || execution(* org.aileme.shejiao..mapper..*(..))")
    public void shejiaoMapperMethods() {}
    
    /**
     * 环绕通知：在方法执行前切换数据源，执行后恢复
     */
    @Around("shejiaoMapperMethods()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取当前数据源
        String currentDataSource = DynamicDataSourceContextHolder.peek();
        
        try {
            // 如果当前没有指定数据源（即没有 @DS 注解），使用默认的 master 数据源
            if (currentDataSource == null) {
                DynamicDataSourceContextHolder.push(DEFAULT_DATASOURCE);
                
                if (log.isDebugEnabled()) {
                    log.debug("🔄 Shejiao Mapper 自动切换数据源: {} -> {}", 
                        joinPoint.getSignature().toShortString(), DEFAULT_DATASOURCE);
                }
            }
            
            // 执行目标方法
            return joinPoint.proceed();
        } finally {
            // 如果是本切面设置的数据源，执行后清理
            if (currentDataSource == null) {
                DynamicDataSourceContextHolder.poll();
            }
        }
    }
}
