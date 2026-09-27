# Persona Distillation Platform V2

## 0. 这份方案解决什么问题

之前的 v1 方案把能力定义成：

- `self_onboarding`
- `ex_relationship`
- `colleague_workstyle`
- 以问答 + 粘贴文本为主

这已经能跑通一个窄场景，但还不够像产品。

你这次的新要求本质上把问题升级了：

1. 不只是 API，而是 app 用户可直接上传聊天记录、截图、材料。
2. 不只是“前任/同事”两个 feature，而是统一的人物蒸馏能力。
3. 不只是生成一句 AI 结论，而是要有证据链、纠错、版本、删除、治理。
4. 不只是 runtime 能算，而是能接入 AiLeMe 的 `智能画像 -> 红娘 -> 恋爱助手` 主链路。

所以这次 v2 的结论是：

- 内部能力统一升级为：`人物蒸馏平台`
- 对外产品语言不主打“蒸馏前任”，而是主打：`认识我 / 关系复盘 / 识人分析`
- app 首发先做 `上传素材 + 生成识人结果 + 保存快照 + 纠错迭代`
- “像对方一样陪聊”不作为私域人物的一期能力

---

## 1. 外部项目启发，哪些该学，哪些不能直接抄

### 1.1 `ex-skill`

有价值的点：

- 明确证明了 `Relationship Memory + Persona` 双层结构很适合关系类人物蒸馏。
- 数据源定义很清楚：聊天记录、社交截图、照片、口述。
- 有版本、纠错、增量 merge 的思路。

不能直接照搬的点：

- 它是本地 skill 形态，不是多租户产品。
- 安全边界靠 README 声明，不是系统治理。
- 它的目标更接近“回忆/陪聊”，AiLeMe 需要的是“关系理解 + 沟通策略 + 风险提醒”。

### 1.2 `yourself-skill`

有价值的点：

- 非常适合拿来校准 AiLeMe 已有 `Human 3.0 / 首登关系画像` 的产品化表达。
- 清楚地区分了 `Self Memory` 和 `Persona`。
- 强调“素材层”和“蒸馏层”分离，这对我们做证据治理非常重要。

不能直接照搬的点：

- 它是单人自我镜像产品，不天然覆盖“识别别人”和“关系复盘”。
- 它没有处理 AiLeMe 的推荐、红娘、聊天助手复用链路。

### 1.3 `nuwa-skill`

有价值的点：

- 最强启发不是“名人视角”，而是它的 `证据 -> 提炼 -> 验证 -> 诚实边界` 方法论。
- 三重验证很适合迁移到 AiLeMe：
  - 跨材料复现
  - 能推断新场景
  - 有人物区分度
- 它强调“诚实边界”，这对私域人物蒸馏非常关键。

不能直接照搬的点：

- 女娲主要处理公开人物和公开材料。
- AiLeMe 要处理的是真实私人关系材料，风险等级高很多。
- 女娲适合“认知框架”，而 AiLeMe 还要覆盖“关系行为模式”和“沟通风险”。

### 1.4 `awesome-persona-distill-skills`

有价值的点：

- 它提醒我们不要把产品想成“前任工具”。
- 正确抽象不是按仓库名分功能，而是按 `scene catalog` 分能力。

结论：

- AiLeMe 的正确方向不是“再做一个前任.skill”。
- 而是做一个可扩展的 `scene catalog + material ingestion + evidence-aware distillation` 平台。

---

## 2. CEO Review: 这到底应该是什么产品

### 2.1 正确定位

这不是：

- 人格测试
- 心理测评
- 前任模拟器
- 同事聊天分身

这应该是：

- `AI 驱动的关系理解引擎`
- `AiLeMe 智能画像的素材化升级版`
- 一个能把“素材”沉淀成“结构化识人结果”的平台能力

### 2.2 AiLeMe 为什么值得做这个，而不是通用 AI 工具做

因为 AiLeMe 已经有后续动作承接能力：

