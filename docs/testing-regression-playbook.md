# AiLeMe 测试数据与回归手册

## 1. 目标

这份手册用于固定一套可重复执行的流程：

1. 先准备数据
2. 再启动服务
3. 再跑页面回归
4. 最后做发布 smoke

这样下次开新窗口，不需要重新回忆“哪个脚本造什么数据、先跑哪个、哪个是验登录、哪个是验活动和群聊”。

## 2. 前置环境

默认本地依赖：

- MySQL 已通过 Docker 启动
- Redis 已通过 Docker 启动
- Java 后端运行在 `http://localhost:8080`
- H5 运行在 `http://localhost:5173`

常见目录：

- 仓库根目录：`/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme`
- H5 脚本目录：`multi-platform-app/scripts`
- Python 数据脚本目录：`scripts`

## 3. 测试数据脚本

### 3.1 社区 / 红娘 / 话题脚手架

文件：`scripts/seed_community_scaffold.py`

作用：

- 为 `hongniang_info`、`topic`、`user_topic` 补齐基础测试数据
- 根据现有用户资料生成更自然的红娘和话题骨架
- 目标是幂等补数据，不是一次性脏插入

典型用法：

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme
python3 scripts/seed_community_scaffold.py
```

可调环境变量：

- `YUELAO_DB_HOST`
- `YUELAO_DB_PORT`
- `YUELAO_DB_USER`
- `YUELAO_DB_PASSWORD`
- `YUELAO_DB_NAME`
- `TARGET_HONGNIANG_TOTAL`
- `TARGET_USER_TOPIC_TOTAL`

### 3.2 补全老用户资料

文件：`scripts/fill_legacy_users_profiles.py`

作用：

- 给历史测试用户补齐昵称、城市、职业、教育、兴趣、自我介绍、爱情宣言等资料
- 用于把“空壳用户”修成更真实的社交测试样本

典型用法：

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme
python3 scripts/fill_legacy_users_profiles.py --execute
```

### 3.3 头像替换与脏头像清理

文件：

- `scripts/apply_baidu_avatar_mix.py`
- `scripts/clean_dirty_avatars.py`

作用：

- 清理 `randomuser`、`pravatar`、测试 COS、`picsum` 等不合适头像
- 让测试数据看起来更接近真实国内社交产品样本
- 会同时处理 `avatar`、`figur`、`info.mohuAvatar`

注意：

- 这是历史测试数据整理工具，不是最终的线上生图方案
- 执行前最好先 dry-run，再决定是否 `--execute`

### 3.4 H5 回归内容数据刷新

文件：`multi-platform-app/scripts/refresh-regression-seed-data.js`

作用：

- 给活动、帖子、内容流刷新一批更自然的回归用文案和图片
- 依赖 `multi-platform-app/scripts/lib/realistic-seed-data.js`
- 主要更新活动标题、描述、帖子标题、内容、媒体数组

典型用法：

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app
node scripts/refresh-regression-seed-data.js
```

可调环境变量：

- `REGRESSION_SEED_MYSQL_CONTAINER`
- `REGRESSION_SEED_MYSQL_DB`
- `REGRESSION_SEED_MYSQL_USER`
- `REGRESSION_SEED_MYSQL_PASSWORD`

## 4. 回归脚本矩阵

### 4.1 登录与认证

- `multi-platform-app/scripts/smoke-auth-code-flow.js`

用途：

- 发送注册验证码
- 注册
- 发送登录验证码
- 短信登录
- 拉用户信息
- 可选再测一次 Agent 润色接口

常用命令：

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app
npm run test:smoke:auth
npm run test:smoke:auth-agent
```

常用环境变量：

- `SMOKE_BASE_URL`
- `SMOKE_MOBILE`
- `SMOKE_WITH_AGENT`
- `SMOKE_AGENT_STYLE`
- `SMOKE_RATE_LIMIT_RETRY`

### 4.2 后台配置 / Quartz / 三方

- `admin-quartz-browser-check.js`
- `admin-thirdparty-browser-check.js`

用途：

- 回归管理后台 Quartz 页面
- 回归三方配置页面

### 4.3 首页 / 广场 / 视频 / 推荐流

- `home-tab-unified-check.js`
- `home-video-browser-check.js`
- `match-browser-check.js`
- `ui-compact-pages-check.js`
- `video-generate-browser-check.js`

用途：

- 看首页 tab 是否可用
- 看视频流和抖音式页面是否正常
- 看广场 / 匹配页主要卡片是否正常

### 4.4 消息 / 聊天 / 群聊

- `message-browser-check.js`
- `message-all-tabs-browser-check.js`
- `message-real-flow-regression.js`
- `activity-group-chat-browser-check.js`

用途：

- 检查消息页各 tab
- 检查真实消息流
- 检查活动群聊链路

### 4.5 红娘 / 活动 / 支付

- `hongniang-activity-flow-regression.js`
- `hongniang-list-pay-browser-check.js`
- `hongniang-topic-strip-browser-check.js`
- `account-browser-check.js`
- `vip-profile-browser-check.js`

用途：

- 检查红娘列表、活动流、支付相关 UI
- 检查账户与 VIP 相关页面

### 4.6 发帖 / 话题 / 可见性

- `post-add-browser-check.js`
- `post-public-visibility-e2e.js`
- `publish-visibility-e2e.js`
- `topic-add-browser-check.js`
- `topic-display-browser-check.js`
- `topic-role-browser-check.js`
- `choose-topic-browser-check.js`

用途：

- 检查发帖、话题创建、可见性控制、圈子展示等链路

## 5. 推荐执行顺序

### 日常功能开发回归

1. 启动 MySQL / Redis
2. 启动 Java 后端
3. 启动 H5
4. 如有需要，先跑数据脚本
5. 跑功能对应的 browser check
6. 最后补一轮认证 smoke

### 涉及活动 / 红娘 / 内容流的改动

1. `python3 scripts/seed_community_scaffold.py`
2. `node multi-platform-app/scripts/refresh-regression-seed-data.js`
3. 跑 `hongniang-*`、`message-*`、`post-*`、`topic-*` 相关脚本

### 涉及登录 / Agent / 配置的改动

1. `npm run test:smoke:auth`
2. `npm run test:smoke:auth-agent`
3. `admin-thirdparty-browser-check.js`
4. 如涉及定时任务，再跑 `admin-quartz-browser-check.js`

## 6. 发布前最小检查集

优先参考：`docs/release-smoke-checklist.md`

最小建议集：

1. H5 构建通过
2. 管理后台构建通过
3. Java 打包或启动通过
4. 三方配置页面可读
5. 验证码登录正常
6. 首页、消息、活动、支付、AI 内容入口能打开
7. 订单/账单/回调相关页面能查看

## 7. 结果物与排障

- `multi-platform-app/test-results/` 会落一部分截图和 JSON 结果
- 遇到 UI 问题先看截图
- 遇到接口问题先看 Java 日志和浏览器脚本报错
- 遇到数据不对先回看是否少跑了造数脚本

## 8. 经验规则

1. 先造“像样”的数据，再看页面，否则很多社交页面会误判成 UI 问题。
2. 认证 smoke 是所有回归的最前置检查，不通就不要继续怀疑页面。
3. 涉及支付、礼物、打赏、活动报名的改动，回归时不要只看页面文案，一定要连账单/订单/后台列表一起看。
4. 涉及 AI 路由的改动，必须顺手检查管理后台三方配置页是否还能读写。
