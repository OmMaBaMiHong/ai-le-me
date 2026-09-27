# 智能恋爱助手治理与执行架构设计

## 1. 目标

本方案面向 AiLeMe 的 `智能恋爱助手`，目标不是做一个无限权限的黑盒代理，而是做一个：

- 高智能：会理解关系阶段、会做多步规划、会利用画像/记忆/图谱
- 可控代执行：在用户明确授权下，可以代聊、送礼、生成动态、协调约会
- 可审计：每次读取、建议、审批、执行、失败都可追踪
- 可限权：不同能力、不同对象、不同预算、不同时间窗口分别授权

本设计补的是 `权限 -> 计划 -> 审批 -> 执行 -> 审计` 这一层治理底座。

## 2. 现有仓库基础

仓库已经具备较完整的 Agent 基础，不需要另起新系统：

- App 侧统一入口已经存在：
  - `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/controller/AppAgentController.java`
- Java 编排层已经存在：
  - `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentOrchestratorService.java`
- Python runtime 桥接已经存在：
  - `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentRuntimeBridgeService.java`
- Python runtime 已有运行记录、记忆、动作计划、trace 表：
  - `ai-le-me-agent-runtime/sql/001_agent_runtime_tables.sql`
- 礼物执行链已经存在：
  - `gift_task_init.sql`
  - `org.aileme.shejiao.domain.entity.app.GiftTaskEntity`

因此本次设计不重复造轮子，继续坚持：

- `Java` 负责业务真执行、权限、账务、风控、审计
- `Python runtime` 负责画像、记忆、关系判断、多步工作流、结构化计划

## 3. 产品边界

### 3.1 可以做什么

在用户开启恋爱助手并逐项授权后，可以支持：

- 基于聊天上下文自动生成回复
- 在低风险条件下代发消息
- 基于预算和关系阶段自动送小额礼物
- 基于用户手动加入的素材生成动态草稿
- 推荐关注对象、推荐私信对象和私信开场
- 给出约会时间建议并协助协调

### 3.2 不应该直接做什么

以下动作不应因为“总开关 + 协议”就默认无限放开：

- 默认扫描整部手机相册和文件
- 大额礼物自动执行
- 自动确认线下见面
- 自动交换微信/手机号等外部联系方式
- 对不在授权范围内的对象发消息
- 在夜间或敏感时间段持续代聊

## 4. 核心原则

### 4.1 细粒度授权优先于总开关

用户开启总开关后，还需要分别确认各能力：

- `photo.read.selected`
- `message.draft`
- `message.auto.send`
- `gift.plan`
- `gift.execute`
- `moment.draft`
- `moment.publish`
- `date.coordinate`
- `follow.recommend`

### 4.2 用户选择素材，不做全盘扫描

“读照片”应落成 `助手创作篮子` 或 `用户手动选择后上传` 模式：

- 默认只读取本次明确选择的图片
- 不默认读取整部手机相册
- 不默认读取聊天附件和系统文件
- 素材授权要带场景：只用于动态、只用于画像、只用于视频等

### 4.3 高风险动作必须经过审批门

审批门不是只针对总开关，而是针对动作级别：

- 大额礼物
- 首次代发消息
- 非白名单对象代发
- 夜间消息代发
- 线下约会确认
- 直接发布动态

### 4.4 Java 持有执行权

所有真实动作继续由 Java 落库和执行：

- 发消息
- 发动态
- 送礼扣费
- 创建约会建议
- 推送通知
- 写审计日志

Python runtime 只输出结构化计划，不直接拥有业务写权限。

## 5. 风险分级

| 风险级别 | 含义 | 典型动作 | 默认策略 |
| --- | --- | --- | --- |
| `low` | 基本无资金和线下风险 | 回复建议、文案草稿、关注推荐 | 可自动生成，不自动外发 |
| `medium` | 会对关系推进产生影响 | 低频代发消息、小额礼物建议、动态草稿创建 | 需能力授权，可条件自动执行 |
| `high` | 涉及消费、身份表达、线下推进 | 大额送礼、替用户确认约会、对陌生人连续代聊 | 必须审批 |
| `critical` | 涉及隐私/安全/越界 | 扫描全部照片、读取系统文件、交换外部联系方式 | 默认禁止 |

## 6. 权限模型

### 6.1 权限记录

新增 `agent_permission` 记录用户对单个能力的授权策略：

- 是否启用
- 授权模式
- 适用对象范围
- 单次金额上限
- 每日金额上限
- 每日动作次数上限
- 静默时段
- 额外策略 JSON
- 用户确认协议版本

### 6.2 授权模式

- `manual_only`
  - 助手只生成建议，不自动执行