- `智能画像` 可以消费蒸馏结果
- `智能红娘` 可以消费沟通偏好、风险点、节奏建议
- `智能恋爱助手` 可以消费关系对象画像和互动策略
- `资料页 / 我的页 / 聊天助手` 都已经有入口可以复用

通用 AI 工具最多给一段分析。
AiLeMe 可以把分析变成：

- 资料文案优化
- 推荐解释
- 破冰建议
- 聊天风险提醒
- 红娘策略路由

### 2.3 产品核心不是“模拟这个人”，而是“理解这个人”

一期必须主动放弃一个很诱人的方向：

- 不做私域人物的 `像 ta 一样和你聊天`

原因：

- 风险高
- 容易引向骚扰/沉迷/替代真实关系
- 不符合 AiLeMe 当前主产品线

一期应该聚焦输出：

- 这个人给人的第一印象
- 真实行为模式
- 和你在关系里的高频循环
- 沟通建议
- 风险与边界提醒
- 结论的证据来源与置信度

### 2.4 v2 的产品抽象

不要把 `前任` 和 `同事` 设计成两个独立系统。

正确抽象：

- `scene_type` 定义用户要解决的问题
- `relation_label` 定义这个人和用户的关系

建议的 scene catalog：

1. `self_bootstrap`
   - 目标：认识我自己，服务资料、推荐、红娘。
2. `private_person_analysis`
   - 目标：分析一个真实的人，服务关系理解、沟通策略、复盘。
3. `public_figure_perspective`
   - 目标：蒸馏公开人物/方法论，服务广场和创作者玩法。

其中 app 首发只开：

1. `self_bootstrap`
2. `private_person_analysis`

`private_person_analysis` 先给两个关系预设：

- `前任`
- `同事`

但内部保留扩展位：

- `暧昧对象`
- `现任`
- `老板`
- `朋友`

### 2.5 首发原则

首发要赢的不是“功能多”，而是“第一次用就感觉真懂我/真懂 ta”。

所以一期取舍：

- 要做：上传材料、证据化分析、结果保存、纠错迭代、跨业务复用
- 不做：全自动导微信、人物拟真对聊、开放式公共分享、复杂社交裂变

---

## 3. CSO Review: 私域人物蒸馏必须先有治理，再谈体验

### 3.1 风险分级

这类能力的风险远高于普通 AI 文案生成，因为输入物是：

- 聊天记录
- 截图
- 照片
- 可能涉及第三方隐私和敏感关系事实

建议把主体分为三类，不同类用不同策略：

1. `self`
   - 风险最低
   - 可开放最完整能力
2. `private_person`
   - 风险最高
   - 必须限制输出用途和留存策略
3. `public_figure`
   - 允许做方法论/视角蒸馏
   - 但仅基于公开材料

### 3.2 一期必须锁死的禁区

对于 `private_person`，系统明确禁止输出：

- 联系方式猜测
- 住址/工作单位/行动轨迹推断
- 跟踪、骚扰、报复、PUA 建议
- 冒充对方发言的长期陪聊人格
- “如何拿捏 ta”“如何逼 ta 回头”这类操控型策略

允许输出的是：

- 关系模式理解
- 沟通中的风险提醒
- 更体面的表达建议
- 如何保护自己边界
- 结论的不确定性说明

### 3.3 数据生命周期

建议 v1 采用激进保守策略：

- 原始上传文件：默认 `7 天` 自动删除
- OCR/转写后的中间文本：默认 `30 天` 自动删除
- 蒸馏后的结构化证据和快照：用户不删除则保留
- 用户删除 subject 时：
  - 删除快照
  - 删除证据
  - 删除材料索引
  - 尽快触发 OSS 物理删除

原因：

- 对用户最有价值的是“蒸馏结果”，不是长期囤积原始私密素材。
- 原始素材是合规和泄露风险最大的部分，应该最短留存。

### 3.4 存储与访问控制

安全上至少要做到：

