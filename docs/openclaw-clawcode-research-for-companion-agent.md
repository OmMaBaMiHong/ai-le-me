# OpenClaw / Claw-Code 对 AiLeMe 智能恋爱助手的启发

## 1. 研究目标

本研究只关注两件事：

1. `OpenClaw` 和 `claw-code` 各自擅长什么能力模型
2. 这些能力里，哪些适合迁移到 `AiLeMe 智能恋爱助手`

注意：

- `OpenClaw` 是公开开源项目，可直接研究其架构和公开文档
- 本地 `claw-code` 仓库的 README 明确强调其当前目标是 clean-room rewrite / harness engineering 研究，不应把它当作“可以直接照搬的产品源码”
- 对 AiLeMe 而言，重点不是复刻别人的工程壳，而是抽取 `多步规划 + 工具治理 + 授权审批 + 长期记忆 + 多通道执行` 这些模式

## 2. 结论先行

一句话总结：

- `claw-code` 更像 `高能力代理壳层`
- `OpenClaw` 更像 `现实世界个人助理平台`
- `AiLeMe 智能恋爱助手` 应该借 `claw-code` 的工作流和工具编排，借 `OpenClaw` 的多渠道、授权、安全、自动化、代理身份模型

对 AiLeMe 最有价值的不是“会写代码”，而是：

- 受控技能系统
- 授权和审批门
- 多会话/多对象隔离
- 长期记忆与 standing orders
- 定时/事件驱动执行
- 助手作为“代理身份”而不是“伪装成用户本人”

## 3. 本地仓库观察

### 3.1 `claw-code`

本地路径：

- `/Users/wade/work-space/claw-code`

从 README 和 parity 文档看，它强调的是：

- agent harness
- 工具注册与执行
- slash commands
- hooks
- skills
- plugins
- MCP
- subagent / team / task orchestration

关键证据：

- README：
  - `/Users/wade/work-space/claw-code/README.md`
- parity gap 分析：
  - `/Users/wade/work-space/claw-code/PARITY.md`
- 命令/工具快照：
  - `/Users/wade/work-space/claw-code/src/reference_data/commands_snapshot.json`
  - `/Users/wade/work-space/claw-code/src/reference_data/tools_snapshot.json`

从 `tools_snapshot.json` 可见的能力面包括：

- `AgentTool`
- `AskUserQuestionTool`
- `BashTool`
- `ConfigTool`
- `FileReadTool`
- `FileWriteTool`
- `GlobTool`
- `GrepTool`
- `LSPTool`
- `MCPTool`
- `McpAuthTool`
- `RemoteTriggerTool`
- `CronCreateTool`
- `SendMessageTool`
- `SkillTool`
- `TaskCreateTool`
- `TaskListTool`
- `TaskStopTool`
- `TeamCreateTool`
- `TodoWriteTool`
- `WebFetchTool`
- `WebSearchTool`

这说明它最值得借鉴的是：

- 工具不是“随便调用函数”，而是有统一名字、统一 schema、统一执行边界
- 命令、技能、MCP、远程触发、任务、团队代理被放在一个统一的 agent harness 里
- subagent 和 task 是一等公民，不是外挂

### 3.2 `OpenClaw`

本地路径：

- `/Users/wade/work-space/openclaw`

它更像一个完整的 `现实世界 assistant 平台`，核心不是 IDE 代理，而是：

- Gateway 控制平面
- 多渠道收发
- 多 agent 路由
- 设备节点
- cron / webhook / standing orders
- pairing / approvals / security
- 技能注册与安装

关键本地文档：

- README：
  - `/Users/wade/work-space/openclaw/README.md`
- 架构：
  - `/Users/wade/work-space/openclaw/docs/concepts/architecture.md`
- agent loop：
  - `/Users/wade/work-space/openclaw/docs/concepts/agent-loop.md`
- skills：
  - `/Users/wade/work-space/openclaw/docs/tools/skills.md`
