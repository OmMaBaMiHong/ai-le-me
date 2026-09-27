# 红娘私域经营一体化全量交付 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 AiLeMe 单仓和现有社交服务内，一次性交付“红娘用户池 + 牵线案件 + 微信群资产 + 群运营任务 + 红娘手机/平板工作台 + App 内牵线申请/微信交换”一体化能力，不新拆系统、不新拆服务。

**Architecture:** 继续以 `ai-le-me-modules/ai-le-me-shejiao-app` 作为业务真源，以 `ai-le-me-ui` 承载管理后台，以 `multi-platform-app` 承载红娘手机/平板工作台。群运营按“双轨制”建设：历史个人微信群只做资产登记与人工运营记录；可真实同步/触达/任务执行的群统一走 `企业微信 + SCRM Provider` 抽象，默认正式实现 `WeCom`，同时保留通用 `SCRM HTTP Bridge` 适配口。

**Tech Stack:** Java 21, Spring Boot 3.5, MyBatis-Plus, Vue3 + Vite + Element Plus, uni-app + Vue3, Redis, MySQL, 企业微信开放接口, Provider 抽象层。

---

## 0. 方案定稿

### 0.1 交付边界

- 不新起系统，不新起微服务，不新做第二套 CRM。
- 不直接托管历史个人微信号消息发送，不做高风险个人号机器人方案。
- 一次性交付内容包含：
  - 红娘用户池升级
  - 牵线案件全链路
  - App 内牵线申请与微信交换
  - 微信群资产管理
  - 群运营任务中心
  - 企业微信正式 Provider
  - 通用 SCRM Bridge 接口
  - 红娘手机/平板工作台
  - 后台统计与权限收口

### 0.2 外部方案结论