1. 原始材料单独 OSS/Bucket 存储，不混到普通业务图片里。
2. 文件名随机化，避免路径泄露真实语义。
3. Java 生成短期签名上传地址，app 不直拿永久凭据。
4. 原始文本、OCR 文本、结构化结论分层存储。
5. 日志、trace、异常上报里禁止打印原文内容。
6. runtime 只拿必要的 sanitized payload，不直接持有全量业务权限。

### 3.5 审计与用户可感知治理

治理不能只在后台做，前台要让用户知道边界：

- 上传前明确提示“仅用于关系理解，不用于骚扰或冒充”
- 结果页展示“结论依据于哪些材料类型”
- 提供“这条结论不对”入口
- 提供“删除全部材料和结果”入口
- 对低置信结论打 `推测` 标签，而不是装作确定

### 3.6 运行时风控

建议在 Java host 侧新增一层 `policy gate`：

- 文件类型校验
- 敏感图像/二维码/证件识别
- 大体量文件限额
- 用户频率限制
- 高风险 prompt/output 二次拦截

runtime 侧新增：

- prompt safety 模板
- output policy filter
- risk flag 输出字段

结论：

- 这个项目的难点不是“能不能分析出来”
- 而是“怎么在不把 AiLeMe 送进风险区的前提下，把能力做成产品”

---

## 4. Design Review: app 首版怎么做才像产品，而不是素材上传器

### 4.1 app 首版的信息架构

不建议新起一套完全独立的大页面体系。
遵守仓库现有规则，优先复用现有入口：

1. `我的 / profile`
   - 增加“人物蒸馏”服务卡
2. `编辑资料页`
   - 承接 `认识我` 的补充素材上传
3. `聊天助手`
   - 承接“识人结果如何帮我沟通”

一期用户看到的三个文案入口建议：

- `认识我`
- `关系复盘`
- `识人分析`

不要直接叫：

- 蒸馏前任
- 蒸馏同事

这些词可以作为内部 preset，不作为首页主文案。

### 4.2 标准流程

#### A. 认识我

1. 先走现有首登 7 题关系画像
2. 结果页提示“可补充聊天/截图，让画像更懂你”
3. 用户补充材料后，生成增强版自我画像

#### B. 识人分析 / 关系复盘

1. 选择关系预设
   - 前任
   - 同事
2. 填基本信息
   - 代号
   - 与你的关系
   - 你最想搞明白什么
3. 上传材料
   - 聊天记录
   - 截图
   - 照片
   - 补充描述
4. 系统异步分析
5. 结果页展示
6. 用户确认保存 / 修改 / 删除

### 4.3 结果页必须长什么样

不能只是一段 AI 总结。

建议固定为 6 个区块：

1. `一句话总结`
   - 这段关系/这个人的主模式是什么
2. `核心洞察`
   - 3 到 5 条
3. `相处与沟通建议`
   - 用户能立刻拿去用
4. `风险与边界`
   - 不适合继续触碰的点
5. `证据来源`
   - 来自聊天/截图/口述，各自占比和代表性样本
6. `置信度与盲区`
   - 哪些是高确定，哪些只是推测

### 4.4 设计上的一个关键差异

`private_person_analysis` 的结果页，不应该长得像“AI 模拟角色卡”。

它应该更像：

- 识人报告
- 关系复盘卡
- 沟通教练面板

而不是：

- 让用户沉浸式和前任继续聊天

### 4.5 纠错与进化体验

结果页必须给两个反馈动作：

1. `这条说得对`
2. `这条不对，补充说明`

这样才能建立：

- correction layer
- 快照版本
- 用户信任

### 4.6 广场宫格的后续玩法

等 app 首版跑顺后，再把广场入口做成：

- `人格实验室`
- `名人视角`
- `关系理解馆`

但广场开放的应优先是：

- `public_figure_perspective`
- 模板化的公开示例

而不是鼓励用户公开分享私域人物材料。

---

## 5. Eng Review: 真正要建设的是“素材到快照”的流水线

### 5.1 v1 架构为什么不够

之前只设计 `subject + snapshot` 两张主表是不够的。

因为现在产品已经包含：

- 原始素材管理
- OCR/转写
- 证据提取
- 异步任务
- 纠错和版本
- 删除和回收

