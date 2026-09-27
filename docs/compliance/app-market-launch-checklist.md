# 爱了么应用市场上架前清单

## 目标

本清单面向后续在华为应用市场、小米应用商店等安卓渠道上架前的准备工作。

## 先说结论

软著很重要，但 `软著不是唯一前置项`。对应用市场首发来说，通常至少还要把下面几类事项准备好：

1. 企业主体与开发者账号
2. APP 备案
3. 隐私政策与用户协议
4. 账号注销能力
5. 审核测试账号
6. 应用介绍、截图、图标、包信息
7. 内容审核与投诉联系方式

## 当前项目建议使用的对外身份

- 应用名称：`爱了么`
- 软件资质名称：`爱了么AI社交平台软件 V1.0`
- 开发者主体：`煊光（杭州）智能科技有限公司`

如果最终品牌不叫 `爱了么`，请在软著、应用包名展示、协议标题、商店标题里一次性改齐。

## 一、基础资质

### 必备

- 企业营业执照
- 法人/管理员实名认证
- 开发者平台企业账号
- 联系电话、联系邮箱
- 应用 Logo、启动图、介绍文案、截图

### 强烈建议尽快补齐

- 软著证书或受理回执
- APP 备案号
- ICP 备案与域名主体一致性检查

## 二、合规能力

### 1. 隐私政策

必须具备：

- 明确的隐私政策正文
- 在登录前可访问
- 与实际收集的权限、数据、用途一致
- 包含开发者主体名称、联系方式、用户权利说明

### 2. 用户协议

建议具备：

- 服务范围
- 会员/充值/支付规则
- 违规内容处理规则
- 未成年人说明

### 3. 账号注销

应用市场通常会重点看：

- App 内是否可发起账号注销
- 注销路径是否清晰
- 注销后是否有结果反馈

### 4. 审核账号

如果应用登录后才能看核心功能，建议预先准备：

- 手机号登录测试账号
- 审核验证码接收方式
- 或固定审核环境说明

## 三、和你这个产品强相关的额外审核点

爱了么不是简单工具类应用，而是带有 `社交 + 内容 + 聊天 + 活动 + 会员支付 + AI 生成` 的产品，审核时通常会比普通工具更细。

重点关注：

1. 聊天、群聊、圈子、帖子、视频这类 UGC 是否有内容治理规则
2. 红娘、活动、报名、支付链路是否有清晰说明
3. AI 画像、AI 内容生成是否有提示语和结果边界
4. 涉及相识交友场景时，是否有投诉反馈入口和平台联系方式

## 四、华为/小米上架前执行清单

### 第 1 步：统一应用主名称

把下列位置统一成一个最终名称：

- `multi-platform-app/src/manifest.json`
- `multi-platform-app/src/shared/config/env.js`
- `multi-platform-app/src/utils/config.js`
- 发布文档中的应用名称
- 隐私政策和用户协议标题

### 第 2 步：补全协议与注销链路

至少确认以下页面或能力真实可用：

- 隐私政策
- 用户协议
- 账号注销入口
- 联系客服/投诉入口

### 第 3 步：准备审核物料

- 应用一句话简介
- 应用详细介绍
- Logo
- 启动图
- 首页、登录、资料、AI画像、圈子、活动、聊天、会员等截图
- 测试账号

### 第 4 步：准备技术与包信息

- Android 包名
- `versionName`
- `versionCode`
- 签名证书
- 渠道包
- 隐私权限说明

### 第 5 步：做首轮冒烟

建议至少验证：

- 登录注册
- 首页与广场
- AI画像报告
- 红娘专区
- 活动详情与报名
- 私聊/群聊
- 会员中心
- 支付结果回显
- 协议页可打开

## 五、仓库里已经能直接利用的现有基础

- 发布清单：[docs/release-manifest.md](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/docs/release-manifest.md)
- 上线冒烟清单：[docs/release-smoke-checklist.md](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/docs/release-smoke-checklist.md)
- 多端打包脚本：[multi-platform-app/scripts/pack-multi-release.js](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/scripts/pack-multi-release.js)

## 六、当前已识别的上线阻塞项

### 阻塞项 1：产品名不一致

当前仓库内同时出现 `爱了么`、`爱爱爱`、`AiAi` 等多个外显名称，这会直接影响：

- 软著名称
- 商店名称
- 应用内展示名称
- 协议正文主体
- 截图页标题

### 阻塞项 2：协议页依赖后端内容

当前协议页通过接口读取：

- `system/protocol`
- `system/privacy`

如果正式环境没配好内容，商店审核时会直接看到空白协议页或加载失败。

### 阻塞项 3：账号注销能力还没有明确落点

我在当前检查范围内还没有看到一条明确的“用户主动注销账号”产品链路。这个项建议作为上架前必补。

## 七、建议你先办的顺序

1. 先定最终应用名，推荐直接用 `爱了么`
2. 用 [qiuou-soft-copyright-package.md](/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/docs/compliance/qiuou-soft-copyright-package.md) 开始准备软著
3. 同时办理 APP 备案，不要等软著下来再开始
4. 补齐隐私政策、用户协议、账号注销、投诉反馈
5. 做安卓首发包和审核账号
6. 先投华为、小米，再扩其他安卓渠道

## 八、官方要求核对入口

以下是我核对这次清单时参考的官方入口，后续正式提交前再复核一次：

- 工信部 APP 备案服务说明：[gov.cn 相关通知](https://www.gov.cn/lianbo/bumen/202308/content_6899379.htm)
- 华为开发者联盟应用发布入口：[创建和发布应用](https://developer.huawei.com/consumer/cn/doc/app/agc-help-add-app-0000001161085160)
- 华为应用审核相关文档检索入口：[华为开发者文档搜索](https://developer.huawei.com/consumer/cn/search/site?query=%E5%BA%94%E7%94%A8%20%E5%AE%A1%E6%A0%B8)
- 小米应用商店审核规范：[小米应用审核标准](https://dev.mi.com/docs/appsmarket/auditing%26evaluation/auditing_criterion/)
- 小米隐私与权限检测服务：[小米隐私与权限服务](https://dev.mi.com/docs/appsmarket/detection%26governance/privacy_service/)

## 九、我对这份清单的默认假设

1. 你准备以企业主体上架，而不是个人主体
2. 首发以安卓渠道为主
3. 当前主发版端是 `multi-platform-app`
4. 软著先按 app 客户端申报，不和后台一起合并申报