- pairing：
  - `/Users/wade/work-space/openclaw/docs/channels/pairing.md`
- subagents：
  - `/Users/wade/work-space/openclaw/docs/tools/subagents.md`
- standing orders：
  - `/Users/wade/work-space/openclaw/docs/automation/standing-orders.md`
- cron：
  - `/Users/wade/work-space/openclaw/docs/automation/cron-jobs.md`
- delegate architecture：
  - `/Users/wade/work-space/openclaw/docs/concepts/delegate-architecture.md`

关键源码线索：

- pairing：
  - `/Users/wade/work-space/openclaw/src/pairing/`
- 执行审批：
  - `/Users/wade/work-space/openclaw/src/infra/exec-approvals.ts`
- 渠道路由与投递：
  - `/Users/wade/work-space/openclaw/src/infra/outbound/`
- 安全：
  - `/Users/wade/work-space/openclaw/src/security/`

## 4. `claw-code` 对 AiLeMe 的启发

### 4.1 工具编排层比“一个超强 prompt”更重要

`claw-code` 给人的最大启发不是模型，而是：

- 命令系统
- 工具系统
- 任务系统
- 子代理系统
- hooks / plugins / skills / MCP 之间的统一编排

对 AiLeMe 来说，智能恋爱助手不应该只是一个：

- `reply(prompt, context) -> text`

而应该是一个：

- 会选择能力
- 会拆解步骤
- 会创建任务
- 会等待回执
- 会根据结果继续下一步

也就是：

- `感知 -> 推理 -> 生成计划 -> 选择工具 -> 执行 -> 记录 -> 继续`

### 4.2 会话和任务要是一等公民

从 `Task*`、`Team*`、`spawnMultiAgent`、`forkSubagent` 这些能力可见，复杂代理系统不能只靠一次请求完成。

对 AiLeMe 的映射：

- 一个“代聊推进关系”的动作，不是单条消息，而是一串任务
- 一个“送礼并推进聊天”的动作，也不是一次调用，而是：
  - 判断关系阶段
  - 挑礼物
  - 判断预算
  - 判断是否审批
  - 生成送礼文案
  - 执行账务
  - 投递消息
  - 观察反馈

因此需要：

- `agent_action_task`
- `agent_action_log`
- `agent_action_approval`
- 后续可加 `agent_workflow_run`

### 4.3 Hooks 非常适合恋爱场景

`claw-code`/Claude Code 体系里，hooks 的价值在于：

- 工具前拦截
- 工具后审计
- 对危险行为做阻断或改写

对 AiLeMe 的映射非常直接：

- 发送消息前：检查是否在静默时段、是否超频、是否对白名单对象
- 送礼前：检查预算、关系阶段、风控等级
- 发布动态前：检查是否使用了未授权照片
- 约会确认前：检查是否必须审批

所以 AiLeMe 后面要补的不是“更多 prompt”，而是 `action hooks / policy hooks`。

## 5. `OpenClaw` 对 AiLeMe 的启发

### 5.1 Gateway 思维非常重要

OpenClaw 的架构核心是一个长生命周期 Gateway：

- 所有渠道连接都归它
- 所有控制客户端都连它
- 所有节点都连它
- 所有定时任务、事件流、agent stream 都由它统一编排

对 AiLeMe 的启发：

- 你们不一定要做一个 WS Gateway 产品形态
- 但一定要做一个 `恋爱助手控制平面`

这个控制平面在 AiLeMe 里可以落为：

- Java 业务执行层
- Python runtime 智能层
- 审批/任务/日志/权限的统一治理层

不要把这些能力散落在：

- 聊天 controller
- 礼物 service
- 动态 service
- 红娘 service

正确方向是：

- 所有恋爱助手动作统一进一个 orchestration plane

### 5.2 Pairing 模式特别适合 AiLeMe 的对象授权

OpenClaw 的 pairing 是：

- 未知 DM 不直接处理
- 先配对、后放行

对 AiLeMe 的直接启发是：

