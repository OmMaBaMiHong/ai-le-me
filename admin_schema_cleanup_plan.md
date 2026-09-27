# Admin/App 单体单库清理结论

更新时间：2026-03-21

## 1. 当前真实情况

- 本地开发是双库：
  - `xg_admin`：系统后台库
  - `bang_yi`：社交业务库
- 代码层也是双数据源：
  - `master -> xg_admin`
  - `app -> bang_yi`
- 但大量后台业务管理代码已经直接走 `@DS("app")`，说明真正的业务核心库其实是 `bang_yi`。

## 2. 建议保留的表

- `bang_yi.recharge`
  不是订单表，而是充值档位/商品定义表。
- `bang_yi.user_recharge`
  真实充值订单流水表。
- `bang_yi.sign_config`
  当前虽然 0 行，但代码还在使用，先不删。
- `bang_yi.tenant_config`
  当前虽然 0 行，但代码还在使用，先不删，后续建议并入 `sys_config`。
- `xg_admin.sj_*`
  SnailJob 相关表，当前定时任务模块还在使用，不能动。

## 3. 可以直接删的表

### xg_admin

- `flow_*`
  原因：当前项目没有 workflow 源码模块，`warm-flow` 只剩配置残留。
- `gen_table`
- `gen_table_column`
  原因：代码生成器表，项目里没有实际在线生成使用链路。
- `test_demo`
- `test_leave`
- `test_tree`
  原因：典型示例/演示表。
- `hongniang_info`
- `hongniang_user_relation`
  原因：这两张在 `xg_admin` 中是空表，真实业务已经走 `bang_yi`。

### bang_yi

- `post_tag`
  原因：代码真实使用的是 `post_tags`，`post_tag` 当前 0 行且无实体绑定。

## 4. 字段/表设计明显不合理点

### `sys_third_party_provider`

问题：
- 还是典型 key-value 设计
- 一个 provider 拆成很多行，回表多，管理也容易乱

结论：
- 只保留迁移期兼容作用
- 最终应收敛到 `sys_third_party_provider.config_json`

### `sys_third_party_provider`

问题：
- `bang_yi` 旧表缺 `config_json`
- 审计字段类型和 `xg_admin` 不一致

结论：
- 应以 `xg_admin` 当前结构为准，整体迁入 `bang_yi`

### `tenant_config`

问题：
- 本质也是弱语义 KV
- 当前 0 行，说明业务并未真正跑起来

结论：
- 后续应并回 `sys_config` 或者做成明确的租户业务配置表，不建议长期单独保留

### `sign_config`

问题：
- 结构简单，但命名偏弱
- 当前无数据

结论：
- 短期保留
- 后续如只做签到规则，可改成更明确的业务配置模型

## 5. 这次代码已配合做的收口

- 增加统一业务配置入口：
  - `PlatformBusinessConfigService`
- 增加统一第三方运行时配置入口：
  - `ThirdPartyRuntimeConfigService`
- 微信公众号、Agent runtime、实名认证、学历认证已经开始走统一门面
- `application-dev.yml` / `application-prod.yml` 已支持通过环境变量切换为单库
- workflow 文档入口和默认启用状态已关闭

## 6. 推荐迁移顺序

1. 执行 [admin_to_app_schema_merge.sql](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/admin_to_app_schema_merge.sql)
2. 将 `YUELAO_MASTER_DB` 和 `YUELAO_APP_DB` 都指向 `bang_yi`
3. 重启后端
4. 验证登录、后台菜单、上传、第三方配置读取
5. 再继续把支付、短信、微信配置迁到统一三方配置
