# 用户首登关系画像 V2

## 1. 来源记录

- 当前功能在代码中的直接承载是首登草稿表 `sql/20260405_user_onboarding_persona.sql` 与首登服务链路：
  - `GET /app/persona/onboarding/my`
  - `POST /app/persona/onboarding/assess`
  - `POST /app/persona/onboarding/confirm`
- 现有实现不是正式版 `AI Persona` 报告，而是一个更轻量的首登蒸馏层：
  - 用少量问题快速理解用户
  - 先生成关系画像草稿
  - 再反哺资料文案、标签与后续推荐
- 本地历史记录显示，这个方向源于 “Human 3.0 / 蒸馏人类” 的启发，但产品上不应直接照搬通用人格测试，而应收敛为“恋爱社交版关系理解引导”。

## 2. CEO Review

### 2.1 这不是资料补全功能

它真正的价值不是“少填几项资料”，而是：

1. 用户第一次进入 app，就感到“这个产品先理解我，再让我经营自己”。
2. 把抽象的 AI 能力，变成用户立刻感知得到的首个卖点。
3. 让后续推荐、红娘节奏、破冰方式和资料文案有同一份起点。

### 2.2 正确的产品定位

建议对外定位为：

- `关系理解引导`
- `初始关系画像`
- `让月老先理解你`

不建议对外强调：

- 人格测试
- 心理测评
- Human 3.0 复刻版

### 2.3 Scope 决策

V1/V2 应坚持：

- 7 题以内，2 分钟左右完成
- 先给理解感，再给资料预填
- 不另造一套正式画像系统
- 首登草稿与正式 `user_persona_snapshot` 分层

## 3. Design Review

### 3.1 情绪路径

理想路径不是“答题 -> 出结果”，而是：

1. 先承诺：只花 2 分钟，且不会覆盖已填资料
2. 提问时解释：每一步会影响什么
3. 出结果时让用户感觉“被理解”，不是“被贴标签”
4. 应用前给用户控制权：文案和标签可分别决定是否应用

### 3.2 页面必须传达的 4 件事

1. 我理解到你是谁
2. 我会如何为你服务
3. 哪些方式不适合你
4. 这次具体会改动资料页哪里

### 3.3 结果页的核心结构

- 画像标题 + 副标题
- 3 条核心洞察
- 红娘/推荐/破冰服务策略
- 雷区提醒
- 资料草稿与标签预览
- 可控开关：应用文案 / 应用标签

## 4. Eng Review

### 4.1 已有稳定边界

- 首登草稿独立存储在 `user_onboarding_persona`
- JSON 草稿可演进，不必频繁改表
- `confirm` 只补空白字段，不覆盖用户已有文案

### 4.2 V2 结构建议

在 `persona_json` 中扩展表达层字段，而不是开新表：

- `archetypeTitle`
- `archetypeSubtitle`
- `coreInsights`
- `recommendedApproach`
- `avoidSignals`

这样可以同时服务：

- 首登结果页展示
- 后续推荐解释卡
- 红娘话术和开场策略

### 4.3 后续推荐链路挂点

下一阶段建议把以下字段接给推荐/红娘系统：

- `relationshipGoal`
- `romancePace`
- `communicationStyle`
- `emotionalNeeds`
- `attractionPreferences`
- `riskFlags`
- `preferredMatchmakerStyle`
- `recommendedOpeningStyle`

## 5. 本次 V2 已落地

- 后端：首登画像草稿增加“标题 / 洞察 / 服务策略 / 雷区提示”
- 前端：结果页强化为“被理解 + 可控应用”的体验
- 文案：从“首登预填”升级为“关系理解引导”

## 6. 下一步建议

1. 在资料页或我的页面增加“重新校准关系画像”入口
2. 把 `recommendedOpeningStyle` 接入红娘首句建议
3. 把 `riskFlags` 接入推荐过滤和聊天提示
4. 为首登画像补埋点：完成率、跳过率、应用率、二次重做率
