# AiLeMe Flutter H5 Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `qiuou-flutter` follow `multi-platform-app` page-by-page and button-by-button without changing existing business logic, route semantics, or backend contract.

**Architecture:** `multi-platform-app` H5/uni-app is the business source of truth for page structure, button behavior, auth/compliance gates, and navigation semantics. Flutter must reuse existing route registration in `qiuou-flutter/lib/app/app_routes.dart`, existing router dispatch in `qiuou-flutter/lib/app/app_router.dart`, and existing feature entry files under `qiuou-flutter/lib/features/**`; parity work means filling behavior gaps inside those carriers, not inventing new flows.

**Tech Stack:** Flutter, Navigator 1.0 route table, existing `*Service` API clients, AiLeMe Java backend contract (`Authorization`, `clientid`, `x-platform`, `{ code, msg, result }`), uni-app H5 source pages in `multi-platform-app/src`.

---

## Hard Rules

- Do not redesign H5 business flow.
- Do not add new business routes unless the route is only a compatibility alias for an existing H5 page.
- Do not replace backend menu/config with Flutter-side heuristics when backend already returns a target.
- Do not collapse multiple H5 pages into one “generic page shell” unless H5 already does that.
- For each page, parity means the same entry, same API, same auth guard, same switch/compliance gate, same empty/loading/error handling, and the same result after tapping each primary button.

## Repo Truths For This Work

- H5 route source of truth: [pages.json](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages.json)
- Flutter route registry: [app_routes.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/app/app_routes.dart)
- Flutter route dispatch: [app_router.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/app/app_router.dart)
- Flutter route normalization/inference: [app_navigation.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/core/navigation/app_navigation.dart)
- API baseline: [api-contract.md](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/docs/api-contract.md)

## Current Findings

### 1. Route truth is split in two layers

- H5 `pages.json` exposes 82 real routes.
- Flutter `AppRoutes.businessRoutes` covers most business destinations, but a noticeable set is exposed only as normalized aliases, not original H5 paths.
- Missing raw H5 route compatibility in Flutter today:
  - `/subpackages/auth/*`
  - `/subpackages/profile/*`
  - `/subpackages/settings/*`
  - `/subpackages/vip/vip`
  - `/subpackages/web/webview`
  - `/subpackages/chat/assistant`
  - `/subpackages/hongniang-workbench/*`
- Flutter-only alias/internal routes currently in use:
  - `/pages/auth/login`
  - `/pages/user/contact`
  - `/pages/user/account-cancel`
  - `/pages/user/home`
  - `/pages/user/email-login`
  - `/pages/user/go-login`
  - `/pages/user/protocol`
  - `/pages/user/register`
  - `/pages/user/sms-login`
  - `/pages/user/persona-report`
  - `/pages/user/vip/vip`
  - `/pages/recommend-setting/recommend-setting`
  - `/pages/system/system`
  - `/pages/webview/webview`
  - `/subpackages/message/social-intent`
  - `/subpages/search/search`

### 2. Several Flutter tabs still contain “route exists but behavior is fake” risk

- [match_tab_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/match/match_tab_page.dart) still shows `暂未找到对应的真实页面入口` fallback.
- [message_tab_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/message/message_tab_page.dart) still shows the same placeholder fallback.
- [profile_tab_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/profile/profile_tab_page.dart) still depends on route inference + same placeholder fallback.
- [profile_service.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/profile/profile_service.dart) resolves menu target by inference; this is useful for兜底 but dangerous as a primary behavior.
- [main_shell.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/shell/main_shell.dart) hardcodes the middle `+` sheet; this must mirror H5 quick-publish rules instead of drifting.

### 3. The first parity wave must focus on the 4 tab pages

- H5 business usage starts from `首页 / 广场 / 消息 / 我的`.
- If these 4 tab pages are not behavior-accurate, all secondary pages feel broken even when routes exist.

## Parity Contract

For every page below, document and implement these seven checks before claiming parity:

1. Entry parity: same route intent, same parameters, same auth guard.
2. Data parity: same API endpoint family, same query/body semantics, same pagination/reset logic.
3. Button parity: same primary CTA, same secondary CTA, same disabled rules, same success/fail feedback.
4. Navigation parity: same next page, same query args, same return behavior.
5. State parity: same loading, empty, error, refresh, load-more, optimistic update rules.
6. Compliance parity: same filing switch, payment switch, assistant switch, persona switch, hongniang switch.
7. Presentation parity: safe-area adaptation is allowed; business structure changes are not.

## Page And Button Matrix

### A. Main Shell And Tabs

