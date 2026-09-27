# Relationship OS Frontend 2.0 方案

生成时间：2026-04-08  
适用范围：`aileme` 新 2.0 用户端前端项目  
视角：`P7 + DX Review`

## 1. 一句话结论

建议为 2.0 单独新建一个前端项目，定位为：

**Relationship OS Web**

它是一个 `H5 first`、`mobile first`、`desktop adaptive` 的旗舰前端，不继续堆在 `multi-platform-app` 里，也不直接把 `hongniang-workbench` 或 `ai-le-me-ui` 改造成用户端。

推荐路线：

- 新建独立目录，例如 `relationship-os-web`
- 技术栈采用 `Vue 3 + TypeScript + Vite + Vue Router + Pinia`
- UI 层采用 `自定义业务组件 + 设计 token + UnoCSS`
- 复用现有后端接口、画像链路和部分请求/鉴权策略
- `qiuou-ux` 保留为多端实验轨道，不承担 2.0 旗舰体验

## 2. 为什么要单独起 2.0 项目

### 现状判断

当前仓库里前端已经分成几条线：

- `multi-platform-app`
  1.0 主用户端，历史包袱最多，且仍在开发中
- `qiuou-ux`
  uni-app x 轨道，偏多端覆盖验证，当前页面还很轻
- `hongniang-workbench`
  红娘工作台，偏 B 端工作台
- `ai-le-me-ui`
  管理后台基座，不适合用户端 2.0 品牌体验

### 为什么不能继续在 1.0 上硬改

- 1.0 当前分支仍在开发，继续重构会把新体验和旧链路缠在一起
- 2.0 的目标不是“修页面”，而是“重做产品心智”
- Relationship OS 需要新的信息架构、交互动线、视觉节奏，不适合在旧页面上贴补丁

### 为什么不建议直接把 `qiuou-ux` 定为旗舰 2.0

`qiuou-ux` 的方向本身没错，但它天然背着“多端一致性”约束。

而你这次明确要的是：

- H5 手机端体验要好
- PC 端不是简单拉伸，而是自适应重排
- 首页、关系页、模型页要能承载 agent 式复杂交互

这类体验，纯 Web 项目更容易做得干净、快、强。

## 3. 2.0 项目的定位

### 产品定位

2.0 不是传统社交 feed app。

它是：

**Relationship OS，先理解你，再替你判断，再陪你行动，再帮你表达。**

### 前端项目定位

这个新前端项目承担的是：

- 品牌旗舰体验
- H5 主入口
- PC 自适应增强体验
- onboarding 到 relationship cockpit 的完整闭环

它不承担的是：

- 小程序强兼容
- 管理后台式表单堆叠
- 旧版 1.0 历史页面兼容

## 4. 技术路线决策

## 4.1 推荐方案

### 方案 C，推荐

新建独立 Web 项目：

```text
relationship-os-web/
  src/
    app/
    router/
    stores/
    services/
    layouts/
    modules/
      briefing/
      inspiration/
      relationship/
      model/
      onboarding/
      auth/
    components/
      primitives/
      business/
    styles/
      tokens.css
      theme.css
      motion.css
```

### 技术选型

- `Vue 3`
- `TypeScript`
- `Vite`
- `Vue Router`
- `Pinia`
- `UnoCSS`
- `VueUse`
- `Axios` 或基于 fetch 的轻请求层

### 为什么推荐这套

- 对 H5 和 PC 响应式最友好
- agent sidecar、drawer、split view、panel layout 更好做
- DX 明显比 uni-app H5 更顺
- 更适合做品牌级视觉和交互，而不是“多端最小公约数”

## 4.2 备选方案

### 方案 A，不推荐

继续在 `multi-platform-app` 上做 2.0 页面。

问题：

- 历史状态太重
- 旧路由和旧心智互相污染
- 验收时很容易出现“2.0 做了，但看起来还是 1.0”

### 方案 B，中间路线

直接把 `qiuou-ux` 扩成 2.0 主项目。

