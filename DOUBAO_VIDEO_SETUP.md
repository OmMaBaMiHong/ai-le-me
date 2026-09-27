# 豆包视频服务配置指南

## 功能说明

已为第三方服务配置系统添加了**新增服务商配置**功能,现在可以通过UI界面轻松添加新的视频生成服务提供商,包括豆包(Doubao)视频服务。

## 功能特点

### 1. 新增服务商入口
- 在"AI视频服务"标签页中,新增了"新增服务商"按钮
- 支持快速添加新的视频服务提供商
- 自动创建默认配置项

### 2. 配置管理功能
- **提供商信息配置**: 代码、名称、官网、文档地址等
- **API配置管理**: 灵活添加/删除配置项
- **配置类型支持**: 文本、密码、数字、选择、JSON等多种类型
- **敏感信息保护**: 支持标记敏感配置项(如API Key)
- **表单验证**: 完整的输入验证机制

### 3. 默认配置项
新增服务商时会自动创建以下默认配置:
- API Endpoint (API服务地址)
- API Key (API密钥)
- 默认模型 (视频生成模型)
- 请求超时 (超时时间配置)

## 快速开始

### 方式一: 通过UI界面添加(推荐)

1. **访问配置页面**
   - 导航至: 系统管理 → 第三方服务配置
   - 切换到"AI视频服务"标签页

2. **点击"新增服务商"按钮**
   - 填写基本信息:
     - 提供商代码: `doubao` (小写字母和下划线)
     - 显示名称: `豆包视频`
     - 官方网站: `https://console.volcengine.com/ark`
     - 文档地址: `https://www.volcengine.com/docs/82379/1298454`
     - 描述: `字节跳动豆包AI视频生成服务 - SeeDance`

3. **配置API参数**
   
   系统会自动创建默认配置项,你也可以添加更多配置:

   **基础配置:**
   - API Endpoint: `https://ark.cn-beijing.volces.com/api/v3`
   - API Key: `你的火山引擎API密钥`
   - 默认模型: `doubao-seedance-1-5-pro-251215`

   **视频参数:**
   - 点击"添加配置"按钮可添加更多配置项:
     - 默认时长: 5秒 / 10秒
     - 宽高比: 16:9 / 9:16 / 1:1
     - 固定相机: true / false
     - 添加水印: true / false

4. **保存配置**
   - 点击"确认新增"按钮
   - 系统会自动验证并保存配置
   - 新服务商会出现在服务商列表中

### 方式二: 通过SQL脚本导入

如果你更喜欢使用SQL脚本,可以直接执行初始化脚本:

```bash
# 执行SQL脚本
mysql -u your_user -p your_database < doubao_video_config.sql
```

该脚本包含:
- 豆包视频服务提供商信息
- 16个完整的配置项
- API调用示例和说明

## 豆包视频API配置详情

### 获取API Key

1. 访问火山引擎控制台: https://console.volcengine.com/ark
2. 进入"API密钥管理": https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey
3. 创建新的API密钥
4. 复制密钥并保存(只显示一次)

### 模型说明

- **doubao-seedance-1-5-pro-251215**: 最新版本,支持图生视频和文生视频
- **doubao-seedance-1-0**: 稳定版本,基础视频生成

### API调用示例

#### 图生视频请求

```bash
curl -X POST https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ARK_API_KEY" \
  -d '{
    "model": "doubao-seedance-1-5-pro-251215",
    "content": [
      {
        "type": "text",
        "text": "无人机以极快速度穿越复杂障碍或自然奇观，带来沉浸式飞行体验 --duration 5 --camerafixed false --watermark true"
      },
      {
        "type": "image_url",
        "image_url": {
          "url": "https://example.com/image.png"
        }
      }
    ]
  }'
```

#### 提示词参数说明

在文本提示词中可以使用以下参数:

- `--duration 5`: 视频时长(5或10秒)
- `--camerafixed false`: 是否固定镜头(true/false)
- `--watermark true`: 是否添加水印(true/false)

## 配置项说明

### API基础配置
| 配置键 | 说明 | 默认值 | 必填 |
|-------|------|-------|------|
| endpoint | API服务地址 | https://ark.cn-beijing.volces.com/api/v3 | 是 |
| api_key | API密钥 | - | 是 |
| model | 默认模型 | doubao-seedance-1-5-pro-251215 | 是 |
| timeout | 请求超时(秒) | 30 | 否 |

