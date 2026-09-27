# colleague-skill / ex-skill 对 AiLeMe Agent 的产品化启发

## 1. 目标

本文不讨论这两个开源项目是否可以直接接入 AiLeMe。

本文只回答 3 个问题：

1. `colleague-skill` 和 `ex-skill` 的核心产品思想是什么
2. 这些思想对 `智能画像`、`智能红娘`、`智能恋爱助手` 分别有什么启发
3. AiLeMe 应该如何吸收这些思想，而不是被它们带偏成“纯聊天模拟器”

结论先行：

- `colleague-skill` 最值得借鉴的是 `把一个人拆成可维护的多层 Persona + Work Skill`
- `ex-skill` 最值得借鉴的是 `把关系记忆从人物性格里独立出来`
- AiLeMe 不应该复刻“某个人的 AI 分身”，而应该把这些方法沉淀成：
  - `智能画像` 的结构化蒸馏框架
  - `关系记忆` 的对象级长期记忆框架
  - `恋爱助手` 的风格控制和纠偏框架

## 2. 两个项目真正强在哪里

### 2.1 `colleague-skill`

本地路径：

- `/Users/wade/work-space/colleague-skill`

它不是一个“陪聊 Agent”，而是一个 `meta-skill generator`。

它把“一个同事”拆成两个独立部分：

- `Part A - Work Skill`
- `Part B - Persona`

关键点不在“像这个人说话”，而在：

- 能把用户主观标签翻译成 `可执行行为规则`
- 能把技术规范、CR 偏好、职责边界单独沉淀成 `工作能力层`
- 能把人格层和工作层解耦，运行时再组合
- 支持 `追加原材料 -> merge -> correction -> versioning`

关键参考：

- `/Users/wade/work-space/colleague-skill/prompts/persona_analyzer.md`
- `/Users/wade/work-space/colleague-skill/prompts/persona_builder.md`
- `/Users/wade/work-space/colleague-skill/prompts/work_analyzer.md`
- `/Users/wade/work-space/colleague-skill/prompts/work_builder.md`
- `/Users/wade/work-space/colleague-skill/prompts/merger.md`

### 2.2 `ex-skill`

本地路径：

- `/Users/wade/work-space/ex-skill`

它比 `colleague-skill` 多迈出的一步是：

- 不只抽“这个人是什么样”
- 还抽“你和这个人之间发生过什么”

所以它把结构变成：

- `Part A - Relationship Memory`
- `Part B - Persona`

这个设计对恋爱场景特别重要，因为：

- 同一个人对不同关系对象的回应方式可能完全不同
- 关系里的共同经历、争吵模式、inside jokes、约会偏好，本质上不是“人格”，而是“关系事实”

关键参考：

- `/Users/wade/work-space/ex-skill/prompts/memory_analyzer.md`
- `/Users/wade/work-space/ex-skill/prompts/persona_analyzer.md`
- `/Users/wade/work-space/ex-skill/prompts/persona_builder.md`
- `/Users/wade/work-space/ex-skill/prompts/merger.md`

## 3. 对 AiLeMe 最重要的 5 个启发

### 3.1 画像必须分层，不能只是一份大报告

这两个项目最值得借鉴的地方，不是生成文案，而是 `人格蒸馏分层`。

AiLeMe 当前的 `智能画像` 如果只是：

- 性格总结
- 标签列表
- 优缺点概述

那只能给运营看，不能稳定驱动恋爱助手。

更合理的做法是把画像拆成：

1. `Layer 0 - 硬边界`
   - 绝对雷区
   - 明显反感的话题
   - 明显不能接受的推进方式
   - 拒绝时常见表现
2. `Layer 1 - 身份与稳定特征`
   - 年龄段、城市、职业、作息、生活方式
   - MBTI/星座只能作为辅助，不是主特征
   - 依恋类型、爱的语言、社交能量、表达习惯
3. `Layer 2 - 表达风格`
   - 语气词
   - 消息长度
   - emoji 频率
   - 主动性
   - 回复时段