优点：

- 已有多端基础
- 有轻量 `http/auth/services` 骨架
- 以后回到小程序更方便

问题：

- 当前工程仍偏“多端验证壳子”
- 对 PC 端复杂自适应布局不如纯 Web 项目自然
- 容易为了多端一致牺牲旗舰体验

## 5. 响应式设计原则

## 5.1 总原则

不是做两套站点。

要做的是：

**一套信息架构，三套布局节奏。**

```text
mobile   : 单列 + 底部 tab + 抽屉
tablet   : 单列增强 + 局部双栏
desktop  : 双栏/三栏 + 固定侧边能力区
```

## 5.2 各端体验重点

### Mobile

- 拇指友好
- 主任务单线程
- 底部 tab 明确
- sidecar 以底部抽屉 / 全屏面板出现

### Tablet

- 卡片更舒展
- 列表和详情可半并列
- 关系页可做会话 + 建议双区

### Desktop

- 不是手机页面放大
- 首页做 briefing cockpit
- 关系页做“会话区 + AI sidecar + 风险/下一步”
- 我的模型页做多模块并排

## 5.3 关键断点

- `< 768px` mobile
- `768px - 1279px` tablet
- `>= 1280px` desktop

这和现有 [device.ts](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/hongniang-workbench/src/utils/device.ts) 里的判断可以保持一致，减少团队认知成本。

## 6. 页面信息架构

建议保留之前定下来的 4 个主页面：

```text
月老 | 灵感 | 关系 | 我的模型
```

## 6.1 月老

- 今日判断
- 今日 1 次有效连接
- 推荐对象卡
- 为什么推荐
- 备用动作

桌面增强：

- 左侧主任务
- 中间推荐对象流
- 右侧 AI 对你的理解摘要

## 6.2 灵感

- 案例
- 开场
- 表达
- 内容工厂入口

桌面增强：

- 左侧分类
- 中间内容流
- 右侧“改成适合我的版本”

## 6.3 关系

- 会话列表
- 当前关系阶段
- 风险提醒
- 建议回复
- 下一步推进建议

桌面增强：

- 左侧会话
- 中间聊天
- 右侧 AI sidecar

## 6.4 我的模型

- 当前 archetype
- 核心洞察
- AI 如何使用你的信号
- 控制权与重新校准
- 表达资产

桌面增强：

- 模型总览卡
- 洞察与策略卡
- 资产与权限卡

## 7. 与现有项目的复用边界

## 7.1 必须复用

- 后端接口域模型
- 登录鉴权规则
- onboarding/persona 服务链路
- `riskFlags`、`recommendedOpeningStyle` 等画像解释字段

## 7.2 可以借鉴

- `qiuou-ux` 的轻量请求封装
- `hongniang-workbench` 的设备判断、TypeScript 工程结构
- 现有 tab 页面里的业务接口和 mock 数据组织方式

## 7.3 不建议直接复用

- `uview-plus` 风格的旧交互壳
- 管理后台式组件库心智
- 1.0 的页面结构和 CSS 体系

## 8. DX Review 结论

虽然这是用户端项目，但这个新前端项目本身也要有好 DX，不然团队很快被拖慢。

## 8.1 目标开发者画像

```text
TARGET DEVELOPER PERSONA
========================
Who:       1-2 名前端工程师，兼顾产品实现和体验打磨
Context:   需要快速搭建新页面、频繁联调、同时看移动端和桌面端效果
Tolerance: 5 分钟内起不来项目，或者改个页面要找半天目录，就会掉效率
Expects:   一条命令启动、路由清晰、模块边界清楚、mock 和真实接口切换简单
```

## 8.2 DX 北极星

- `TTHW < 5 分钟`
- `pnpm install && pnpm dev` 一次启动
- 默认就能看到 mobile + desktop 两种体验
- 新增页面时不需要猜目录和状态归属
- 接口错误能快速定位到业务模块

## 8.3 DX 评分