### 视频参数配置
| 配置键 | 说明 | 默认值 | 可选值 |
|-------|------|-------|-------|
| duration | 默认时长(秒) | 5 | 5, 10 |
| aspect_ratio | 默认宽高比 | 16:9 | 16:9, 9:16, 1:1 |
| camera_fixed | 固定相机 | false | true, false |
| watermark | 添加水印 | true | true, false |

### 费用配置
| 配置键 | 说明 | 默认值 |
|-------|------|-------|
| cost_per_request | 单次费用(积分) | 100 |
| vip_free_quota | VIP免费次数/月 | 5 |
| cost_strategy | 费用策略 | per_request |

### 高级配置
| 配置键 | 说明 | 默认值 |
|-------|------|-------|
| retry_count | 重试次数 | 3 |
| retry_interval | 重试间隔(秒) | 5 |
| webhook | Webhook回调地址 | - |
| concurrency | 并发限制 | 3 |
| extra_params | 额外参数(JSON) | - |

## 技术实现

### 后端更新

1. **Service层接口扩展** (`ISysThirdPartyService.java`)
   - 新增 `addProvider()` 方法: 新增服务提供商
   - 新增 `addConfigBatch()` 方法: 批量新增配置项

2. **Service层实现** (`SysThirdPartyServiceImpl.java`)
   - 实现提供商唯一性验证
   - 自动设置显示顺序
   - 批量配置项插入和验证

3. **Controller层扩展** (`SysThirdPartyController.java`)
   - `POST /system/thirdparty/providers/{serviceType}`: 新增提供商
   - `POST /system/thirdparty/providers/{serviceType}/with-configs`: 新增提供商并批量创建配置

### 前端更新

1. **API接口** (`thirdparty.ts`)
   - `addProvider()`: 新增服务提供商
   - `addProviderWithConfigs()`: 新增提供商并批量创建配置

2. **UI组件** (`VideoConfigPanel.vue`)
   - 新增"新增服务商"按钮
   - 完整的新增服务商对话框
   - 动态配置项管理(添加/删除)
   - 表单验证和错误提示
   - 配置类型智能选择

## 注意事项

1. **提供商代码规范**
   - 只能使用小写字母和下划线
   - 必须唯一,不能与现有提供商重复

2. **配置键命名**
   - 使用下划线分隔,如: `api_key`, `retry_count`
   - 保持命名一致性和可读性

3. **敏感信息保护**
   - API Key等敏感配置项会被自动脱敏显示
   - 只有具有特定权限的用户才能查看完整值

4. **选择类型配置**
   - 选择类型的配置项需要提供 `selectOptions`
   - 格式为JSON数组,如: `["option1","option2"]`

## 常见问题

### 1. 新增提供商后看不到?
- 确保刷新页面或点击刷新按钮
- 检查是否保存成功(查看通知消息)

### 2. 配置项验证失败?
- 检查必填字段是否都已填写
- 提供商代码是否符合命名规范
- 配置键是否使用了正确的格式

### 3. 如何测试API连接?
- 保存配置后,点击"测试连接"按钮
- 系统会使用当前配置测试API可用性
- 测试失败会显示详细错误信息

### 4. 如何切换服务提供商?
- 点击要切换的服务商卡片
- 系统会自动切换并加载对应配置
- 切换后当前配置会保存到数据库

## 扩展使用

### 添加其他视频服务商

按照相同的方式,你可以添加其他视频生成服务:

1. **Runway Gen-3**
   - API文档: https://runwayml.com/docs
   
2. **Pika Labs**
   - API文档: https://pika.art/docs

3. **Stable Video Diffusion**
   - API文档: https://stability.ai/docs

每个服务商都可以自定义配置项,满足不同的业务需求。

## 相关文件

- SQL初始化脚本: `doubao_video_config.sql`
- 后端Service接口: `ai-le-me-modules/ai-le-me-system/src/main/java/org/dromara/system/service/ISysThirdPartyService.java`
- 后端Service实现: `ai-le-me-modules/ai-le-me-system/src/main/java/org/dromara/system/service/impl/SysThirdPartyServiceImpl.java`
- 后端Controller: `ai-le-me-modules/ai-le-me-system/src/main/java/org/dromara/system/controller/system/SysThirdPartyController.java`
- 前端API: `ai-le-me-ui/src/api/system/thirdparty.ts`
- 前端组件: `ai-le-me-ui/src/views/system/thirdparty/components/VideoConfigPanel.vue`

## 支持

如有问题,请参考:
- 火山引擎文档: https://www.volcengine.com/docs/82379/1298454
- 项目文档: [第三方服务配置实现计划](third_party_config_implementation_plan.md)
