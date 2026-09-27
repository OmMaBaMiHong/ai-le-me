# AiLeMe 项目总览

## 0. 最高边界规则

以下 4 条是本项目最高边界规则，优先级高于常规实现习惯。每次做需求前必须先过一遍，违反时先停下，不要直接开做：

1. 不要重复造轮子。
2. 不要套壳。
3. 不要新起路由。
4. 不要为了“兼容”做硬兼容。

补充说明：

- 优先复用现有页面、现有链路、现有配置入口、现有数据结构。
- 如果入口还没配，优先补到现有入口体系里，不要先新做一个页面等入口后补。
- 如果确实认为必须突破以上规则，必须先重新核对现有实现和产品入口，确认没有可复用方案后再决定。

## 1. 仓库定位

这是一个单仓多端项目，当前至少包含 5 条主线：

1. Java 后端业务核心
2. H5 / uni-app 多端用户端
3. Vue3 管理后台
4. Flutter 客户端
5. Python agent runtime

做需求时不要只盯一个端。这个项目很多能力都跨越「前端入口 -> Java 接口 -> 配置中心 -> 订单/账单 -> 消息通知 -> AI runtime / 三方服务」整条链。

## 1.1 产品定位

AiLeMe 当前产品定位不是传统婚恋工具叠加一点 AI，而是：

- `AI 驱动的新型社交产品`

当前最核心的三条 agent 业务线：

1. `智能画像`
2. `智能红娘`
3. `智能恋爱助手`

三者之间不是独立烟囱，默认应共享一套智能底座：

- 用户资料 / 行为信号
- 长短期记忆
- 检索与召回
- 关系图谱
- 结构化推理结果
- Java 当前结果分发

默认建设顺序：

1. 先把 `智能画像` 做强，沉淀结构化 persona
2. 再让 `智能红娘` 基于 persona + 偏好 + 检索重排推荐对象
3. 最后让 `智能恋爱助手` 复用画像、记忆、关系图谱与推荐结果

## 2. 模块地图

| 模块 | 路径 | 职责 | 典型入口 |
| --- | --- | --- | --- |
| Java 启动模块 | `ai-le-me-admin` | Spring Boot 启动、环境配置、装配 system/shejiao 模块 | `ai-le-me-admin/src/main/java/org/dromara/AilemeApplication.java` |
| 公共基础层 | `ai-le-me-common` | Web、安全、MyBatis、Redis、OSS、Sa-Token、OpenAPI 等公共依赖与能力 | `ai-le-me-common/pom.xml` |
| 社交业务核心 | `ai-le-me-modules/ai-le-me-shejiao-app` | 用户、资料、帖子、视频、红娘、活动、聊天、支付、账单、AI 内容等主业务 | `ai-le-me-modules/ai-le-me-shejiao-app/pom.xml` |
| 系统模块 | `ai-le-me-modules/ai-le-me-system` | 系统管理、字典、租户、三方配置等后台基础能力 | `ai-le-me-modules/ai-le-me-system/pom.xml` |
| AI 网关 | `ai-le-me-modules/ai-le-me-ai-gateway` | 模型路由、Provider 适配、运行时配置解析 | `ai-le-me-modules/ai-le-me-ai-gateway/pom.xml` |
| 管理后台 | `ai-le-me-ui` | Vue3 + Element Plus 管理台，负责三方配置、Quartz、订单、账单、内容审核等 | `ai-le-me-ui/package.json` |
| 用户端主前端 | `multi-platform-app` | 当前唯一继续上线的多端前端，覆盖 H5 / 小程序 / App Plus / 鸿蒙元服务 | `multi-platform-app/package.json` |
| Flutter 客户端 | `qiuou-flutter` | 独立客户端实现，复用 Java 后端合同，但界面与交互独立演进 | `qiuou-flutter/pubspec.yaml` |
| Python agent runtime | `ai-le-me-agent-runtime` | 人设推理、陪伴策略、结构化 AI 输出、后续 LangGraph/记忆/关系图谱边界 | `ai-le-me-agent-runtime/pyproject.toml` |
| SQL / 脚本 / 文档 | `sql`、`scripts`、`docs` | 测试数据、迁移 SQL、回归脚本、方案文档、发布检查 | 对应目录内文件 |

## 3. 各端当前边界

### 3.1 Java 后端

- 业务真实来源仍然是 Java。
- 权限、支付、账单、消息推送、风控、审核、租户隔离都应该由 Java 负责。
- Python runtime 目前是智能边界，不应直接抢业务主写入权。

### 3.2 H5 / uni-app