- `auto_draft`
  - 助手自动生成草稿，用户点发送
- `conditional_auto`
  - 满足预算、对象、时间窗条件时允许自动执行

### 6.3 对象范围

- `none`
- `whitelist`
- `friends`
- `matched_only`
- `custom`

## 7. 数据模型

### 7.1 继续复用的现有表

- `gift_task`
  - AI 礼物生成任务
- `pay_product`
  - 礼物商品
- `pay_order`
  - 礼物订单
- `pay_order_detail`
  - 礼物事件流水
- `account`
  - 爱情币账户
- `account_bill`
  - 爱情币账单
- `agent_action_plan`
  - Python runtime 输出的动作计划
- `agent_runtime_trace`
  - runtime 调试 trace

### 7.2 本次新增的治理表

#### `agent_permission`

用户对单项能力的授权与限制。

#### `agent_action_task`

Java 侧待执行/已执行动作任务，是 runtime 计划和业务执行的桥梁。

#### `agent_action_approval`

高风险动作审批单，记录审批请求、审批结果、过期时间。

#### `agent_action_log`

不可变审计日志，记录从权限校验到实际执行结果的全过程。

## 8. 端到端流程

### 8.1 代聊消息

1. 用户开启 `message.auto.send`
2. App 发送当前会话上下文到 Java
3. Java 校验当前用户权限和对象范围
4. Java 请求 runtime 产出回复计划
5. Java 根据权限和风险级别决定：
   - 仅返回建议
   - 自动创建草稿
   - 创建待审批动作
   - 直接执行发送
6. 全流程写 `agent_action_task` 与 `agent_action_log`

### 8.2 自动送礼

1. 用户开启 `gift.execute`
2. Java 拉取预算和白名单策略
3. runtime 输出礼物计划
4. Java 判断是否超出单次/日预算
5. 若超出预算或关系阶段过早，则生成审批单
6. 审批通过后复用既有礼物链：
   - `gift_task`
   - `pay_order`
   - `account_bill`

### 8.3 读照片生成动态

1. 用户从 App 选择图片加入助手创作篮子
2. App 上传到 OSS 并把素材引用传给 Java
3. Java 只将本次已授权素材传给 runtime
4. runtime 产出动态草稿
5. Java 创建 `agent_action_task`
6. 默认停留在草稿态；只有开启 `moment.publish` 且风险满足条件时才允许直发

### 8.4 约会协调

1. runtime 基于聊天、关系阶段、双方时间偏好生成候选时间
2. Java 生成建议卡片与消息草稿
3. 默认只发送“时间建议”，不直接确认线下见面
4. 真正确认见面必须走审批

## 9. 服务边界

### 9.1 App / uni-app

负责：

- 助手总开关
- 单项能力授权页
- 预算和对象范围配置
- 素材选择与上传
- 审批弹窗
- 最近执行记录展示

### 9.2 Java

负责：

- 权限与策略判断
- 审批单生成
- 动作任务创建与调度
- 真实执行消息/礼物/动态/邀约
- 审计落库

### 9.3 Python runtime

负责：

- 结构化策略输出
- 关系阶段判断
- 记忆检索
- 多步工作流
- 解释为什么推荐该动作

## 10. 建议的 API 演进

在保留现有 `/app/agent/*` 接口的基础上，新增治理相关接口：

- `GET /app/agent/permissions`
- `POST /app/agent/permissions/save`
- `GET /app/agent/actions`
- `POST /app/agent/actions/{taskId}/approve`
- `POST /app/agent/actions/{taskId}/reject`
- `GET /app/agent/actions/{taskId}`

现有接口保持不变，但它们的执行逻辑由“直接建议”升级为“先过权限/审批门再决定执行方式”。

## 11. 推荐的第一阶段落地顺序

### 阶段 A：治理底座

- 新增治理表
- 新增 Java 实体和 DAO
- 新增权限常量
- 补设计文档和执行计划

### 阶段 B：消息自动化

- `message.auto.send` 权限
- 低风险条件自动发送
- 首次代聊必须审批

### 阶段 C：礼物自动化

- `gift.execute` 权限
- 预算守卫
- 审批通过后复用现有送礼和账务链

### 阶段 D：内容自动化

- 助手创作篮子
- 动态草稿生成
- 审批后发布

## 12. 结论

AiLeMe 的智能恋爱助手应该做成：

- `会规划`
- `会记忆`
- `会调用受控技能`
- `会在用户明确授权范围内代执行`
- `但不会因为一个总开关就变成无限权限代理`

这与仓库当前方向一致：

- `Java 管业务执行`
- `Python runtime 管智能`
- `OpenClaw 类项目只借鉴技能和通道抽象，不直接照搬执行核心`
