package org.aileme.shejiao.common.aspect;

import org.aileme.shejiao.common.enums.LimitType;
import org.aileme.shejiao.common.annotation.Limit;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.IPUtil;
import org.aileme.shejiao.common.utils.RedisLimitUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.lang.reflect.Method;
import org.aspectj.lang.reflect.MethodSignature;
/**
 * 限流处理
 * @author linfeng
 * @date 2022/10/13 19:55
 */
@Slf4j
@Aspect
@Component
public class LimitAspect {

    @Autowired
    private RedisLimitUtil redisLimitUtil;

    /**
     * 前置通知，判断是否超出限流次数
     *
     * @param point
     */
    @Before("@annotation(limit)")
    public void doBefore(JoinPoint point, Limit limit) {
        try {
            // 拼接key
            String key = getCombineKey(limit, point);
            // 判断是否超出限流次数
            if (!redisLimitUtil.limit(key, limit.count(), limit.time())) {
                throw new LinfengException("访问过于频繁请稍候再试");
            }
        } catch (LinfengException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("接口限流异常请稍候再试");
        }
    }

    /**
     * 根据限流类型拼接key
     */
    public String getCombineKey(Limit limit, JoinPoint point) {
        StringBuilder sb = new StringBuilder(limit.prefix());
        // 按照IP限流
        if (limit.type() == LimitType.IP) {
            sb.append(IPUtil.getIp(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest())).append("-");
        }
        // 拼接类名和方法名
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        sb.append(targetClass.getName()).append("-").append(method.getName());
        return sb.toString();
    }
}
