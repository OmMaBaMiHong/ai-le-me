package org.aileme.shejiao.api.service;

/**
 * 平台业务配置统一读取入口。
 *
 * 约束：
 * 1. 这里只读取 sys_config 一类“运营/业务规则”配置
 * 2. 不再让业务代码直接散落依赖底层 system 配置服务
 */
public interface PlatformBusinessConfigService {

    String getString(String key);

    String getString(String key, String defaultValue);

    Integer getInt(String key, Integer defaultValue);

    Long getLong(String key, Long defaultValue);

    Boolean getBoolean(String key, Boolean defaultValue);
}