### 当前如果直接在旧工程上做

| 维度 | 评分 | 问题 |
|---|---:|---|
| Getting Started | 4/10 | 多个前端并存，新人不知道该改哪个 |
| API/CLI/SDK | 6/10 | 现有服务层可参考，但跨项目不统一 |
| Error Messages | 5/10 | 业务态和网络态边界不清 |
| Documentation | 3/10 | 缺少 2.0 新项目前端入口文档 |
| Upgrade Path | 4/10 | 1.0 与 2.0 没有明确切分策略 |
| Dev Environment | 6/10 | 可运行，但缺统一命令和 mock 规范 |
| Community | 5/10 | 团队内部可口头传承，文档化不足 |
| Measurement | 2/10 | 没有 TTHW、联调和构建体验指标 |

### 目标状态

| 维度 | 目标 |
|---|---:|
| Getting Started | 9/10 |
| API/CLI/SDK | 8/10 |
| Error Messages | 8/10 |
| Documentation | 8/10 |
| Upgrade Path | 8/10 |
| Dev Environment | 9/10 |
| Community | 7/10 |
| Measurement | 7/10 |

## 8.4 DX 设计要求

### Getting Started

- 根目录文档必须明确写出：1.0、2.0、工作台分别是什么
- 2.0 项目单独 README
- 3 步内启动成功

### Dev Environment

- `pnpm dev`
- `pnpm dev --host`
- `pnpm build`
- `pnpm test`
- `pnpm lint`

### Mock 与联调

- `mock` 和 `api` 双模式
- 环境变量只保留最少集合
- 登录态、persona、推荐对象、关系侧边栏都要有稳定 mock

### 目录约束

- 页面按业务模块分，不按通用类型乱堆
- `components/primitives` 只放纯 UI 原语
- `components/business` 放 Relationship OS 业务组件
- `services` 只做数据访问，不做页面状态

### 错误处理

- 401 统一跳登录
- persona 空态有专门文案
- 推荐失败时有降级视图，不白屏
- sidecar 失败时不阻塞聊天主流程

## 9. 实施分期

## Phase 0，脚手架与规则

- 新建 `relationship-os-web`
- 建立路由、store、layout、services、tokens 目录
- 建立 mobile/tablet/desktop 断点策略
- 接好基础鉴权与 request 层
- 写 README 和启动命令

## Phase 1，闭环最小版

- 登录
- onboarding / persona
- 月老
- 我的模型

目标：

- 用户能完成首登理解引导
- 用户能看到“今天建议做什么”
- 用户能理解“AI 如何理解我”

## Phase 2，差异化核心版

- 灵感页
- 关系页
- AI sidecar
- 推荐 why explain layer

目标：

- 用户第一次真正感受到 Relationship OS 与传统社交 app 的差异

## Phase 3，品牌增强版

- 内容工厂深接
- 关系记忆
- 桌面端效率布局强化
- 埋点与体验优化

## 10. 项目命名建议

推荐从以下名字里选一个：

- `relationship-os-web`
- `aileme-app-v2`
- `qiuou-web`

我更推荐：

**`relationship-os-web`**

原因：

- 对内语义最清楚
- 不和 1.0 混
- 以后即使品牌文案变化，工程名依然稳

## 11. 不该做的事

- 不要让 2.0 再背上小程序兼容优先
- 不要先选大而全组件库，再把品牌感做没
- 不要复制 1.0 页面再改标题
- 不要把 AI sidecar 只做成弹窗模板机
- 不要把 persona 做成一次性答题页，而不回写全局体验

## 12. 下一步建议

按执行顺序，我建议这样走：

1. 先确认 2.0 项目名与技术路线
2. 我直接为新项目出目录结构和初始化文件清单
3. 然后我开始搭 `Phase 0` 脚手架
4. 第一批只实现 `登录 -> onboarding -> 月老 -> 我的模型`

这条路最稳，也最像真的在做一个新产品，而不是重装修旧房子。