所以数据模型至少要升级为 6 层。

### 5.2 建议的数据模型

#### 1. `agent_distillation_subject`

表示一个蒸馏对象。

关键字段：

- `owner_user_id`
- `scene_type`
- `subject_type`
- `relation_label`
- `subject_name`
- `status`
- `latest_snapshot_id`
- `latest_job_id`
- `confirmed`

#### 2. `agent_distillation_material`

表示上传的素材。

关键字段：

- `subject_id`
- `material_type`
- `storage_url`
- `mime_type`
- `size_bytes`
- `source_origin`
- `ocr_status`
- `parse_status`
- `retention_expire_at`
- `deleted_at`

#### 3. `agent_distillation_evidence`

表示从素材中抽出的结构化证据，不再依赖原始文件才能解释结论。

关键字段：

- `subject_id`
- `material_id`
- `evidence_type`
- `excerpt_json`
- `confidence_score`
- `sensitivity_level`
- `is_user_confirmed`

#### 4. `agent_distillation_snapshot`

表示某次生成的结果版本。

关键字段：

- `subject_id`
- `scene_type`
- `snapshot_json`
- `confidence_summary_json`
- `trace_id`
- `version_no`

#### 5. `agent_distillation_job`

表示异步分析任务。

关键字段：

- `subject_id`
- `job_type`
- `status`
- `progress_pct`
- `error_code`
- `runtime_trace_id`

#### 6. `agent_distillation_feedback`

表示用户纠错和确认。

关键字段：

- `snapshot_id`
- `insight_key`
- `feedback_type`
- `feedback_text`

### 5.3 服务边界怎么拆

#### Java host 负责

- 用户权限
- 上传签名
- 任务创建
- 风控 gate
- 材料索引
- 快照主写入
- 删除与保留策略
- 运营配置和开关

#### Python runtime 负责

- OCR 后文本归一
- 多材料 evidence 提取
- scene-specific 推理
- 证据冲突处理
- confidence 计算
- 结构化 snapshot 生成

结论保持不变：

- Java 管业务真源
- Python 管智能蒸馏

### 5.4 推荐的处理流水线

建议统一成异步 job pipeline：

1. app 创建 subject
2. app 上传材料到 OSS
3. Java 写入 `material`
4. Java 创建 `job`
5. preprocess worker 执行：
   - 文件校验
   - OCR / 文本解析 / EXIF 清洗
   - 敏感信息预处理
6. runtime 执行 evidence extraction：
   - 行为模式
   - 情绪模式
   - 关系冲突点
   - 沟通偏好
   - 风险边界
7. runtime 生成 snapshot
8. Java 落 `snapshot + evidence`
9. app 轮询或订阅任务状态

### 5.5 scene contract 应该怎么定义

v2 不建议再把合同设计成“按 feature 单独一套”。

统一请求结构建议：

```json
{
  "scene_type": "private_person_analysis",
  "subject_type": "private_person",
  "relation_label": "ex_partner",
  "analysis_goal": "我想知道为什么每次一吵架就会断联",
  "owner": {},
  "subject": {},
  "materials": [],
  "signals": [],
  "policy_context": {}
}
```

统一输出结构建议：

```json
{
  "summary": "",
  "core_insights": [],
  "interaction_guidance": [],
  "risk_flags": [],
  "evidence_cards": [],
  "confidence_notes": [],
  "persona_kernel": {},
  "service_hooks": {}
}
```

其中 `service_hooks` 预留给 AiLeMe 下游消费：

- `recommended_opening_style`
- `matchmaker_style_hint`
- `assistant_guardrails`
- `profile_copy_hint`

### 5.6 复用现有 AiLeMe 资产

这件事不应该另起第二套画像系统。

建议复用：

- 现有 `user_onboarding_persona`
  - 继续作为 `self_bootstrap` 的轻量首登草稿
- 现有 `AgentRuntimeBridgeService`
  - 新增 distillation route，而不是旁路新通信体系