- `MoChat` 官方仓库说明其是“基于企业微信的开源 SCRM 应用开发框架&引擎，也是一套通用的企业私域流量管理系统”，搜索结果可见其已有“客户群、自动拉群”等成熟能力，适合作为功能对标，但不适合直接并入当前 Java 主栈。[MoChat GitHub](https://github.com/mochat-cloud/mochat)
- `OpenSCRM` 官方仓库仍可参考能力设计，但当前搜索结果仅能稳定确认它是 `Go + React` 的企业微信私域系统；维护状态不适合作为你们主系统底座来依赖。[OpenSCRM GitHub](https://github.com/openscrm/api-server)
- `LinkWeChat` 外部公开资料普遍将其描述为“基于企业微信的开源 SCRM 系统，采用 Java 微服务架构”，因此它更适合作为你们 Java 侧接口和模块拆分参考，而不是替代你们现有社交服务。[相关检索结果](https://download.csdn.net/download/weixin_44976692/88805371)

### 0.3 核心决策

- 群管理产品形态：做成 AiLeMe 里的“红娘私域经营中心”，不是再造独立 SaaS。
- 正式三方主通道：`WeComGroupTouchProvider`
- 兼容生态通道：`ScrmBridgeProvider`
- 移动端入口：直接并入 `multi-platform-app`，面向红娘角色展示工作台，不拆新客户端。
- 牵线动作统一沉淀为“案件”，不再散落在用户关系、活动报名、聊天消息里。

## 1. 模块文件地图

### 1.1 SQL / 数据层

**Modify:**
- `sql/20260405_hongniang_match_case_and_wechat_group.sql`

**Create:**
- `sql/20260405_hongniang_private_domain_runtime.sql`

**Scope:**
- 补齐牵线案件索引、状态字段、统计字段
- 新增案件申请、SOP 模板、群运营执行记录、移动工作台看板聚合所需结构

### 1.2 Java 后端

**Modify:**
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/service/impl/HongniangMatchCaseServiceImpl.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/service/impl/HongniangWechatGroupServiceImpl.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/service/impl/HongniangServiceImpl.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/impl/SocialIntentService.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/impl/SocialIntentMessageService.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/controller/AppSocialIntentController.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/controller/AppHongniangController.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/controller/AppMessageController.java`

**Create:**
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/controller/HongniangPrivateDomainDashboardController.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/controller/AppHongniangWorkbenchController.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/api/service/HongniangPrivateDomainService.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/service/impl/HongniangPrivateDomainServiceImpl.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/admin/HongniangMatchRequestEntity.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/admin/HongniangGroupSopTemplateEntity.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/admin/HongniangGroupTaskExecutionEntity.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/dao/HongniangMatchRequestDao.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/dao/HongniangGroupSopTemplateDao.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/dao/HongniangGroupTaskExecutionDao.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/runtime/thirdparty/group/GroupTouchProviderRegistry.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/runtime/thirdparty/group/ScrmBridgeProvider.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/quartz/HongniangGroupTaskDispatchQuartzHandler.java`
- `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/quartz/HongniangGroupSyncQuartzHandler.java`

### 1.3 管理后台

**Modify:**
- `ai-le-me-ui/src/views/shejiao/hongniang/hongniang-user/index.vue`
- `ai-le-me-ui/src/views/shejiao/hongniang/match-case/index.vue`
- `ai-le-me-ui/src/views/shejiao/hongniang/wechat-group/index.vue`
- `ai-le-me-ui/src/api/hongniang/matchCase.ts`
- `ai-le-me-ui/src/api/hongniang/wechatGroup.ts`
- `ai-le-me-ui/src/api/hongniang/types.ts`

**Create:**
- `ai-le-me-ui/src/views/shejiao/hongniang/private-domain-dashboard/index.vue`
- `ai-le-me-ui/src/api/hongniang/privateDomain.ts`

### 1.4 用户端 / 红娘工作台

**Modify:**
- `multi-platform-app/src/pages/hongniang/index.vue`
- `multi-platform-app/src/pages/hongniang/detail.vue`
- `multi-platform-app/src/api/hongniang.js`
- `multi-platform-app/src/api/message.js`
- `multi-platform-app/src/shared/social-intent/index.js`
- `multi-platform-app/src/subpackages/chat/detail.vue`

**Create:**
- `multi-platform-app/src/subpackages/hongniang-workbench/index.vue`
- `multi-platform-app/src/subpackages/hongniang-workbench/cases.vue`
- `multi-platform-app/src/subpackages/hongniang-workbench/case-detail.vue`
- `multi-platform-app/src/subpackages/hongniang-workbench/groups.vue`
- `multi-platform-app/src/subpackages/hongniang-workbench/group-detail.vue`
- `multi-platform-app/src/subpackages/hongniang-workbench/users.vue`
- `multi-platform-app/src/subpackages/hongniang-workbench/api.js`

## 2. 数据模型总方案

### 2.1 保留现有表

- `hongniang_user_relation`：只做红娘用户池归属，不承载关系进度。
- `hongniang_match_case`
- `hongniang_match_progress`
- `hongniang_match_case_group`
- `hongniang_wechat_group`
- `hongniang_wechat_group_user`
- `hongniang_group_touch_task`
- `hongniang_group_touch_log`

### 2.2 新增表

#### `hongniang_match_request`

用途：承接“红娘为两位用户发起牵线申请”的业务动作，支持两种触达方式。

关键字段：
- `id`
- `case_id`
- `hongniang_id`
- `from_user_id`
- `to_user_id`
- `request_channel`
- `request_status`
- `request_message`
- `intent_request_id`
- `wechat_share_snapshot`
- `expire_time`
- `create_time`
- `update_time`

枚举：
- `request_channel`：
  - `1 app私信申请`
  - `2 红娘代分享微信`
- `request_status`：
  - `0 待发送`
  - `1 已发送`
  - `2 已接受`
  - `3 已拒绝`
  - `4 已过期`

#### `hongniang_group_sop_template`

用途：存群欢迎语、群规则、跟进提醒、活动召回等可复用 SOP 模板。

#### `hongniang_group_task_execution`

用途：记录群任务单次执行明细，避免仅在 touch_log 里看不到调度维度。

### 2.3 业务约束

- 一个案件允许存在多次“牵线申请”，但同一方向同一渠道在 `待发送/已发送` 状态下只能有一条有效申请。
- 历史个人微信群可绑定用户和案件，但 `provider_type=0` 时只允许“记录型任务”，不允许真实发送。
- 群任务执行状态与第三方响应必须保留原文摘要，便于排障。
- 红娘成功数完全由案件阶段 `已结婚/已生子` 聚合得出。

## 3. 一次性交付任务清单

### Task 1: 冻结私域域模型与数据库迁移

**Files:**
- Modify: `sql/20260405_hongniang_match_case_and_wechat_group.sql`
- Create: `sql/20260405_hongniang_private_domain_runtime.sql`

- [ ] **Step 1: 补全一次性交付 SQL 迁移**

```sql
ALTER TABLE hongniang_match_case
  ADD COLUMN source_snapshot_json text NULL COMMENT '来源快照',
  ADD COLUMN latest_request_status tinyint NULL COMMENT '最近牵线申请状态';

CREATE TABLE IF NOT EXISTS hongniang_match_request (
  id bigint NOT NULL AUTO_INCREMENT,
  case_id bigint NOT NULL,
  hongniang_id bigint NOT NULL,
  from_user_id bigint NOT NULL,
  to_user_id bigint NOT NULL,
  request_channel tinyint NOT NULL,
  request_status tinyint NOT NULL DEFAULT 0,
  request_message varchar(500) DEFAULT NULL,
  intent_request_id varchar(64) DEFAULT NULL,
  wechat_share_snapshot varchar(255) DEFAULT NULL,
  expire_time datetime DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_case_status (case_id, request_status),
  KEY idx_to_user_status (to_user_id, request_status)
);
```

- [ ] **Step 2: 写菜单和权限迁移**

```sql
INSERT INTO sys_menu (menu_name, parent_id, path, component, menu_type, perms)
VALUES ('红娘私域总览', @hongniang_parent_menu_id, 'private-domain-dashboard', 'shejiao/hongniang/private-domain-dashboard/index', 'C', 'hongniang:privateDomain:list');
```

- [ ] **Step 3: 在本地开发库执行迁移**

Run:

```bash
python3 scripts/run_mysql_sql.py sql/20260405_hongniang_match_case_and_wechat_group.sql
python3 scripts/run_mysql_sql.py sql/20260405_hongniang_private_domain_runtime.sql
```

Expected:
- 新表创建成功
- `sys_menu` 中出现 `用户池管理 / 牵线案件 / 微信群管理 / 红娘私域总览`

### Task 2: 后端聚合服务与后台总览

**Files:**
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/controller/HongniangPrivateDomainDashboardController.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/api/service/HongniangPrivateDomainService.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/service/impl/HongniangPrivateDomainServiceImpl.java`
- Create: `ai-le-me-ui/src/views/shejiao/hongniang/private-domain-dashboard/index.vue`
- Create: `ai-le-me-ui/src/api/hongniang/privateDomain.ts`

- [ ] **Step 1: 定义后台聚合接口**

```java
@GetMapping("/admin/hongniang-private-domain/overview")
public R overview(@RequestParam(required = false) Integer hongniangId) { ... }

@GetMapping("/admin/hongniang-private-domain/kanban")
public R kanban(@RequestParam Map<String, Object> params) { ... }
```

- [ ] **Step 2: 聚合核心指标**

```java
Map<String, Object> result = Map.of(
    "poolUserCount", poolUserCount,
    "openCaseCount", openCaseCount,
    "successCaseCount", successCaseCount,
    "wechatGroupCount", wechatGroupCount,
    "touchTaskPendingCount", touchTaskPendingCount,
    "followTodayCount", followTodayCount
);
```

- [ ] **Step 3: 后台总览页做四个主面板**

```ts
const panels = [
  '用户池与跟进',
  '牵线案件漏斗',
  '微信群资产与活跃度',
  '待执行群任务与异常'
]
```

- [ ] **Step 4: 回归接口和页面**

Run:

```bash
mvn -f /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/pom.xml -pl ai-le-me-admin -am -DskipTests compile
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-ui && npm run build:dev
```

Expected:
- 后端编译通过
- 后台构建通过

### Task 3: 完成牵线案件与牵线申请一体化

**Files:**
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/service/impl/HongniangMatchCaseServiceImpl.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/admin/HongniangMatchRequestEntity.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/dao/HongniangMatchRequestDao.java`
- Modify: `ai-le-me-ui/src/views/shejiao/hongniang/hongniang-user/index.vue`
- Modify: `ai-le-me-ui/src/views/shejiao/hongniang/match-case/index.vue`

- [ ] **Step 1: 在案件服务中新增“发起牵线申请”能力**

```java
public Long createMatchRequest(Integer caseId,
                               Integer fromUserId,
                               Integer toUserId,
                               Integer requestChannel,
                               String requestMessage,
                               Long operatorId) { ... }
```

- [ ] **Step 2: 后台案件详情增加申请时间线**

```ts
type MatchRequest = {
  id: number
  requestChannel: number
  requestStatus: number
  requestMessage: string
  fromUserId: number
  toUserId: number
}
```

- [ ] **Step 3: 后台用户池“牵线”弹窗支持直接选配对方并选择触达方式**

```ts
const requestChannelOptions = [
  { label: 'App私信发起牵线申请', value: 1 },
  { label: '红娘代分享微信', value: 2 }
]
```

- [ ] **Step 4: 校验业务约束**

Run:

```bash
curl -X POST http://127.0.0.1:8080/admin/hongniang-match-case/create
curl -X POST http://127.0.0.1:8080/admin/hongniang-match-case/create-request
```

Expected:
- 同一案件可多次申请，但同方向未处理申请不可重复创建
- 申请记录能在案件详情看到

### Task 4: 打通 App 内牵线申请与微信交换

**Files:**
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/impl/SocialIntentService.java`
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/impl/SocialIntentMessageService.java`
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/controller/AppSocialIntentController.java`
- Modify: `multi-platform-app/src/shared/social-intent/index.js`
- Modify: `multi-platform-app/src/subpackages/chat/detail.vue`
- Modify: `multi-platform-app/src/pages/hongniang/detail.vue`

- [ ] **Step 1: 扩展社交意图消息，支持牵线场景**

```json
{
  "type": "hongniang_match_request",
  "requestId": "xxx",
  "caseId": 123,
  "hongniangId": 88,
  "requesterUid": 1001,
  "targetUid": 1002,
  "requestChannel": 1,
  "message": "红娘向你推荐了一位合适对象，是否愿意进一步了解？"
}
```

- [ ] **Step 2: 保留现有微信申请能力，但把结果回写案件申请表**

```java
result.put("caseId", caseId);
result.put("matchRequestId", matchRequestId);
```

- [ ] **Step 3: 用户端聊天页新增“牵线申请卡片”**

```ts
const cardTypes = ['wechat_request', 'gift_tip', 'hongniang_match_request']
```

- [ ] **Step 4: 接受牵线后支持两条路**

```ts
if (requestChannel === 1) {
  // 打开聊天并发送确认卡片
} else {
  // 展示红娘代分享的微信号或继续走微信确认
}
```

- [ ] **Step 5: 回归聊天与消息流**

Run:

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app && npm run dev:h5
```

Expected:
- 红娘发起牵线申请后，对方在消息/聊天中可见卡片
- 接受/拒绝后案件申请状态同步更新

### Task 5: 完成微信群资产、SOP、任务中心

**Files:**
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/service/impl/HongniangWechatGroupServiceImpl.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/admin/HongniangGroupSopTemplateEntity.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/admin/HongniangGroupTaskExecutionEntity.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/dao/HongniangGroupSopTemplateDao.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/admin/dao/HongniangGroupTaskExecutionDao.java`
- Modify: `ai-le-me-ui/src/views/shejiao/hongniang/wechat-group/index.vue`

- [ ] **Step 1: 新增 SOP 模板与任务执行表**

```sql
CREATE TABLE hongniang_group_sop_template (...);
CREATE TABLE hongniang_group_task_execution (...);
```

- [ ] **Step 2: 群任务中心支持三类任务**

```java
enum TaskBizType {
    GROUP_ANNOUNCEMENT,
    FOLLOW_REMINDER,
    ACTIVITY_RECALL
}
```

- [ ] **Step 3: 后台群详情页增加四块内容**

```ts
const sections = ['基础信息', '绑定用户', '绑定案件', '任务与执行记录']
```

- [ ] **Step 4: 历史微信群和企微群按钮差异化**

```ts
const canSend = row.groupType === 2 && [1, 2].includes(row.providerType)
```

- [ ] **Step 5: 验证真实发送限制**

Expected:
- 个人微信群只能登记任务，不能真实发
- 企微群可同步、可下发任务、可看执行结果

### Task 6: 实现 WeCom 正式 Provider + 通用 SCRM Bridge

**Files:**
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/runtime/thirdparty/group/WecomGroupTouchProvider.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/runtime/thirdparty/group/ScrmBridgeProvider.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/runtime/thirdparty/group/GroupTouchProviderRegistry.java`
- Modify: `ai-le-me-ui/src/views/system/thirdparty/index.vue`
- Modify: `ai-le-me-ui/src/views/system/thirdparty/GenericServiceConfigPanel.vue`

- [ ] **Step 1: 新增三方配置分组**

```json
{
  "serviceType": "wecom_customer",
  "providerCode": "wecom",
  "config": {
    "corpId": "",
    "corpSecret": "",
    "agentId": "",
    "token": "",
    "aesKey": ""
  }
}
```

```json
{
  "serviceType": "scrm_vendor",
  "providerCode": "http_bridge",
  "config": {
    "baseUrl": "",
    "appKey": "",
    "appSecret": "",
    "vendorCode": ""
  }
}
```

- [ ] **Step 2: Provider 注册中心统一调度**

```java
public interface GroupTouchProvider {
    ProviderResult syncGroup(HongniangWechatGroupEntity group);
    ProviderResult createTask(HongniangGroupTouchTaskEntity task);
    ProviderResult retryTask(HongniangGroupTouchTaskEntity task);
}
```

- [ ] **Step 3: WeCom Provider 实现正式 HTTP 调用**

```java
String accessToken = wecomTokenService.getToken(corpId, corpSecret);
```

- [ ] **Step 4: SCRM Bridge Provider 实现标准 HTTP 合同**

```json
POST /openapi/group/sync
POST /openapi/group/task/create
POST /openapi/group/task/retry
```

- [ ] **Step 5: 在后台三方配置页以“一个服务一个弹窗”方式展示**

Expected:
- `wecom_customer` 和 `scrm_vendor` 直接在三方配置统一面板中编辑
- 不再拆碎成难用的单个 key/value 页

### Task 7: 红娘手机/平板工作台一次性并入 multi-platform-app

**Files:**
- Create: `multi-platform-app/src/subpackages/hongniang-workbench/index.vue`
- Create: `multi-platform-app/src/subpackages/hongniang-workbench/cases.vue`
- Create: `multi-platform-app/src/subpackages/hongniang-workbench/case-detail.vue`
- Create: `multi-platform-app/src/subpackages/hongniang-workbench/groups.vue`
- Create: `multi-platform-app/src/subpackages/hongniang-workbench/group-detail.vue`
- Create: `multi-platform-app/src/subpackages/hongniang-workbench/users.vue`
- Create: `multi-platform-app/src/subpackages/hongniang-workbench/api.js`
- Modify: `multi-platform-app/src/pages.json`
- Modify: `multi-platform-app/src/shared/hongniang/role.js`

- [ ] **Step 1: 新增红娘角色工作台入口**

```json
{
  "path": "subpackages/hongniang-workbench/index",
  "style": {
    "navigationBarTitleText": "爱情主理人工作台"
  }
}
```

- [ ] **Step 2: 工作台首页放四个主入口**

```ts
const entries = [
  '我的用户池',
  '牵线案件',
  '微信群',
  '今日待跟进'
]
```

- [ ] **Step 3: 布局按手机/平板自适应**

```css
grid-template-columns: repeat(auto-fit, minmax(280rpx, 1fr));
```

- [ ] **Step 4: 案件详情页支持直接发起牵线申请和记录跟进**

```ts
await api.createMatchRequest(payload)
await api.addCaseProgress(payload)
```

- [ ] **Step 5: 群详情页支持查看用户、案件、任务**

Expected:
- 手机竖屏可单列
- 平板横屏可双列/三列
- 不需要电脑也能完成红娘日常推进

### Task 8: 权限、Quartz、回归与上线清单

**Files:**
- Modify: `sql/20260405_hongniang_private_domain_runtime.sql`
- Create: `docs/release-checklists/2026-04-05-hongniang-private-domain.md`

- [ ] **Step 1: 权限前缀统一**

```text
hongniang:privateDomain:*
hongniang:matchCase:*
hongniang:wechatGroup:*
hongniang:groupTouch:*
hongniang:matchRequest:*
```

- [ ] **Step 2: Quartz 任务接入**

```text
hongniangGroupSyncQuartzHandler
hongniangGroupTaskDispatchQuartzHandler
```

- [ ] **Step 3: 一次性回归命令**

Run:

```bash
mvn -f /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/pom.xml -pl ai-le-me-admin -am -DskipTests compile
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-ui && npm run build:dev
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app && npm run dev:h5
```

- [ ] **Step 4: 关键人工回归**

Expected:
- 后台能从用户池建档、推进案件、发起牵线申请、绑定群、建群任务
- App 能收到牵线申请、接受/拒绝、继续聊天或交换微信
- 红娘手机工作台可完成每日主流程
- 企微群能同步和发任务
- 历史个人微信群不会出现“可发但实际发不出去”的误导按钮

## 4. 最终交付标准

- 后台侧边栏完整出现：
  - `用户池管理`
  - `牵线案件`
  - `微信群管理`
  - `红娘私域总览`
- 红娘在后台和手机/平板两端都能完成核心工作。
- 牵线动作统一沉淀为案件和申请记录。
- 微信申请和 App 私信牵线申请都能回写案件。
- 群资产、群任务、群同步、群绑定用户/案件可统一查看。
- 正式三方接入走企微；SCRM 走 Bridge 协议兼容。

## 5. 风险与硬性规则

- 个人微信号自动化群控风险高，禁止作为正式主方案。
- 任何真实发送必须建立在企微或合规 SCRM 能力之上。
- 不拆新系统，不复制用户、案件、群数据到第二套业务库。
- 如果第三方能力受限，前端必须清晰展示“仅登记”状态，不能伪装为已发送。

## 6. 执行顺序建议

1. 先跑 SQL 与菜单。
2. 再做 Java 聚合与 Provider。
3. 再补后台总览和群任务中心。
4. 再打通 App 社交意图和红娘工作台。
5. 最后做全链路联调与回归。

Plan complete and saved to `docs/superpowers/plans/2026-04-05-hongniang-private-domain-full-delivery.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