- `multi-platform-app` 是当前唯一保留并持续上线的用户端主前端。
- 页面总入口在 `multi-platform-app/src/pages.json`。
- 当前页面大类包括：首页、广场、消息、我的、红娘、活动、话题、帖子、支付、资料编辑、会员等。
- H5 默认开发命令是 `npm run dev:h5`，脚本会先清理占用的 `5173` 端口。
- `我的` 页的“我的服务”宫格入口必须由后端配置接口返回，当前以 `userMenu/list` 为准；前端不要写死图标、文案或跳转链接。

### 3.3 管理后台

- `ai-le-me-ui` 负责系统配置与运营台能力。
- 当前尤其重要的是三方配置、Quartz、OSS、订单、账单、退款、审核等后台页面。
- 遇到「运营可配」「路由可切换」「第三方成本可控」的问题，通常要同步管理后台。

### 3.4 Flutter

- `qiuou-flutter` 是单独客户端工程。
- 当前已拆出 `auth`、`chat`、`content`、`finance`、`home`、`hongniang`、`match`、`message`、`profile`、`video` 等 feature。
- API 合同约定见 `qiuou-flutter/docs/api-contract.md`，和 H5 共用后端协议。

### 3.5 Python agent runtime

- `ai-le-me-agent-runtime` 是独立 Python 服务，不是 Java 内嵌子模块。
- 适合承接：画像报告、陪伴回复建议、下一步策略、后续 LangGraph 工作流、记忆和图谱层。
- 当前设计原则：Java 管业务，Python 管智能；共享三方配置读取，但 runtime 自己的数据放独立表。

## 4. 业务主链路

### 4.1 用户与资料

- 注册/短信登录/用户信息拉取是最基础主链。
- 资料字段会同时影响：推荐、匹配、聊天开场、内容生成、人设报告、形象图生成、红娘/活动展示。
- 做资料改动时，至少联动检查：
  - H5 编辑页
  - Flutter 对应表单或展示页
  - Java DTO / VO / 实体
  - 审核状态与展示状态
  - AI 画像 / 文案 / 生图是否读取这些字段

### 4.2 首页 / 广场 / 内容

- 首页和广场承接图文、视频、活动、圈子等内容流。
- 内容不是只有 `post` 表展示，还会牵涉：
  - 发帖入口
  - 媒体上传
  - AI 图文/视频生成
  - 详情页、卡片流、推荐流
  - 点赞、评论、收藏、分享

### 4.3 红娘 / 活动 / 圈子 / 群聊

- 这是社交推进最重的一条业务线。
- 主链通常是：
  - 红娘列表 / 详情
  - 申请认识 / 破冰页
  - 活动报名 / 支付 / 审核
  - 话题圈子 / 成员
  - 活动群聊 / 私聊
- 任何一处产品设计调整，都不要漏掉消息通知、支付扣费、报名状态、群聊准入和后台审核字段。

### 4.4 消息 / 聊天 / 破冰

- 消息模块不仅是会话列表，还包含通知、访客、群聊、破冰动作。
- 「申请微信」「打赏 / 送心意」「私信跳转」「破冰礼物」本质上都和账户扣费、消息创建、关系推进有关。

### 4.5 支付 / 爱情币 / VIP / 打赏

当前统一方向已经很明确，不要重复造轮子。

#### 已存在的支付骨架

- `pay_product`：统一商品表
- `pay_order`：统一订单表
- `pay_order_detail`：订单事件流水
- `account`：爱情币账户
- `account_bill`：爱情币账单流水

#### 已确认的实体与历史命名

- `RechargeEntity` 对应 `pay_product`
- `VipOptionEntity` 对应 `pay_product`
- `UserRechargeEntity` 对应 `pay_order`
- `PayOrderDetailEntity` 对应 `pay_order_detail`
- `BillEntity` 对应 `account_bill`

#### 当前结论

- 会员购买、充值、活动报名已经在统一订单模型附近。
- 普通打赏目前更偏直接账务流：`AccountService + BillServiceImpl.rewardIntegral(...)`。
- 平台抽成已经有逻辑，不要另做一套分账模型。

#### 礼物 / AI 礼物建议

- 礼物商品复用 `pay_product`
- 礼物订单复用 `pay_order`
- 订单明细复用 `pay_order_detail`
- 扣币/入账复用 `account` 与 `account_bill`
- AI 礼物的生成资产、任务状态单独补 `gift_asset` / `gift_task` 一类表，不要污染支付主表

### 4.6 AI 内容 / 生图 / Runtime

- Java 侧更适合做：
  - 任务入库
  - 权限校验
  - 扣费
  - 审核
  - 媒体回填
- Python runtime 更适合做：
  - 提示词编排
  - Persona reasoning
  - Long-running workflow
  - 结构化输出
- 模型路由、三方配置、成本控制不要散在代码里硬编码，优先走 `sys_third_party_provider` 与 `sys_third_party_route_rule` 体系。