- 现有 app 页面：
  - `profile.vue`
  - `edit-info`
  - `chat/assistant.vue`

### 5.7 运行时方法论

runtime 的蒸馏逻辑建议吸收外部项目的三套思想：

1. `Memory + Persona`
   - 适合自我和关系材料
2. `Evidence-aware distillation`
   - 先抽证据，再下结论
3. `Honest boundary`
   - 信息不足时明确说不足

建议内部拆成四层：

1. `material normalization`
2. `evidence extraction`
3. `scene synthesis`
4. `feedback merge`

### 5.8 测试策略

这个能力不能只测接口通不通。

必须补四类测试：

1. parser/OCR 单测
2. scene synthesis golden cases
3. policy red-team cases
4. 删除与留存回收测试

还要补一套合成测试数据，避免把真实私聊材料放进仓库。

---

## 6. 建议的产品合同

### 6.1 Scene 级别

一期只开放：

- `self_bootstrap`
- `private_person_analysis`

二期开放：

- `public_figure_perspective`

### 6.2 Material 级别

一期支持：

- `text_note`
- `chat_export_text`
- `chat_screenshot`
- `social_screenshot`
- `image_photo`

一期暂不支持：

- 微信账号直连拉取
- 飞书/钉钉 OAuth 同步
- 大批量历史自动抓取

### 6.3 Output 级别

对 `self_bootstrap`：

- 自我画像
- 关系偏好
- 红娘服务策略
- 资料文案建议

对 `private_person_analysis`：

- 对象画像
- 关系循环总结
- 沟通策略
- 风险边界
- 证据与盲区

不开放：

- 私域人物全拟真分身聊天

---

## 7. app-first 路线图

### Phase 0: 升级方案，不急着写大功能

先统一：

- 场景抽象
- 安全边界
- 数据模型
- runtime 合同

### Phase 1: app 内首发

目标：

- `认识我` 支持补充材料增强画像
- `关系复盘/识人分析` 支持前任、同事两种预设
- 用户可上传文字和截图
- 结果可保存、删除、纠错

落点：

- 我的页入口
- 编辑资料页补充入口
- 聊天助手消费入口

### Phase 2: 业务联动

把 distillation 结果接到：

- 红娘建议
- 聊天 guardrails
- 推荐解释卡
- 资料文案建议

### Phase 3: 广场宫格

优先做：

- 名人视角
- 方法论人物馆
- 公开示例案例

谨慎做：

- 用户私域人物分享

---

## 8. 这版方案锁定的关键决策

1. 对外主叙事是 `关系理解`，不是 `模拟某个人`。
2. `前任/同事` 是首发预设，不是最终产品抽象。
3. 私域人物一期不开放拟真陪聊，只开放识人和沟通建议。
4. 上传原始素材必须短留存，不能长期默认囤积。
5. 工程上必须从 `subject/snapshot` 升级为 `subject/material/evidence/snapshot/job/feedback` 六层模型。
6. app 首发优先复用现有入口，不单独平地起一个新系统。
7. 广场后续优先承接 `public_figure_perspective`，而不是公开私域人物。

---

## 9. 与旧版计划的关系

文件：

- `docs/superpowers/plans/2026-04-08-relationship-distillation-platform.md`

可以继续视作：

- `结构化问答版 v1`

这份新文档则是：

- `素材上传 + 治理 + 异步流水线 + app 产品化版 v2`

两者不是互相冲突，而是 v2 明确扩展并覆盖了 v1 的产品边界。

---

## 10. 推荐下一步

建议下一步不要直接写大段实现代码，而是先做 3 份落地文档或最小骨架：

1. `distillation domain model`
   - 把六层数据模型落成 SQL 草案
2. `runtime scene contract`
   - 把 `self_bootstrap` 和 `private_person_analysis` 的输入输出 schema 锁死
3. `app entry flow`
   - 把 `我的页 / 编辑资料页 / 聊天助手` 的首版交互稿锁死

如果这三份先锁定，后面的 Java / Python / uni-app 实现就会很顺，不容易返工。
