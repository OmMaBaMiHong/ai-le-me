# 配置收敛清单

更新时间：2026-03-21

## 1. 三层配置目标

### A. 部署层配置
保留在 `application*.yml` / 环境变量。

适合放这里的内容：
- 数据源
- Redis
- 监控 / job / mail
- `justauth`
- JWT / 安全基础参数
- 本地开发地址、端口、线程池

原则：
- 这层是“应用启动就要有”的配置
- 不走业务后台热更新

### B. 系统业务配置
保留在 `sys_config`。

适合放这里的内容：
- 积分比例
- VIP 限制
- 审核开关
- 业务默认值
- 产品策略开关

原则：
- 这层是“平台运营规则”
- 不放渠道密钥和模型 endpoint

### C. 三方渠道运行配置
统一到：
- `sys_third_party_provider`
- `sys_third_party_route_rule`

适合放这里的内容：
- API Key / Secret
- endpoint / bucket / domain / region
- model / profile / timeout
- provider 启用状态
- provider 路由规则

原则：
- 这层是“外部能力接入”
- 每个 provider 一条主记录，配置聚合进 `config_json`

## 2. 当前已经执行的收敛

- OSS 运行时初始化已改为从 `sys_third_party_provider(service_type=oss)` 读取
- `cloud.*` 相关代码已删除
- `sys_oss_config` 不再是 OSS 运行时主数据源

涉及代码：
- [SystemApplicationRunner.java](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-system/src/main/java/org/dromara/system/runner/SystemApplicationRunner.java)
- [ISysThirdPartyService.java](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-system/src/main/java/org/dromara/system/service/ISysThirdPartyService.java)
- [SysThirdPartyServiceImpl.java](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-system/src/main/java/org/dromara/system/service/impl/SysThirdPartyServiceImpl.java)
- [oss_third_party_config_migration.sql](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/oss_third_party_config_migration.sql)

## 3. 下一批建议迁移

### 优先级 P0
- 微信支付配置：从 `sys_config` 迁到 `payment` provider `config_json`
- 支付宝配置：从 `sys_config` 迁到 `payment` provider `config_json`
- 微信公众号 / 小程序 AppId Secret：明确一套主来源，不再 `shejiao.yml + sys_config` 双读

### 优先级 P1
- 短信配置：统一到 `sys_third_party_provider(service_type=sms)`，删除阿里老 `SmsUtils` 风格
- 实名配置：统一到 `realname` provider `config_json`
- 学历认证配置：统一到 `education` provider `config_json`

### 优先级 P2
- 已收敛到 `sys_third_party_provider.config_json` + `sys_third_party_route_rule`
- 后台三方配置页面改成编辑 provider 级 JSON/表单，而不是一行一 key

## 4. 可废弃项

### 代码
- `org.aileme.common.cloud.*`

### 表
- `sys_oss_config`
  条件：OSS 后台管理入口也切到三方配置后可删

### 过渡表
- `sys_third_party_provider.config_json`
  条件：所有运行时都改成只读 `config_json` 后可下线

## 5. `sys_config` 建议保留的范围

保留：
- `integral`
- `vipTopicNumber`
- `commonTopicNumber`
- `rewardCommissionRate`
- `commentCheck`
- `circleCheck`
- `pay.mock.enabled`
- 各类业务展示开关 / 审核开关 / 文案协议

迁出：
- `pay.alipay.*`
- `wxPayKey`
- `wxPaySecret`
- `WxAppId`
- `wxAppSecret`
- `WxMpAppId`
- `WxMpSecret`
- `realname.*`
- `education.*`
- 旧 `oss.*`
- 旧 `sms.*`

## 6. `admin` 单体后端层级结构优化建议

不做拆服务、不做架构升级，只从单体角度优化。

### 当前主要问题
- 模块维度和层级维度混在一起
  `ai-le-me-system`、`ai-le-me-shejiao-app`、`ai-le-me-common` 是一套维度；`org.aileme.shejiao.admin/app/api/common` 又是一套维度
- 公共层过厚
  `ai-le-me-common` 和 `org.aileme.shejiao.common` 同时存在，职责边界不够清晰
- 配置入口分散
  `application*.yml`、`shejiao.yml`、`sys_config`、`sys_third_party_*`、旧表并存
- 基础设施和业务适配器混放
  例如微信、支付、OSS、AI 的调用代码散在 `utils`、`service`、`strategy` 里

### 单体内更顺的做法
- 保留现有模块，但每个模块内部按“领域 + adapter”组织
- `org.aileme.shejiao.common` 里只保留业务公共常量/枚举/异常
- 外部渠道适配代码统一收拢到 `integration` 或 `infra`
- `utils` 只保留纯函数工具，禁止继续堆 API 调用器
- 配置读取统一走两套入口
  一套 `SystemBusinessConfigService`
  一套 `ThirdPartyRuntimeConfigService`

### 推荐整理顺序
1. 先收敛配置入口
2. 再把微信/支付/短信/OSS/AI 的调用类从 `utils` 挪成 `integration`
3. 最后再裁剪重复公共层

## 7. 执行顺序建议

1. 执行 [oss_third_party_config_migration.sql](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/oss_third_party_config_migration.sql)
2. 重启服务并验证上传
3. 迁支付
4. 迁短信
5. 迁实名 / 学历
6. 改后台三方配置页面
7. 已下线旧的第三方逐项配置表，后续只维护 provider JSON 与 route rule
