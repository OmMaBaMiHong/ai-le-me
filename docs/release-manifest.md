# 爱了么正式发布清单

## 本次发布范围

- `multi-platform-app` H5
- `multi-platform-app` 微信小程序
- `ai-le-me-admin` Java 单体服务
- `ai-le-me-ui` 管理后台

以下目录不纳入本次正式上线链：

- `qiuou-flutter`
- `qiuou-ux`
- `ai-le-me-agent-runtime`

## 产物路径

- H5: `multi-platform-app/dist/build/h5`
- 微信小程序: `multi-platform-app/dist/build/mp-weixin`
- Java 单体: `ai-le-me-admin/target/ai-le-me-admin.jar`
- 管理后台: `ai-le-me-ui/dist`

## 构建命令

### 用户端

```bash
cd multi-platform-app
npm run build:h5
npm run build:mp-weixin
```

### 管理后台

```bash
cd ai-le-me-ui
npm run build:prod
```

### Java 单体

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme
mvn -pl ai-le-me-admin -am -DskipTests package
```

## 启动命令

### Java 单体

```bash
java -jar ai-le-me-admin/target/ai-le-me-admin.jar
```

### H5 本地联调

```bash
cd multi-platform-app
npm run dev:h5
```

### 管理后台本地联调

```bash
cd ai-le-me-ui
npm run dev
```

## 依赖服务

- MySQL: 业务主库统一为 `bang_yi`
- Redis
- 对象存储服务
- 短信服务
- 微信开放平台 / 小程序 / 公众号
- 微信支付 / 支付宝
- 火山引擎 Ark AI 服务

## 配置边界

- 部署层: `application*.yml` 与环境变量
- 业务层: `sys_config`
- 三方运行层: `sys_third_party_provider` 与 `sys_third_party_route_rule`

要求：

- 渠道密钥不得继续落在 `sys_config`
- OSS、微信、支付、短信、AI 模型统一走三方配置
- Quartz 是唯一保留的任务体系

## 回滚路径

### 应用回滚

- Java 单体回滚到上一个稳定 jar 包
- H5 与管理后台回滚到上一个静态资源目录
- 小程序回滚到上一个已上传审核版本

### 数据回滚

- 配置迁移脚本单独归档，执行前先备份 `sys_config`、`sys_third_party_provider`、`sys_third_party_route_rule`
- 清理脚本必须先在测试库验证，再进入生产

## 发布顺序

1. 导入数据库初始化与迁移脚本
2. 校验三方配置和路由规则
3. 部署 Java 单体
4. 部署管理后台
5. 发布 H5 静态资源
6. 上传并验证微信小程序版本
7. 执行 smoke checklist