- 恋爱助手不应该默认对所有对象代聊
- 应该有 `对象级授权`

可以引入：

- `message.auto.send` 只对白名单对象生效
- 首次对某个对象代聊，要求用户确认
- 若对方并非好友/未互相关注/非匹配对象，强制审批

也就是说，不只是“能力授权”，还要有：

- `对象 pairing`
- `关系阶段 pairing`

### 5.3 Delegate Architecture 非常适合你们

OpenClaw 的 `delegate architecture` 最值得学的一点是：

- agent 有自己的身份
- agent 代表人做事，但不伪装成这个人
- 动作权限由 standing orders 和 policy 限制

这对 AiLeMe 非常关键。

因为你们的“智能恋爱助手”如果直接完全伪装成用户本人，风险很高：

- 法务风险
- 伦理风险
- 关系误伤风险
- 用户对“谁在说话”的感知混乱

更好的做法是分层：

- `建议模式`
  - 明确是助手建议
- `代执行模式`
  - 由用户授权，按用户设定语气代发
- `代理身份模式`
  - 某些场景明确标记“由恋爱助手代拟/代发”

在产品文案和协议上，要明确三种模式差别。

### 5.4 Standing Orders 很适合“恋爱推进程序”

OpenClaw 的 standing orders 不是一次性 prompt，而是：

- 稳定的授权程序
- 有边界的自动化规则
- 有升级和审批条件

这对 AiLeMe 的适配度很高。

可以把“恋爱助手”拆成多个 program：

- `Program: 回复与续聊`
- `Program: 送礼与情绪维护`
- `Program: 动态经营`
- `Program: 约会推进`
- `Program: 风险监测`

每个 program 都要定义：

- authority
- trigger
- approval gate
- escalation rules

例如：

- `回复与续聊`
  - 低风险自动草稿
  - 夜间不自动发
  - 同一对象 30 分钟最多 1 条
- `送礼与情绪维护`
  - 单次不超过 19.9
  - 日累计不超过 39.9
  - 关系阶段早期禁止高价值礼物
- `约会推进`
  - 只能推荐时间
  - 不能直接确认见面

### 5.5 Cron / Webhook / Event 触发很适合恋爱助手

OpenClaw 的 cron / webhook / background task 对 AiLeMe 也很有价值。

可以落的触发类型：

- 定时：
  - 每晚 9 点检查今天重要关系对象是否需要回访
  - 每周生成关系推进总结
- 事件：
  - 对方刚发消息 -> 触发回复建议
  - 对方动态更新 -> 触发互动建议
  - 礼物任务完成 -> 触发跟进建议
- 条件：
  - 48 小时无互动 -> 触发低压破冰建议

但这里一定不能做成无约束自动化，必须走你们自己的权限和审批体系。

### 5.6 Typed Tools 比 Shell 技能更适合婚恋产品

OpenClaw 的一个很重要方向是：

- 优先 typed tools
- 尽量减少 agent 直接 shell

这对 AiLeMe 很关键。

不要让“恋爱助手”默认拥有：

- 任意执行 shell
- 任意读文件
- 任意访问手机全量数据

而要把能力做成 typed tools：

- `chat.send_message`
- `chat.draft_reply`
- `gift.plan`
- `gift.execute`
- `moment.create_draft`
- `moment.publish`
- `photo.read_selected`
- `date.suggest_slots`
- `follow.recommend`

这样：

- 更容易做风控
- 更容易做审计
- 更容易做 A/B 和限权

## 6. 对 AiLeMe 最值得直接落地的 8 个点

### 6.1 技能注册表

不是自由 prompt，而是注册后的能力：

- 能力编码
- 输入 schema
- 风险级别
- 是否需要审批
- 是否需要预算
- 执行 owner

### 6.2 多阶段工作流

把复杂动作拆成 graph/workflow：

- 感知
- 记忆召回
- 关系阶段判断
- 候选动作生成
- 风险打分
- 执行或审批
- 跟踪回执