4. `Layer 3 - 决策模式`
   - 喜欢慢热还是快推进
   - 对邀约敏感还是开放
   - 对微信交换谨慎还是自然
   - 更吃幽默、真诚、陪伴、仪式感还是成熟稳定
5. `Layer 4 - 关系行为`
   - 冲突模式
   - 冷战模式
   - 安慰偏好
   - 拉近关系的有效动作
6. `Correction Layer`
   - 用户纠偏
   - 模型后验修正
   - 重要事实冲突记录

这套分层一旦稳定下来，`智能画像` 就不再只是报告，而是 `推理底座`。

### 3.2 关系记忆必须从画像里独立出来

`ex-skill` 给 AiLeMe 最大的直接启发是：

- `关系记忆 != 人物画像`

对 AiLeMe 而言，关系记忆应该是 `owner-target` 维度的一等公民。

对每一个陪聊对象，都应该独立维护：

- 关系时间线
- 聊天节奏
- 互动频率
- 正向反馈信号
- 负向反馈信号
- 共同经历
- inside jokes
- 约会偏好
- 礼物接受偏好
- 微信交换前的阻力点

这正好和现有 runtime 表结构高度契合：

- `agent_memory_profile`
- `agent_memory_fact`

建议不是另起新表，而是扩展现有语义：

- `agent_memory_profile.summary`
  - 变成对象级关系摘要
- `preferences_json`
  - 不只存“喜欢什么”，还存“什么场景下更容易接受”
- `taboo_json`
  - 存储推进雷区和拒绝触发器
- `signals_json`
  - 存储阶段升降级证据

`agent_memory_fact.fact_type` 建议进一步标准化为：

- `preference`
- `taboo`
- `promise`
- `topic`
- `ritual`
- `date_preference`
- `gift_preference`
- `communication_pattern`
- `rejection_signal`
- `progress_signal`

### 3.3 恋爱助手的风格控制不能只靠 prompt

这两个项目都在做一件事：

- 让模型在“说什么”之前，先知道“这个人会怎么说”

对 AiLeMe 来说，这点应该转译为：

- 恋爱助手生成回复前，先选择 `relationship coach style`

这个风格不应是固定模板，而应是：

- 由 `智能画像` 推断对方接受哪种推进方式
- 由 `关系记忆` 判断当前阶段适合什么强度
- 由 `红娘推荐信号` 判断最初破冰点是什么

可以抽象成：

- `温柔安全感型`
- `轻松幽默型`
- `成熟靠谱型`
- `生活感陪伴型`
- `轻仪式感浪漫型`
- `节奏克制慢热型`

然后运行时做三段式组合：

1. `画像层` 决定适配风格范围
2. `关系层` 决定当前可用风格强度
3. `会话层` 决定当前这条消息的语气、温度、长度和目标

这样恋爱助手就不是：

- “统一恋爱话术库”

而是：

- “面向具体对象的动态风格路由器”

### 3.4 用户纠偏机制必须产品化

`colleague-skill` 和 `ex-skill` 都很强调 correction。

这个设计对 AiLeMe 极其重要，因为恋爱场景里最大的风险之一就是：

- 模型自以为懂对方

建议在 AiLeMe 增加 3 类纠偏入口：

1. `画像纠偏`
   - 她不是话痨
   - 他更慢热
   - 她不喜欢太油
2. `关系纠偏`
   - 我们没有去过这个地方
   - 她对花不感兴趣
   - 他之前明确拒绝过换微信
3. `话术纠偏`
   - 她不会这么说
   - 这个推进太快
   - 这个语气像客服，不像真实聊天

纠偏不应只改 prompt，而应进入结构化存储，优先写入：

- `agent_memory_fact`
- `agent_memory_profile`
- `agent_persona_report_v2.evidence_json`

必要时在 runtime 中单独加：

- `relationship_correction_json`

### 3.5 版本化记忆比“覆盖式更新”更适合恋爱场景

这两个项目都做了 `追加 -> merge -> 版本存档 -> 回滚`。

对 AiLeMe 也应该坚持：

- 关系判断永远可能变
- 风格选择永远可能被纠偏
- 画像不是“真相”，而是当前最优解释