- `pages/tab/home`
  - H5 source: [home.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/tab/home.vue)
  - Flutter target: [home_tab_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/home/home_tab_page.dart)
  - Entry: app default tab index `0`
  - Minimum button contract:
    - Top tabs `抖音 / 同城 / 爱情主理人 / 圈子`
    - Video card actions `点赞 / 收藏 / 评论 / 进详情 / 进用户主页 / 私聊 / 打赏`
    - Same-city actions `切换城市 / 刷新列表 / 喜欢 / AI 破冰 / 打开用户主页 / 打开 AI 视频`
    - Love actions `主理人详情 / 活动详情 / 主理人广场 / 我的主理人页或申请`
    - Topic actions `切分类 / 进圈子 / 进帖子 / 申请圈主`
    - Popup action `我知道了`
  - Current gap:
    - Flutter already有 4 tab 内容承载，但必须逐项核对视频卡动作、同城动作、爱情主理人入口、话题分类与申请圈主逻辑。
    - H5 首页受备案开关影响，Flutter 也必须按同一开关隐藏 `love`。

- `pages/tab/match`
  - H5 source: [match.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/tab/match.vue)
  - Flutter target: [match_tab_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/match/match_tab_page.dart)
  - Entry: app tab index `1`
  - Minimum button contract:
    - Header tabs `推荐 / 热榜`
    - 推荐页入口 `话题广场 / 圈子推荐 / 最新动态`
    - 热榜页入口 `精选入口 / 快捷入口 / 热帖排行`
    - Feed card taps go to post detail or video detail according to type
  - Current gap:
    - 页面结构基本对齐，但仍存在 route fallback placeholder。
    - 必须把 H5 `navigation/getNav`、`link/list` 的跳转类型处理完整，不允许只做一层文案卡片。

- `pages/tab/message`
  - H5 source: [message.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/tab/message.vue)
  - Flutter target: [message_tab_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/message/message_tab_page.dart)
  - Entry: app tab index `2`, must auth-guard
  - Minimum button contract:
    - Top right `搜索 / 全部已读 / 设置`
    - Hub tabs `私信 / 好友 / 看过我的 / 评论 / 点赞 / 新粉丝 / 通知 / 关注我的`
    - Chat session tap enters chat detail
    - Friend swipe delete / tap continue chat
    - Visitor/fans tap opens user home
    - Metric card tap opens corresponding message list or detail
  - Current gap:
    - Flutter已有 tab 框架，但还要补齐 H5 的“全部已读”“搜索”“设置”“好友删除”“统一 badge”行为细节。
    - H5 里 assistant 入口受备案开关控制，Flutter也必须同样收口。

- `pages/tab/profile`
  - H5 source: [profile.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/tab/profile.vue)
  - Flutter target: [profile_tab_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/profile/profile_tab_page.dart)
  - Entry: app tab index `3`, must auth-guard for data
  - Minimum button contract:
    - Header card tap opens personal home or login
    - Account/VIP/Persona modules open exact destinations
    - Persona visibility switch toggles backend visibility, not only local UI
    - Stats `粉丝 / 关注 / 帖子 / 积分`
    - Service menu grid must follow backend `userMenu/list`
  - Current gap:
    - Flutter page currently mixes fixed cards with inferred menu routes.
    - Must remove menu guesswork where backend target exists; only keep inference as last-resort compatibility.

### B. Auth, Profile, Settings, VIP

- `subpackages/auth/login`
  - H5 source: [login.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/auth/login.vue)
  - Flutter alias target: `/pages/auth/login`, [login_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/auth/login_page.dart)
  - Buttons: 手机号输入、验证码发送、协议勾选、登录、去邮箱登录、去短信登录、去注册、返回
  - Gap: add raw H5 route compatibility; keep login redirect semantics identical.

- `subpackages/auth/email-login`
  - H5 source: [email-login.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/auth/email-login.vue)
  - Flutter alias target: `/pages/user/email-login`, [login_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/auth/login_page.dart)
  - Buttons: 邮箱输入、密码输入、登录、去注册、返回
  - Gap: alias compatibility + exact tab/form mode restore.

- `subpackages/auth/go-login`
  - H5 source: [go-login.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/auth/go-login.vue)
  - Flutter alias target: `/pages/user/go-login`, [login_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/auth/login_page.dart)
  - Buttons: 登录 CTA、返回原页面
  - Gap: must preserve forced-login return target.