### 6.3 对象级授权

不是用户只开一个总开关，而是：

- 哪些对象可代聊
- 哪些对象可自动回复
- 哪些对象可送礼

### 6.4 预算与频控

对礼物、消息、动态都要有额度和频次控制。

### 6.5 审批中心

审批不是弹窗散落各页面，而是统一审批中心：

- 待审批任务
- 原因
- 风险等级
- 建议动作
- 一键批准 / 拒绝 / 永久放行同类低风险动作

### 6.6 助手身份与语气模板

要区分：

- 用户本人语气
- 助手代拟语气
- 助手代理身份输出

### 6.7 背景任务与回访

要有：

- 定时回访
- 长时间未互动提醒
- 阶段推进建议
- 礼物送出后的 follow-up

### 6.8 全链路审计

至少记录：

- 看了哪些照片
- 基于哪些上下文做了建议
- 是否自动执行
- 谁审批了
- 发送了什么
- 花了多少钱
- 执行是否成功

## 7. 不建议照搬的部分

### 7.1 不要照搬 `coding agent` 工具面

`claw-code` 的很多工具是为编程准备的：

- 文件编辑
- LSP
- shell
- plugin lifecycle

这些不适合直接成为恋爱助手的一等能力。

### 7.2 不要默认把手机当成开放沙箱

OpenClaw 能调设备节点、照片、位置、通知等，这对个人助理合理，但对婚恋产品非常敏感。

AiLeMe 必须坚持：

- 默认最小权限
- 只读用户本次选择的素材
- 设备敏感能力严格分级

### 7.3 不要把“自动化”当“无限自动”

OpenClaw 的 standing orders / cron 很强，但你们恋爱场景里：

- 自动发消息
- 自动送礼
- 自动约会

都比一般待办自动化更敏感，必须保守。

## 8. 对 AiLeMe 的推荐架构

### 8.1 总体原则

- `Java` 管业务执行
- `Python runtime` 管智能推理和工作流
- `治理层` 管权限、审批、任务、审计

### 8.2 推荐新增抽象

- `AgentCapabilityRegistry`
- `AgentPolicyEngine`
- `AgentApprovalService`
- `AgentTaskService`
- `AgentAuditService`
- `RelationshipProgramEngine`

### 8.3 推荐的 program 划分

- `reply_program`
- `follow_program`
- `gift_program`
- `moment_program`
- `date_program`
- `risk_watch_program`

### 8.4 推荐的 typed tools

- `companion.reply.suggest`
- `companion.reply.send`
- `companion.gift.plan`
- `companion.gift.execute`
- `companion.moment.draft`
- `companion.moment.publish`
- `companion.photo.read_selected`
- `companion.date.suggest`
- `companion.follow.recommend`

## 9. 对当前 AiLeMe 的直接建议

结合当前仓库现状，最适合的下一步不是去追求“全自动恋爱代理”，而是按下面顺序落：

### 第一阶段

- 权限模型
- 审批模型
- 任务模型
- 审计模型

### 第二阶段

- `代聊` 接入治理门
- `送礼` 接入治理门
- `动态草稿` 接入治理门

### 第三阶段

- standing orders / relationship programs
- 定时回访与事件触发
- 对象级 pairing

### 第四阶段

- 多 agent 分工
  - 主恋爱助手
  - 风险监督 agent
  - 内容经营 agent
  - 红娘协同 agent

## 10. 最终判断

如果只问一句：

> 我们最该学谁？

答案是：

- 产品和安全架构上，优先学 `OpenClaw`
- 工作流、工具编排、子代理和 hooks 思维上，吸收 `claw-code / Claude Code` 的方法

如果只问一句：

> 最不该学什么？

答案是：

- 不要做成一个拿到总开关后就能“无限代执行”的黑盒代理

对 AiLeMe 来说，真正高级的不是“它能不能替用户做任何事”，而是：

- 它能否在正确边界内，持续、审慎、稳定地推进关系