所以建议把现有 `agent_persona_report_v2.version_no` 和 `agent_memory_profile.version_no` 真正用起来：

- 每次重大纠偏或阶段变更时递增版本
- 允许保留上一个版本摘要
- 对高风险动作，记录“基于哪个画像版本/记忆版本做出的判断”

这样后面出现“为什么助手判断错了”时，能追溯到：

- 当时吃的是哪份画像
- 哪份关系记忆
- 哪个 coach style

## 4. 不能照搬的地方

这两个项目很有启发，但 AiLeMe 不能直接照搬，原因有 4 个。

### 4.1 它们是蒸馏器，不是自治执行器

这两个项目的重点是：

- 生成 skill
- 运行人格

而不是：

- 事件监听
- 任务调度
- 风险治理
- 多对象并行自治

所以它们能补的是 `画像与关系建模层`，补不了 `自治执行层`。

### 4.2 它们没有商业化治理约束

AiLeMe 有：

- VIP 权益
- 爱情币计费
- 自动执行权限
- 礼物预算
- 风险审批
- 审计日志

这部分决定了 AiLeMe 必须继续坚持：

- `Java 管业务真执行`
- `Python runtime 管智能推理`

### 4.3 它们没有关系推进评分体系

AiLeMe 当前已经在走：

- `intimacy_score`
- `progression_score`
- `tone_warmth`
- `date_ready_score`
- `wechat_ready_score`

这类分数是恋爱助手的核心，不是这两个 skill 的重点。

所以正确做法是：

- 用这两个项目补 `分数背后的结构化特征`
- 不要用它们替代我们的关系评分引擎

### 4.4 它们默认是“单角色对话”，不是“多对象经营”

AiLeMe 的恋爱助手必须支持：

- 多对象并行经营
- 红娘推荐对象自动起聊
- 对方来消息自动回
- 若干轮后评估是否推进约会/微信

这意味着我们必须保留并强化当前的：

- `relationship program`
- `agent_action_task`
- `agent_action_plan`
- `agent_runtime_trace`

## 5. 对 AiLeMe 三大 Agent 的具体落点

### 5.1 对 `智能画像`

最应该吸收的是：

- 多层 Persona 拆分
- 标签翻译成行为规则
- 纠偏层
- 版本化

建议产出从“纯报告”升级成“双输出”：

1. `画像报告`
   - 给用户和运营看
2. `画像内核`
   - 给红娘和恋爱助手调用

`画像内核` 建议至少包含：

- stable_traits
- expression_style
- relationship_style
- attachment_style
- love_language
- romance_pace
- taboo_rules
- coach_style_candidates
- evidence_summary

### 5.2 对 `智能红娘`

最应该吸收的是：

- “人设不是标签，而是行为模式”的建模方式

红娘推荐不应该只看：

- 年龄、城市、学历、标签匹配

还要看：

- 回复节奏兼容度
- 推进节奏兼容度
- 表达风格兼容度
- 冲突模式兼容度
- 仪式感/生活感偏好是否兼容

也就是说，`智能红娘` 应该消费的是“画像内核”，不是只消费 profile 表字段。

### 5.3 对 `智能恋爱助手`

最应该吸收的是：

- 关系记忆单独建模
- 运行时先做人格/风格判断，再做内容生成
- 用户纠偏快速生效

运行时建议固定为：

1. 读取对象级画像内核
2. 读取对象级关系记忆
3. 读取最近聊天上下文
4. 计算关系状态分数
5. 选择 coach style
6. 生成当前动作建议
7. 返回结构化计划给 Java

## 6. 对现有 6 层架构的补充建议

如果沿着你们现在的 `Java 真执行 + Python 智能编排` 路线继续走，我建议在既有 6 层架构上补 2 层语义。

### 6.1 在“记忆层”内部补成双层

不是只有一个 memory。

应该拆成：

- `Persona Memory`
  - 稳定画像
  - 风格偏好
  - 关系推进边界
- `Relationship Memory`
  - owner-target 共同历史
  - 时间线
  - 争吵/甜蜜/约会/礼物/inside jokes

### 6.2 在“推理层”内部补一个 Style Router

现在的推理层如果直接：

- context -> llm -> reply

