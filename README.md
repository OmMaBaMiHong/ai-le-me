# 爱了么 ai-le-me（ai-le-me-Plus）

基于 [RuoYi-Vue-Plus](https://github.com/dromara/RuoYi-Vue-Plus) 二开的开源婚恋 / 智能体平台。

> 项目原名 `yuelao`，已重命名为 **ai-le-me-Plus**。所有模块统一以 `ai-le-me-` 前缀命名，Java 包名统一为 `org.aileme`（社交子模块为 `org.aileme.shejiao`），Python 智能体运行时包名为 `aileme_agent_runtime`。

## 模块结构

| 模块 | 说明 |
| --- | --- |
| `ai-le-me-admin` | 管理后台（Spring Boot 后端，RuoYi 风格） |
| `ai-le-me-common` | 公共模块（加密、MyBatis、Sa-Token 等） |
| `ai-le-me-system` | 系统模块（用户、角色、租户、第三方配置等） |
| `ai-le-me-ai-gateway` | AI 网关模块 |
| `ai-le-me-shejiao-app` | 社交 / 红娘 App 后端（包名 `org.aileme.shejiao`） |
| `ai-le-me-ui` | 前端（Vue 3 + Element Plus） |
| `ai-le-me-agent-runtime` | Python 智能体运行时（`aileme_agent_runtime`） |

## 快速开始

### 后端

1. 准备 MySQL / Redis（版本见 `pom.xml`）。
2. 复制配置模板并填入你自己的凭证：
   ```bash
   cp ai-le-me-admin/src/main/resources/application.yml.example \
      ai-le-me-admin/src/main/resources/application.yml
   # 同理创建 application-dev.yml / application-prod.yml，并替换为你的真实数据库、Redis、RSA 密钥等
   ```
   > ⚠️ `application.yml` / `application-dev.yml` / `application-prod.yml` **不纳入版本库**，请勿提交真实密钥、私钥与密码。
   > 生成你自己的 RSA 密钥对替换 `application.yml` 中的 `privateKey` / `publicKey` 与 `jwt-secret-key`。
3. 执行 `sql/` 下的初始化 SQL（数据库 schema 与配置项默认值）。
4. 构建运行：
   ```bash
   mvn clean package -DskipTests
   # 或 IDE 直接运行 AiLeMeApplication
   ```

### 前端

```bash
cd ai-le-me-ui
npm install
npm run dev
```

### 智能体运行时

```bash
cd ai-le-me-agent-runtime
python -m venv .venv && source .venv/bin/activate
pip install -e .
cp .env.example .env   # 填入你的 API Key
```

## 安全说明

本仓库**不包含任何真实密钥、私钥、密码或第三方凭证**：
- `application*.yml`、`*.env` 等含凭证文件已被 `.gitignore` 排除；
- 数据库初始化 SQL 中的密钥字段均为空默认值，需你自行在配置中心填写；
- 发布前已对全部源码做密钥扫描。

请勿将含有真实凭证的文件提交到本仓库。

## 许可证

[商业授权 / 保留所有权利（使用需付费）](LICENSE)
