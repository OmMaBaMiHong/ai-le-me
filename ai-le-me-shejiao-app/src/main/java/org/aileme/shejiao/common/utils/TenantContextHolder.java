package org.aileme.shejiao.common.utils;

/**
 * 租户上下文管理器
 * 用于在请求处理过程中传递和管理租户ID
 */
public class TenantContextHolder {
    
    private static final ThreadLocal<Long> CONTEXT = new ThreadLocal<>();
    
    /**
     * 设置当前线程的租户ID
     */
    public static void setTenantId(Long tenantId) {
        CONTEXT.set(tenantId);
    }
    
    /**
     * 获取当前线程的租户ID
     */
    public static Long getTenantId() {
        return CONTEXT.get();
    }
    
    /**
     * 清除当前线程的租户ID
     */
    public static void clear() {
        CONTEXT.remove();
    }
    
    /**
     * 检查是否存在租户ID
     */
    public static boolean hasTenantId() {
        return CONTEXT.get() != null;
    }
}