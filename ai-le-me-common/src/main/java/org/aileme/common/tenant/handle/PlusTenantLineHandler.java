package org.aileme.common.tenant.handle;

import cn.hutool.core.collection.ListUtil;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import org.aileme.common.core.utils.StringUtils;
import org.aileme.common.satoken.utils.LoginHelper;
import org.aileme.common.tenant.helper.TenantHelper;
import org.aileme.common.tenant.properties.TenantProperties;

import java.util.List;

/**
 * 自定义租户处理器
 *
 * @author Lion Li
 */
@Slf4j
@AllArgsConstructor
public class PlusTenantLineHandler implements TenantLineHandler {

    /**
     * 单库模式下仍然沿用 shejiao 业务表的历史租户策略：
     * 仅 hongniang_info 注入 tenant_id，其他社交业务表默认忽略租户注入。
     */
    private static final List<String> SHEJIAO_SINGLE_DB_TABLES = ListUtil.toList(
        "account",
        "account_bill",
        "activity_chat_group",
        "activity_chat_member",
        "activity_chat_message",
        "app_event_metric_daily",
        "app_event_track",
        "cash_out",
        "category",
        "chat_message",
        "comment",
        "comment_thumbs",
        "discuss",
        "follow",
        "friend",
        "hongniang_apply",
        "hongniang_info",
        "hongniang_user_relation",
        "link",
        "luckdraw",
        "luckdraw_record",
        "message",
        "name_change",
        "navigation",
        "notice",
        "os_sensitive",
        "pay_order",
        "pay_order_detail",
        "pay_product",
        "persona_view_record",
        "post",
        "post_collection",
        "post_fabulous",
        "post_tags",
        "profile_visit",
        "recommend_love",
        "report",
        "search",
        "sign_config",
        "sys_region",
        "sys_university",
        "tags",
        "tb_moment",
        "tenant_config",
        "topic",
        "topic_admin",
        "topic_apply",
        "topic_block",
        "topic_top",
        "user",
        "user_impression_tag",
        "user_level",
        "user_menu",
        "user_persona_snapshot",
        "user_recharge_refund",
        "user_scans",
        "user_setting",
        "user_sign",
        "user_tags",
        "user_topic",
        "user_video",
        "video_template",
        "vip_benefit",
        "vote_option",
        "vote_result",
        "vote_subject",
        "xiangqin_activity",
        "xiangqin_enrollment"
    );

    private static final List<String> SHEJIAO_TENANT_TABLES = ListUtil.toList(
        "hongniang_info"
    );

    private final TenantProperties tenantProperties;

    @Override
    public Expression getTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            log.error("无法获取有效的租户id -> Null");
            return new NullValue();
        }
        // 返回固定租户
        return new StringValue(tenantId);
    }

    @Override
    public boolean ignoreTable(String tableName) {
        String tenantId = TenantHelper.getTenantId();
        // 判断是否有租户
        if (StringUtils.isNotBlank(tenantId)) {
            // ============ 超级管理员和租户管理员豁免 ============
            // 超级管理员和租户管理员可以查看所有租户的数据
            // 注意：必须先检查是否已登录，避免在登录过程中抛出异常
            try {
                if (LoginHelper.isLogin() && (LoginHelper.isSuperAdmin() || LoginHelper.isTenantAdmin())) {
                    log.debug("🔓 [租户拦截] 超级管理员/租户管理员，跳过租户拦截，表: {}", tableName);
                    return true;
                }
            } catch (Exception e) {
                // 登录过程中或 token 失效时，忽略此异常，继续执行租户拦截逻辑
                log.debug("⚠️  [租户拦截] 检查用户权限时出错，继续执行租户拦截: {}", e.getMessage());
            }
            
            // ============ 动态数据源判断 ============
            // 获取当前线程使用的数据源名称
            String currentDataSource = DynamicDataSourceContextHolder.peek();
            
            // 调试日志：输出当前判断信息
            log.debug("租户拦截判断 - 表名: {}, 数据源: {}, 租户ID: {}", tableName, currentDataSource, tenantId);
            
            // 单库模式下仍保留 shejiao 业务表的历史租户语义
            if ("app".equals(currentDataSource)
                || StringUtils.equalsAnyIgnoreCase(tableName, SHEJIAO_SINGLE_DB_TABLES.toArray(new String[0]))) {
                // 如果是白名单表，不忽略（注入 tenant_id）
                if (StringUtils.equalsAnyIgnoreCase(tableName, SHEJIAO_TENANT_TABLES.toArray(new String[0]))) {
                    log.info("✅ [租户拦截] 表 [{}] 在 shejiao 白名单中，注入租户ID: {}", tableName, tenantId);
                    return false;
                }
                // 其他 shejiao 业务表全部忽略（不注入 tenant_id）
                log.debug("⏭️  [租户拦截] 表 [{}] 属于 shejiao 业务表且不在白名单，跳过租户注入", tableName);
                return true;
            }
            
            // ============ RuoYi 系统表排除（master 数据源）============
            // 不需要过滤租户的表（从配置文件读取）
            List<String> excludes = tenantProperties.getExcludes();
            // 非业务表（代码生成器相关）
            List<String> tables = ListUtil.toList(
                "gen_table",
                "gen_table_column"
            );
            tables.addAll(excludes);
            boolean shouldIgnore = StringUtils.equalsAnyIgnoreCase(tableName, tables.toArray(new String[0]));
            if (shouldIgnore) {
                log.debug("⏭️  [租户拦截] 表 [{}] 在 master 数据源排除列表中，跳过租户注入", tableName);
            } else {
                log.debug("✅ [租户拦截] 表 [{}] 在 master 数据源，注入租户ID: {}", tableName, tenantId);
            }
            return shouldIgnore;
        }
        log.debug("⏭️  [租户拦截] 无租户ID，跳过所有表的租户注入");
        return true;
    }

}