会不稳定。

建议显式增加：

- `style_router`

职责：

- 根据画像和关系状态选风格
- 控制消息温度、长度、主动性、仪式感强度
- 对不同对象输出不同恋爱大师模式

## 7. 一版可执行的数据语义调整

本次建议优先复用现有表，不追求大迁移。

### 7.1 `agent_persona_report_v2`

建议 `report_json` 统一补齐以下结构：

```json
{
  "stable_traits": {},
  "expression_style": {},
  "relationship_style": {},
  "attachment_style": "",
  "love_language": [],
  "romance_pace": "",
  "taboo_rules": [],
  "coach_style_candidates": [],
  "confidence_breakdown": {}
}
```

### 7.2 `agent_memory_profile`

建议约定：

- `memory_scope = global`
  - 主人级长期偏好和助手策略
- `memory_scope = relationship`
  - 对象级关系摘要

`signals_json` 里统一加入：

- `intimacy_score`
- `progression_score`
- `tone_warmth`
- `date_ready_score`
- `wechat_ready_score`
- `risk_score`

### 7.3 `agent_memory_fact`

建议扩充事实类型并加证据等级：

- relationship_ritual
- relationship_trigger
- gift_preference
- date_preference
- communication_pattern
- rejection_pattern
- repair_pattern

### 7.4 `agent_action_plan`

建议 `plan_json` 增加：

- `persona_version`
- `relationship_memory_version`
- `selected_coach_style`
- `why_this_style`
- `trigger_signals`

这样每条动作计划都能回答：

- 为什么这次用这种语气
- 为什么这次推进/不推进

## 8. 产品侧立刻可落的 6 个功能

### 8.1 智能画像页新增“可纠偏画像内核”

不要只展示大段报告，增加：

- 表达风格
- 关系节奏
- 雷区
- 推荐恋爱风格

并支持“一键纠偏”。

### 8.2 恋爱助手页新增“对象级关系记忆”

每个对象展示：

- 当前阶段
- 最近进展信号
- 甜蜜点
- 雷区
- 推荐风格

### 8.3 聊天页新增“这句话不像 TA”反馈入口

用于反向修正：

- 风格
- 长度
- 推进速度

### 8.4 红娘推荐页新增“为什么适合”

用画像内核解释：

- 节奏兼容
- 表达兼容
- 兴趣兼容
- 关系风格兼容

### 8.5 助手计划页新增“本次风格选择原因”

把黑盒推理变成可解释：

- 当前亲密度低
- 对方更吃生活感
- 最近对方响应偏慢
- 暂不建议直接换微信

### 8.6 管理台新增“画像纠偏 / 记忆纠偏 / 风格路由”观测能力

方便运营和产品看：

- 当前风格选择分布
- 哪些纠偏高频出现
- 哪些对象最容易推错

## 9. 最终判断

如果只问一句：

- `colleague-skill` 和 `ex-skill` 对 AiLeMe 有没有启发？

答案是：

- `有，而且启发点非常具体`

但真正应该学的不是“生成某个人的分身”，而是：

- 把人物蒸馏成多层结构
- 把关系记忆独立成对象级知识层
- 把纠偏和版本化产品化
- 让运行时先选人格/风格，再生成内容

所以对 AiLeMe 的正确转译是：

- `colleague-skill -> 智能画像内核`
- `ex-skill -> 对象级关系记忆`
- `AiLeMe 自己现有的 Java + runtime + graph + governance -> 真正的自治恋爱助手`

## 10. 下一步建议

建议按以下顺序继续推进：

1. 先重构 `智能画像` 输出结构，把“报告”升级成“报告 + 画像内核”
2. 再给 `agent_memory_profile / agent_memory_fact` 补齐关系记忆语义
3. 给 runtime 增加 `style_router`
4. 在 H5 加画像纠偏、关系纠偏、话术纠偏入口
5. 最后再把这些能力并入 `relationship program` 的自治 loop

这样做的好处是：

- 不会把 AiLeMe 做成“会说话的壳”
- 会真正做成“会理解对象、会记住关系、会持续学习、会受控执行”的恋爱 Agent