## 5. 当前重要技术事实

### 5.1 后端环境

- 根 `pom.xml` 是多模块 Maven 工程。
- Java 版本是 21。
- Spring Boot 版本是 3.5.9。
- 默认 `dev` profile 已激活。
- `ai-le-me-admin/src/main/resources/application-dev.yml` 与 `application-prod.yml` 目前都收敛到了单库 `master` 数据源。
- `ai-le-me-admin` 只是启动入口，真正会被打进包里的业务代码还来自 `ai-le-me-common`、`ai-le-me-system`、`ai-le-me-shejiao-app`、`ai-le-me-ai-gateway`。
- 只要改动不止发生在 `ai-le-me-admin/src`，就必须按根 reactor 重编，不能赌单模块打包会自动带上最新依赖源码。

### 5.2 前端环境

- `multi-platform-app` 使用 uni-app + Vue3 + Pinia。
- `ai-le-me-ui` 使用 Vue3 + Vite + Element Plus。
- H5 常用开发端口是 `5173`。
- Java 默认本地端口是 `8080`。

### 5.3 Agent runtime 环境

- Python 3.12
- FastAPI + SQLAlchemy
- 预留 LangGraph、ChromaDB、Neo4j、Mem0、Letta、Graphiti 等能力

## 6. 做需求时的全链路检查清单

每次做功能，至少过一遍下面这张清单：

1. 这是只改 H5，还是 H5 / Flutter / 管理后台都要改？
2. 页面入口在哪个端暴露？`pages.json`、Flutter route、管理后台菜单是否都要同步？
3. Java 接口、参数对象、VO、数据库实体是否完整闭环？
4. 是否涉及三方配置、模型路由、短信、OSS、支付配置？
5. 是否涉及订单、账单、账户余额、平台抽成、退款或回调？
6. 是否需要消息通知、站内信、群聊、推送联动？
7. 是否会影响 AI 内容、画像、提示词、生图、生视频、审核？
8. 是否有后台可配置项，还是写死在前端 / 后端了？
9. 是否要补测试数据、回归脚本、冒烟路径？
10. 是否需要考虑脏数据修复、历史数据迁移、SQL 回填？

## 7. 建议的工作顺序

1. 先明确功能落点属于哪条主链。
2. 找到主前端入口和对应 Java 控制器。
3. 顺着账务、通知、配置、审核四条横向链再扫一遍。
4. 如果涉及 AI，再判断是 Java 任务编排还是 Python runtime 智能处理。
5. 改完后至少跑一轮对应的造数/回归脚本。

## 8. 常用入口命令

### 后端

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme
mvn -f pom.xml -pl ai-le-me-admin -am -DskipTests package
java -jar ai-le-me-admin/target/ai-le-me-admin.jar
```

补充边界：

- 如果只是查看日志或临时联调，`spring-boot:run` 可以用，但前提是没有改动 `ai-le-me-common`、`ai-le-me-modules/*`。
- 只要动了 `ai-le-me-shejiao-app`、`ai-le-me-system`、`ai-le-me-ai-gateway`、`ai-le-me-common` 任何一个模块，就统一走上面的 root reactor 打包命令。
- 这样可以避免 `admin` 启动了，但包里嵌套的 `BOOT-INF/lib/ai-le-me-shejiao-app-*.jar` 还是旧版本。

### H5 / uni-app

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app
npm run dev:h5
```

### 管理后台

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-ui
npm run dev
```

### Flutter

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter
flutter pub get
flutter run
```

### Python agent runtime

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-agent-runtime
source .venv/bin/activate
uvicorn agent_runtime.main:app --reload --port 8091
```

## 9. 关键参考文件

- 后端总入口：`ai-le-me-admin/src/main/java/org/dromara/AilemeApplication.java`
- 后端总配置：`ai-le-me-admin/src/main/resources/application.yml`
- 开发环境配置：`ai-le-me-admin/src/main/resources/application-dev.yml`
- 生产环境配置：`ai-le-me-admin/src/main/resources/application-prod.yml`
- H5 页面注册：`multi-platform-app/src/pages.json`
- H5 项目说明：`multi-platform-app/README.md`
- 管理后台依赖与脚本：`ai-le-me-ui/package.json`
- Flutter API 合同：`qiuou-flutter/docs/api-contract.md`
- Agent 方案：`docs/agent-stack-landing.md`
- 发布 smoke：`docs/release-smoke-checklist.md`
- 支付统一模型：`payment_account_schema_reset.sql`

## 10. 给后续礼物 agent 的直接结论

礼物能力不要新起支付体系，直接挂在现有支付/账务模型上；AI 礼物只把「生成任务与生成资产」拆出去。这样后续无论是纯打赏、破冰礼物、图文礼物、视频礼物，链路都能统一。