- `subpackages/auth/register`
  - H5 source: [register.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/auth/register.vue)
  - Flutter alias target: `/pages/user/register`, [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: 账号输入、验证码发送、注册、去登录、返回
  - Gap: verify-code timing and agreement flow must match H5.

- `subpackages/auth/sms-login`
  - H5 source: [sms-login.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/auth/sms-login.vue)
  - Flutter alias target: `/pages/user/sms-login`, [login_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/auth/login_page.dart)
  - Buttons: 手机号、验证码、发送验证码、登录、切换其他登录方式
  - Gap: alias compatibility + same countdown, disabled, and error toast behavior.

- `subpackages/profile/contact`
  - H5 source: [contact.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/profile/contact.vue)
  - Flutter alias target: `/pages/user/contact`, [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: 复制微信号、查看二维码大图、返回
  - Gap: add raw route compatibility and match copy/view behavior.

- `subpackages/profile/home`
  - H5 source: [home.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/profile/home.vue)
  - Flutter alias target: `/pages/user/home`, [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: 关注/取消关注、私聊、举报、查看资料卡、查看作品、查看关系、返回
  - Gap: Flutter current user home is too light; must expand to H5 home contract instead of only showing top summary cards.

- `subpackages/profile/persona-report`
  - H5 source: [persona-report.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/profile/persona-report.vue)
  - Flutter alias target: `/pages/user/persona-report`, [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: 查看报告、下载报告、切换公开/私密、返回
  - Gap: must obey persona paid switch and visibility switch, not just show data.

- `subpackages/settings/protocol`
  - H5 source: [protocol.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/settings/protocol.vue)
  - Flutter alias target: `/pages/user/protocol`, [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: 协议类型切换、返回
  - Gap: add raw route compatibility and exact protocol type handling.

- `subpackages/settings/account-cancel`
  - H5 source: [account-cancel.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/settings/account-cancel.vue)
  - Flutter alias target: `/pages/user/account-cancel`, [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: 风险提示、确认注销、返回
  - Gap: add raw route compatibility and final confirm semantics.

- `subpackages/settings/system`
  - H5 source: [system.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/settings/system.vue)
  - Flutter alias target: `/pages/system/system`, [message_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/message/message_pages.dart)
  - Buttons: 查看系统消息、已读处理、进入详情
  - Gap: alias divergence and message read-state parity.

- `subpackages/vip/vip`
  - H5 source: [vip.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/vip/vip.vue)
  - Flutter alias target: `/pages/user/vip/vip`, [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: 会员套餐切换、支付、权益说明、返回
  - Gap: add raw route compatibility and payment switch gating.

- `pages/user/edit-info/edit`
  - H5 source: [edit.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/user/edit-info/edit.vue)
  - Flutter target: [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: 头像、相册、基础信息项、每个字段编辑入口、保存/提交
  - Gap: must follow H5 field grouping and submit flow, not自由裁剪字段顺序.

- `pages/user/edit-info/setting`
  - H5 source: [setting.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/user/edit-info/setting.vue)
  - Flutter target: [misc_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/common/misc_pages.dart)
  - Buttons: 铃声开关、震动开关、隐私开关、推荐设置、协议、注销、退出登录
  - Gap: current Flutter page is close, but every switch must map to H5/back-end/local pref exactly.

- `pages/user/edit-info/submit`
  - H5 source: [submit.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/user/edit-info/submit.vue)
  - Flutter target: [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: 输入/选择值、确认提交、返回
  - Gap: keep field-specific title/type/value semantics, no generic loose form.

### C. Topic, Discuss, Post, Tag, Content

- `pages/topic/add/add`
  - H5 source: [add.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/topic/add/add.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 圈子封面、名称、简介、分类、提交
  - Gap: must preserve `hongniangId` scene and apply-owner logic.

- `pages/topic/add/category`
  - H5 source: [category.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/topic/add/category.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 分类列表选择、确认、返回
  - Gap: ensure same category source and selected-state backfill.

- `pages/topic/admin`
  - H5 source: [admin.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/topic/admin.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 成员管理、置顶/移除/审核等管理动作、返回
  - Gap: admin mode must not degrade into read-only member list.

- `pages/topic/choose-topic/choose-topic`
  - H5 source: [choose-topic.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/topic/choose-topic/choose-topic.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 搜索、选择圈子、确认、返回
  - Gap: choose mode should return value exactly like H5.

- `pages/topic/class-list`
  - H5 source: [class-list.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/topic/class-list.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 分类切换、圈子卡片、下拉刷新、加载更多
  - Gap: keep category filter pagination identical.

- `pages/topic/detail`
  - H5 source: [detail.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/topic/detail.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 进圈、退圈、发帖、查看讨论、查看成员、圈主主页/主理人主页、举报
  - Gap: must mirror owner route, join state, post feed tabs, and moderation affordances.

- `pages/topic/info-edit`
  - H5 source: [info-edit.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/topic/info-edit.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 编辑封面/名称/简介/分类、保存
  - Gap: keep edit mode distinct from create mode.

- `pages/topic/topic-user`
  - H5 source: [topic-user.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/topic/topic-user.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 搜索成员、点用户主页、管理动作
  - Gap: pagination + member role display must align.

- `pages/discuss/add`
  - H5 source: [add.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/discuss/add.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 标题、内容、关联圈子、提交
  - Gap: same form validation and topic linkage.

- `pages/discuss/choose-discuss/choose-discuss`
  - H5 source: [choose-discuss.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/discuss/choose-discuss/choose-discuss.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 搜索、选择讨论、确认
  - Gap: same choose-mode return contract.

- `pages/discuss/detail`
  - H5 source: [detail.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/discuss/detail.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 查看正文、评论、点赞、举报、返回
  - Gap: must match thread and comment actions, not only detail text.

- `pages/post/add`
  - H5 source: [add.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/post/add.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 标题、正文、图片、视频、标签、圈子、活动、AI 草稿、付费设置、下一步/发布
  - Gap: current Flutter page already covers many fields; must verify each sheet and submit payload against H5.

- `pages/post/confirm`
  - H5 source: [confirm.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/post/confirm.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 确认发布、回编辑页
  - Gap: preserve payload echo and final submit semantics.

- `pages/post/video-detail`
  - H5 source: [video-detail.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/post/video-detail.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 播放、点赞、收藏、评论、分享、私聊、打赏、进入作者主页
  - Gap: the reward button and comment sheet are easy to drift; treat this as a separate regression page.

- `subpackages/post/detail`
  - H5 source: [detail.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/post/detail.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 图文详情、点赞、评论、收藏、分享、举报、进入用户主页、进圈子
  - Gap: detail actions must match video-detail rules by content type.

- `pages/tag/square`
  - H5 source: [square.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/tag/square.vue)
  - Flutter target: [misc_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/common/misc_pages.dart)
  - Buttons: 标签分类、标签卡片、搜索
  - Gap: same ranking/filter source.

- `pages/tag/search`
  - H5 source: [search.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/tag/search.vue)
  - Flutter target: [misc_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/common/misc_pages.dart)
  - Buttons: 搜索、选择标签、返回
  - Gap: search debounce and return contract.

- `pages/tag/detail`
  - H5 source: [detail.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/tag/detail.vue)
  - Flutter target: [misc_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/common/misc_pages.dart)
  - Buttons: 查看标签动态、进详情、返回
  - Gap: list pagination and tag summary.

- `subpackages/content/list`
  - H5 source: [list.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/content/list.vue)
  - Flutter target: [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: kind 筛选、搜索、进入内容详情、删除/管理、加载更多
  - Gap: `my_post / my_topic / my_discuss / my_collect` kinds must follow same API branching.

- `subpages/content/article/add`
  - H5 source: [add.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/content/article/add.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 标题、正文、封面、提交
  - Gap: keep long-form content payload consistent.

- `subpages/content/article/article`
  - H5 source: [article.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/content/article/article.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 浏览正文、分享、返回
  - Gap: article render and media display parity.

### D. Hongniang, Activity, Xiangqin

- `pages/hongniang/index`
  - H5 source: [index.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/hongniang/index.vue)
  - Flutter target: [hongniang_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/hongniang/hongniang_pages.dart)
  - Buttons: 主理人列表、活动列表、我的主理人视图、待审核报名、活动筛选、私聊报名用户、审核报名、发活动/去申请
  - Gap: this page is a core chain and already large in Flutter; focus on exact list segmentation and approval actions.

- `pages/hongniang/detail`
  - H5 source: [detail.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/hongniang/detail.vue)
  - Flutter target: [hongniang_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/hongniang/hongniang_pages.dart)
  - Buttons: 查看主理人介绍、关注/联系、查看活动、发起申请、进入关联圈子/相亲群
  - Gap: must mirror H5 CTA priority and fee/entry visibility.

- `pages/hongniang/apply`
  - H5 source: [apply.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/hongniang/apply.vue)
  - Flutter target: [hongniang_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/hongniang/hongniang_pages.dart)
  - Buttons: 申请资料、上传、提交、返回
  - Gap: same audit status and re-submit rules.

- `pages/hongniang/activity-detail`
  - H5 source: [activity-detail.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/hongniang/activity-detail.vue)
  - Flutter target: [hongniang_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/hongniang/hongniang_pages.dart)
  - Buttons: 报名/取消报名、支付、联系主理人、查看报名须知、审核名单、进入群聊
  - Gap: fee type, enroll state, and manager-vs-user actions must match exactly.

- `pages/hongniang/activity-publish`
  - H5 source: [activity-publish.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/hongniang/activity-publish.vue)
  - Flutter target: [hongniang_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/hongniang/hongniang_pages.dart)
  - Buttons: 标题、时间、地点、封面、费用、人数、提交
  - Gap: current Flutter page exists; verify all field names/body keys match H5 publish payload.

- `subpages/xiangqin/detail`
  - H5 source: [detail.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/xiangqin/detail.vue)
  - Flutter target: [hongniang_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/hongniang/hongniang_pages.dart)
  - Buttons: 相亲详情、进入群聊、支付、返回
  - Gap: verify routing path and parameter source from activity/detail.

- `subpages/xiangqin/group-chat`
  - H5 source: [group-chat.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/xiangqin/group-chat.vue)
  - Flutter target: [hongniang_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/hongniang/hongniang_pages.dart)
  - Buttons: 群信息、复制群号/入群方式、返回
  - Gap: confirm current Flutter carrier and make route visible in router smoke.

- `subpackages/hongniang-workbench/index`
  - H5 source: [index.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/hongniang-workbench/index.vue)
  - Flutter target: none today
  - Buttons: 工作台总览、用户池、案件、群管理
  - Gap: no raw route or page carrier yet; decide whether mobile Flutter must support it or explicitly mark out-of-scope.

- `subpackages/hongniang-workbench/users`
  - H5 source: [users.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/hongniang-workbench/users.vue)
  - Flutter target: none today
  - Buttons: 用户筛选、查看详情、推进案件
  - Gap: same as above.

- `subpackages/hongniang-workbench/cases`
  - H5 source: [cases.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/hongniang-workbench/cases.vue)
  - Flutter target: none today
  - Buttons: 案件列表、状态切换、进入详情
  - Gap: same as above.

- `subpackages/hongniang-workbench/case-detail`
  - H5 source: [case-detail.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/hongniang-workbench/case-detail.vue)
  - Flutter target: none today
  - Buttons: 案件详情、推进节点、联系双方
  - Gap: same as above.

- `subpackages/hongniang-workbench/groups`
  - H5 source: [groups.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/hongniang-workbench/groups.vue)
  - Flutter target: none today
  - Buttons: 群列表、进入群详情
  - Gap: same as above.

- `subpackages/hongniang-workbench/group-detail`
  - H5 source: [group-detail.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/hongniang-workbench/group-detail.vue)
  - Flutter target: none today
  - Buttons: 群详情、成员、群动作
  - Gap: same as above.

### E. Message, Chat, Visitor, Relations

- `subpackages/chat/session`
  - H5 source: [session.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/chat/session.vue)
  - Flutter target: [message_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/message/message_pages.dart)
  - Buttons: 会话列表、进入会话、返回、搜索
  - Gap: must sync unread/read updates with message tab badges.

- `subpackages/chat/detail`
  - H5 source: [detail.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/chat/detail.vue)
  - Flutter target: [message_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/message/message_pages.dart)
  - Buttons: 发送文本、图片、语音、社交意图消息、复制、长按动作、返回
  - Gap: chat is a full fidelity page; do not simplify message type handling.

- `subpackages/chat/assistant`
  - H5 source: [assistant.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/chat/assistant.vue)
  - Flutter target: no dedicated route today, related carrier [message_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/message/message_pages.dart)
  - Buttons: 助手总开关、代聊开关、权限配置、目标对象管理
  - Gap: add route support or explicitly fold into current social-intent / assistant hub with same entry semantics.

- `subpackages/visitor/list`
  - H5 source: [list.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/visitor/list.vue)
  - Flutter target: [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: 访客列表、查看用户主页、私聊、返回
  - Gap: same pagination and visit-time grouping.

- `subpackages/user/relations`
  - H5 source: [relations.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/user/relations.vue)
  - Flutter target: [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: tabs/type 切换、进入用户主页、关注/取关、返回
  - Gap: obey privacy settings and relation type mapping.

- `subpackages/message/list`
  - H5 source: [list.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/message/list.vue)
  - Flutter target: [message_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/message/message_pages.dart)
  - Buttons: message type list、已读/未读、进入关联内容
  - Gap: type `1/2/3` mapping must equal H5.

- `subpackages/message/social-intent`
  - H5 source: not in current `pages.json`, but Flutter already exposes route
  - Flutter target: [message_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/message/message_pages.dart)
  - Buttons: 微信号交换、礼物/转账意图处理、状态更新
  - Gap: treat as existing business page and align to chat message payload rules.

- `subpages/search/search`
  - H5 source: legacy page route via normalization
  - Flutter target: [message_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/message/message_pages.dart)
  - Buttons: 搜索会话、进入会话
  - Gap: route alias only; ensure entry points use same route.

- `subpages/im/notice-list/notice-list`
  - H5 source: [notice-list.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/im/notice-list/notice-list.vue)
  - Flutter target: [message_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/message/message_pages.dart)
  - Buttons: 通知列表、进入详情、已读
  - Gap: list read-state and badge clearing.

### F. Finance, Payment, Sign, Bill, Certification, Report

- `pages/pay/pay`
  - H5 source: [pay.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/pay/pay.vue)
  - Flutter target: [finance_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/finance/finance_pages.dart)
  - Buttons: 充值档位、支付方式、确认支付
  - Gap: payment switch and success return path parity.

- `pages/pay/mock`
  - H5 source: [mock.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/pages/pay/mock.vue)
  - Flutter target: [finance_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/finance/finance_pages.dart)
  - Buttons: 模拟支付确认
  - Gap: ensure only dev/test flow.

- `subpages/account/account`
  - H5 source: [account.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/account/account.vue)
  - Flutter target: [finance_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/finance/finance_pages.dart)
  - Buttons: 充值、提现、账单、签到、流水 tab
  - Gap: current Flutter page is close; verify every ledger tab and amount sign rule.

- `subpages/account/cash-out`
  - H5 source: [cash-out.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/account/cash-out.vue)
  - Flutter target: [finance_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/finance/finance_pages.dart)
  - Buttons: 提现金额、收款方式、提交
  - Gap: same form validation and status toast.

- `subpages/bill/bill`
  - H5 source: [bill.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/bill/bill.vue)
  - Flutter target: [finance_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/finance/finance_pages.dart)
  - Buttons: 账单类型筛选、明细项、返回
  - Gap: same list partition and pagination.

- `subpages/sign/sign`
  - H5 source: [sign.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/sign/sign.vue)
  - Flutter target: [finance_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/finance/finance_pages.dart)
  - Buttons: 签到、抽奖入口、积分入口
  - Gap: daily sign status and reward feedback must match H5.

- `subpages/sign/integral`
  - H5 source: [integral.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/sign/integral.vue)
  - Flutter target: [finance_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/finance/finance_pages.dart)
  - Buttons: 积分明细、返回
  - Gap: same ledger types and labels.

- `subpages/luck-draw/luck-draw`
  - H5 source: [luck-draw.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/luck-draw/luck-draw.vue)
  - Flutter target: [finance_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/finance/finance_pages.dart)
  - Buttons: 抽奖、查看规则、查看奖品记录
  - Gap: animation can differ, reward/result semantics cannot.

- `subpages/luck-draw/prize-record`
  - H5 source: [prize-record.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/luck-draw/prize-record.vue)
  - Flutter target: [finance_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/finance/finance_pages.dart)
  - Buttons: 奖品记录列表、返回
  - Gap: same record source and empty state.

- `subpages/auth/realname`
  - H5 source: [realname.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/auth/realname.vue)
  - Flutter target: [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: 身份信息、上传、提交
  - Gap: same certification status and upload payload.

- `subpages/auth/education`
  - H5 source: [education.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/auth/education.vue)
  - Flutter target: [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
  - Buttons: 学历信息、上传、提交
  - Gap: same certification status and upload payload.

- `subpages/report/list`
  - H5 source: [list.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/report/list.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 举报列表、查看详情
  - Gap: status and category labels must match backend.

- `subpages/report/detail`
  - H5 source: [detail.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/report/detail.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 查看举报详情、处理结果、返回
  - Gap: show same proof/media fields.

- `subpages/report/report`
  - H5 source: [report.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/report/report.vue)
  - Flutter target: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
  - Buttons: 选择举报类型、输入内容、提交
  - Gap: `bizId/cateId` derivation must match H5 routing entry.

### G. Video, Vote, Web, Misc

- `subpages/video/template`
  - H5 source: [template.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/video/template.vue)
  - Flutter target: [video_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/video/video_pages.dart)
  - Buttons: 分类筛选、模板选择、进入生成
  - Gap: current Flutter page exists; verify category and template payload mapping.

- `subpages/video/generating`
  - H5 source: [generating.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/video/generating.vue)
  - Flutter target: [video_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/video/video_pages.dart)
  - Buttons: 输入文案、开始生成、轮询、进入预览
  - Gap: same poll interval, done-state routing, and fail retry.

- `subpages/video/preview`
  - H5 source: [preview.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/video/preview.vue)
  - Flutter target: [video_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/video/video_pages.dart)
  - Buttons: 预览、删除、发布
  - Gap: same delete confirm and publish handoff.

- `subpages/video/publish`
  - H5 source: [publish.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/video/publish.vue)
  - Flutter target: [video_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/video/video_pages.dart)
  - Buttons: 标题、文案、封面、发布
  - Gap: same final publish payload and return path.

- `subpages/vote/vote`
  - H5 source: [vote.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/vote/vote.vue)
  - Flutter target: [misc_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/common/misc_pages.dart)
  - Buttons: 投票标题、选项管理、发布
  - Gap: same option count/validation.

- `subpages/content/level/level`
  - H5 source: [level.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/content/level/level.vue)
  - Flutter target: [misc_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/common/misc_pages.dart)
  - Buttons: 等级说明、成长值列表、返回
  - Gap: same summary cards and rule text source.

- `subpages/jump/jump`
  - H5 source: [jump.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpages/jump/jump.vue)
  - Flutter target: [misc_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/common/misc_pages.dart)
  - Buttons: 外链跳转确认、返回
  - Gap: external open vs in-app webview rule must match.

- `subpackages/web/webview`
  - H5 source: [webview.vue](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/src/subpackages/web/webview.vue)
  - Flutter alias target: `/pages/webview/webview`, [misc_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/common/misc_pages.dart)
  - Buttons: WebView load、返回、关闭
  - Gap: add raw route compatibility.

## Priority Order

### P0: Route Compatibility And Tab Truth

- Fix raw H5 route compatibility for auth/profile/settings/vip/web/assistant routes before new page work.
- Remove or sharply reduce `暂未找到对应的真实页面入口` on tab pages.
- Make bottom tabs and middle `+` sheet mirror H5 publish/navigation rules.

### P1: The 4 Tab Pages

- `HomeTabPage`
- `MatchTabPage`
- `MessageTabPage`
- `ProfileTabPage`

### P2: User/Profile/Content Core Pages

- Login/register/go-login/sms/email
- User home / edit profile / persona report / relations / visitors
- Topic detail / post add / post detail / discuss detail

### P3: Hongniang Chain

- Hongniang index/detail/apply/activity detail/publish
- Xiangqin detail/group-chat
- Decide workbench support scope explicitly

### P4: Finance + AI + Misc

- Pay/account/bill/sign/luck-draw/certification/report
- Video template/generating/preview/publish
- Vote/level/web/jump/tag pages

## File Map For Implementation

### Routing

- Modify: [app_routes.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/app/app_routes.dart)
- Modify: [app_router.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/app/app_router.dart)
- Modify: [app_navigation.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/core/navigation/app_navigation.dart)

### Shell And Tabs

- Modify: [main_shell.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/shell/main_shell.dart)
- Modify: [home_tab_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/home/home_tab_page.dart)
- Modify: [match_tab_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/match/match_tab_page.dart)
- Modify: [message_tab_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/message/message_tab_page.dart)
- Modify: [profile_tab_page.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/profile/profile_tab_page.dart)

### Secondary Carriers

- Modify: [content_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/content/content_pages.dart)
- Modify: [hongniang_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/hongniang/hongniang_pages.dart)
- Modify: [message_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/message/message_pages.dart)
- Modify: [user_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/user/user_pages.dart)
- Modify: [finance_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/finance/finance_pages.dart)
- Modify: [video_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/video/video_pages.dart)
- Modify: [misc_pages.dart](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/qiuou-flutter/lib/features/common/misc_pages.dart)

### Service Layer Audits

- Modify as needed: `home_service.dart`, `match_service.dart`, `message_service.dart`, `profile_service.dart`, `user_service.dart`, `content_service.dart`, `hongniang_service.dart`, `finance_service.dart`, `video_service.dart`

## Task Breakdown

### Task 1: Route Compatibility Baseline

**Files:**
- Modify: `qiuou-flutter/lib/app/app_routes.dart`
- Modify: `qiuou-flutter/lib/app/app_router.dart`
- Modify: `qiuou-flutter/lib/core/navigation/app_navigation.dart`
- Reference: `multi-platform-app/src/pages.json`

- [ ] Add raw H5 compatibility for auth/profile/settings/vip/web/assistant route names.
- [ ] Ensure raw H5 route, Flutter alias route, and menu route all resolve to the same page carrier.
- [ ] Remove cases where valid backend menu targets fall through to “guess by menu name”.
- [ ] Smoke-check all current raw-vs-alias pairs by `Navigator.pushNamed`.

### Task 2: Main Shell And Publish Sheet

**Files:**
- Modify: `qiuou-flutter/lib/features/shell/main_shell.dart`
- Reference: `multi-platform-app/src/pages/tab/home.vue`
- Reference: `multi-platform-app/src/components` publish/navigation usage

- [ ] Align bottom tab auth guards with H5.
- [ ] Align middle `+` sheet entries, order, labels, and destination parameters with H5.
- [ ] Apply filing/payment/hongniang/assistant switches to sheet entries exactly where H5 does.

### Task 3: Home Tab Parity

**Files:**
- Modify: `qiuou-flutter/lib/features/home/home_tab_page.dart`
- Modify: `qiuou-flutter/lib/features/home/home_service.dart`
- Reference: `multi-platform-app/src/pages/tab/home.vue`

- [ ] Audit and match the four top tabs.
- [ ] Match video card action set and result state updates.
- [ ] Match same-city list actions, city switch, AI break-ice flow, and fallback city behavior.
- [ ] Match love tab entry gating, activities, and main CTA rules.
- [ ] Match topic category switching, topic highlight entry, and post feed behavior.

### Task 4: Match Tab Parity

**Files:**
- Modify: `qiuou-flutter/lib/features/match/match_tab_page.dart`
- Modify: `qiuou-flutter/lib/features/match/match_service.dart`
- Reference: `multi-platform-app/src/pages/tab/match.vue`

- [ ] Match `推荐 / 热榜` tab semantics.
- [ ] Match quick-entry and feature-link target resolution by type.
- [ ] Replace placeholder route failure behavior with real parity paths or explicit unsupported handling only where H5 also blocks.
- [ ] Match latest post pagination and hot rank presentation/entry.

### Task 5: Message Tab And Chat Parity

**Files:**
- Modify: `qiuou-flutter/lib/features/message/message_tab_page.dart`
- Modify: `qiuou-flutter/lib/features/message/message_pages.dart`
- Modify: `qiuou-flutter/lib/features/message/message_service.dart`
- Reference: `multi-platform-app/src/pages/tab/message.vue`
- Reference: `multi-platform-app/src/subpackages/chat/*.vue`

- [ ] Match top-right search, mark-all-read, and settings behavior.
- [ ] Match every message hub tab and badge source.
- [ ] Match friend deletion, session open, visitor/fans open-user logic.
- [ ] Match chat detail message type handling including text/image/voice/social-intent.
- [ ] Match assistant entry and filing switch behavior.

### Task 6: Profile Tab And User Pages

**Files:**
- Modify: `qiuou-flutter/lib/features/profile/profile_tab_page.dart`
- Modify: `qiuou-flutter/lib/features/profile/profile_service.dart`
- Modify: `qiuou-flutter/lib/features/user/user_pages.dart`
- Modify: `qiuou-flutter/lib/features/common/misc_pages.dart`
- Reference: `multi-platform-app/src/pages/tab/profile.vue`
- Reference: `multi-platform-app/src/subpackages/profile/*.vue`
- Reference: `multi-platform-app/src/pages/user/edit-info/*.vue`

- [ ] Match header card, modules, stats, service grid, and login entry behavior.
- [ ] Stop relying on menu-name inference when backend menu URL exists.
- [ ] Expand user home to match H5 personal home interaction depth.
- [ ] Match persona report actions, visibility toggle, and paid switch rules.
- [ ] Match edit-info field list, setting toggles, submit page behavior, and logout/cancel flow.

### Task 7: Topic, Post, Discuss, Tag, Content Parity

**Files:**
- Modify: `qiuou-flutter/lib/features/content/content_pages.dart`
- Modify: `qiuou-flutter/lib/features/common/misc_pages.dart`
- Reference: `multi-platform-app/src/pages/topic/*.vue`
- Reference: `multi-platform-app/src/pages/discuss/*.vue`
- Reference: `multi-platform-app/src/pages/post/*.vue`
- Reference: `multi-platform-app/src/pages/tag/*.vue`
- Reference: `multi-platform-app/src/subpackages/post/detail.vue`
- Reference: `multi-platform-app/src/subpackages/content/list.vue`
- Reference: `multi-platform-app/src/subpages/content/article/*.vue`

- [ ] Match topic create/edit/admin/detail/member pages.
- [ ] Match discuss choose/add/detail pages.
- [ ] Match post add/confirm/detail/video detail actions.
- [ ] Match tag square/search/detail.
- [ ] Match user content list and article compose/detail.

### Task 8: Hongniang And Xiangqin Parity

**Files:**
- Modify: `qiuou-flutter/lib/features/hongniang/hongniang_pages.dart`
- Modify: `qiuou-flutter/lib/features/hongniang/hongniang_service.dart`
- Reference: `multi-platform-app/src/pages/hongniang/*.vue`
- Reference: `multi-platform-app/src/subpages/xiangqin/*.vue`
- Reference: `multi-platform-app/src/subpackages/hongniang-workbench/*.vue`

- [ ] Match hongniang index/detail/apply/activity detail/activity publish.
- [ ] Match activity enroll, audit, payment, and chat branches.
- [ ] Match xiangqin detail/group chat.
- [ ] Decide and document whether hongniang workbench belongs in mobile Flutter scope; if yes, add real carrier pages, if no, explicitly block with product decision.

### Task 9: Finance, Report, Video, Misc Parity

**Files:**
- Modify: `qiuou-flutter/lib/features/finance/finance_pages.dart`
- Modify: `qiuou-flutter/lib/features/video/video_pages.dart`
- Modify: `qiuou-flutter/lib/features/common/misc_pages.dart`
- Modify: `qiuou-flutter/lib/features/content/content_pages.dart`
- Reference: `multi-platform-app/src/subpages/**`

- [ ] Match pay/account/cash-out/bill/sign/integral/luck-draw/prize-record.
- [ ] Match realname/education/report list/detail/submit.
- [ ] Match video template/generating/preview/publish.
- [ ] Match vote/level/jump/webview.

### Task 10: Regression And Acceptance

**Files:**
- Add or update smoke scripts under `qiuou-flutter` if needed
- Reference: `docs/release-smoke-checklist.md`

- [ ] Build a route smoke checklist covering every page in the matrix.
- [ ] Verify all tab-page primary buttons and at least one secondary button each.
- [ ] Verify badge sync, login redirect return, payment gating, filing gating, and assistant gating.
- [ ] Verify no page was “made pretty” by dropping H5 behaviors.

## Acceptance Checklist

- All 4 tabs behave according to H5 button contract.
- Raw H5 route names and Flutter alias route names both resolve correctly where parity is required.
- No placeholder snackbar remains on first-tier user paths.
- Backend `userMenu/list` targets are respected before heuristics.
- Filing switches, payment switches, persona switches, and assistant switches behave exactly like H5.
- Flutter changes do not change H5 business order, CTA priority, or route semantics.

Plan complete and saved to `docs/superpowers/plans/2026-04-05-flutter-h5-parity-roadmap.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
