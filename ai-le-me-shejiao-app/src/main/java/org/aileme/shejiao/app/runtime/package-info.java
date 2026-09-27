/**
 * 运行时配置与第三方能力装配边界。
 *
 * <p>约束：
 * 1. 平台业务配置读取走 sys_config。
 * 2. 三方渠道配置读取走 sys_third_party_provider / sys_third_party_route_rule。
 * 3. 新增运行时配置实现优先放在本包下，避免散落到通用 service/impl。
 */
package org.aileme.shejiao.app.runtime;
