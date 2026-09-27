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
package org.aileme.shejiao.common.thread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池装配类
 * @author linfeng
 * @date 2022/2/20 22:04
 */
@Configuration
public class AsyncTaskExecutePool implements AsyncConfigurer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncTaskExecutePool.class);

    /** 注入配置类 */
    private final AsyncTaskProperties config;

    public AsyncTaskExecutePool(AsyncTaskProperties config) {
        this.config = config;
    }

    @Override
    public Executor getAsyncExecutor() {
        log.info("初始化异步线程池 - corePoolSize: {}, maxPoolSize: {}, queueCapacity: {}, keepAliveSeconds: {}",
            config.getCorePoolSize(), config.getMaxPoolSize(), config.getQueueCapacity(), config.getKeepAliveSeconds());
            
        // 验证参数合法性
        if (config.getCorePoolSize() <= 0) {
            log.error("核心线程数不能小于等于 0，当前值：{}", config.getCorePoolSize());
            throw new IllegalArgumentException("核心线程数必须大于 0，当前值：" + config.getCorePoolSize());
        }
        if (config.getMaxPoolSize() <= 0) {
            log.error("最大线程数不能小于等于 0，当前值：{}", config.getMaxPoolSize());
            throw new IllegalArgumentException("最大线程数必须大于 0，当前值：" + config.getMaxPoolSize());
        }
        if (config.getQueueCapacity() <= 0) {
            log.error("队列容量不能小于等于 0，当前值：{}", config.getQueueCapacity());
            throw new IllegalArgumentException("队列容量必须大于 0，当前值：" + config.getQueueCapacity());
        }
            
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程池大小
        executor.setCorePoolSize(config.getCorePoolSize());
        //最大线程数
        executor.setMaxPoolSize(config.getMaxPoolSize());
        //队列容量
        executor.setQueueCapacity(config.getQueueCapacity());
        //活跃时间
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        //线程名字前缀
        executor.setThreadNamePrefix("lf-async-");
        // setRejectedExecutionHandler：当 pool 已经达到 max size 的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("异步线程池初始化成功");
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            log.error("===="+throwable.getMessage()+"====", throwable);
            log.error("exception method:"+method.getName());
        };
    }
}
