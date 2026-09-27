# AI人物画像功能使用说明

## 一、功能架构

### 1. AI模型策略(可切换)

支持3种AI提供商：
- **mock**: 测试模式，返回固定数据
- **tongyi**: 阿里通义千问(推荐)
- **zhipu**: 智谱GLM

### 2. 配置方式

在`application.yml`或`application.properties`中配置：

```yaml
# AI提供商选择
ai:
  persona:
    provider: mock  # mock / tongyi / zhipu
  
  # 通义千问配置
  tongyi:
    api-key: your-api-key
    model: qwen-turbo
  
  # 智谱GLM配置
  zhipu:
    api-key: your-api-key
    model: glm-4

# 费用配置
persona:
  generate:
    cost:
      integral: 100  # 生成画像费用(积分)
      vip-free-monthly: 3  # VIP每月免费次数
  view:
    cost:
      integral: 50  # 查看他人画像费用(积分)
```

## 二、API接口

### 1. 生成画像 `POST /app/persona/generate`

**请求参数**:
```json
{
  "source": "manual_generate",  // 可选: manual_generate, auto_update
  "forcePay": false  // 是否强制付费(第二次调用时传true)
}
```

**响应(需要付费时)**:
```json
{
  "code": 0,
  "data": {
    "needPay": true,
    "cost": 100,
    "vipRemainingQuota": 0,
    "message": "VIP免费配额已用完，生成需消耗100积分"
  }
}
```

**响应(生成成功)**:
```json
{
  "code": 0,
  "data": {
    "success": true,
    "snapshotId": 123,
    "imageUrl": "https://xxx/persona_123.png",
    "summary": "一个真诚且有趣的人",
    "generatedAt": "2026-02-14T18:00:00",
    "paidIntegral": 0  // 本次消耗的积分
  }
}
```

### 2. 查看我的画像 `GET /app/persona/my`

**响应**:
```json
{
  "code": 0,
  "data": {
    "hasPersona": true,
    "snapshotId": 123,
    "imageUrl": "https://xxx/persona_123.png",
    "summary": "一个真诚且有趣的人",
    "generatedAt": "2026-02-14T18:00:00",
    "versionNo": 1,
    "vipRemainingQuota": 2,  // VIP剩余免费次数
    "generateCost": 100  // 生成费用
  }
}
```

### 3. 查看他人画像 `GET /app/persona/view?targetUserId=xxx`

**响应(已解锁)**:
```json
{
  "code": 0,
  "data": {
    "hasPersona": true,
    "unlocked": true,
    "snapshotId": 456,
    "summary": "慢热但认真的人",
    "imageUrl": "https://xxx/persona_456.png"
  }
}
```

**响应(未解锁)**:
```json
{
  "code": 0,
  "data": {
    "hasPersona": true,
    "unlocked": false,
    "snapshotId": 456,
    "summary": "慢热但认真的人",
    "blurImageUrl": "https://xxx/persona_456_blur.png",
    "unlockCost": 50
  }
}
```

### 4. 解锁他人画像 `POST /app/persona/unlock`

**请求参数**:
```json
{
  "targetUserId": 789,
  "payType": "integral"  // integral(积分) 或 balance(余额)
}
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "unlocked": true,
    "imageUrl": "https://xxx/persona_789.png",
    "recordId": 101,
    "paidIntegral": 50
  }
}
```

### 5. 删除我的画像 `DELETE /app/persona/delete`

**响应**:
```json
{
  "code": 0,
  "message": "删除成功"
}
```

## 三、业务规则

### 1. 生成画像

- **VIP用户**: 每月前N次免费(配置`vip-free-monthly`)
- **非VIP**: 每次消耗积分(配置`generate.cost.integral`)
- 首次调用时，如果需要付费，会返回`needPay=true`
- 用户确认后，传`forcePay=true`再次调用完成生成

### 2. 查看他人画像

- 未解锁时只能看到模糊预览图和摘要
- 解锁需要消耗积分(配置`view.cost.integral`)
- 解锁后永久可查看

### 3. 画像数据聚合

生成画像时会聚合以下数据：
- 基础信息：性别、年龄、城市、职业
- 标签数据：自我标签、印象标签
- 文本内容：自我介绍、兴趣爱好、爱情宣言
- 未来可扩展：帖子内容、圈子参与、互动行为等

### 4. AI生成内容

画像JSON结构：
```json
{
  "summary": "一句话总结",
  "personality": {
    "introvert_extrovert": "内外向倾向",
    "stability": "情绪稳定性",
    "openness": "开放程度"
  },
  "love_style": {
    "attitude": "对待感情的态度",
    "risk_points": ["风险点1", "风险点2"]
  },
  "social_style": {
    "online": "线上社交风格",
    "offline": "线下社交特点"
  },
  "tags_highlight": ["关键标签1", "标签2", "标签3"],
  "suggestions": ["建议1", "建议2"]
}
```

## 四、待完善功能

### 1. 图片渲染服务

当前`renderPersonaImage()`返回占位URL，需要实现：

**方案A: HTML模板+Puppeteer截图**(推荐)
```java
// 1. 使用Thymeleaf/Freemarker渲染HTML
String html = renderHtmlTemplate(personaJson);

// 2. 调用Puppeteer截图服务
String imageUrl = screenshotService.captureHtml(html);

// 3. 上传到OSS
return ossService.upload(imageUrl);
```

**方案B: Java2D直接画图**
```java
BufferedImage image = new BufferedImage(750, 1334, TYPE_INT_RGB);
Graphics2D g = image.getGraphics2D();
// ... 绘制背景、文字、装饰元素
ImageIO.write(image, "PNG", outputStream);
```

### 2. 积分扣费集成

需要对接积分系统：
```java
// 生成画像扣费
integralService.deduct(userId, cost, "生成AI画像", bizId);

// 解锁画像扣费
integralService.deduct(userId, cost, "解锁用户"+targetUserId+"的画像", bizId);

// 检查余额
if (integralService.getBalance(userId) < cost) {
    throw new LinfengException("积分不足");
}
```

### 3. 数据聚合增强

当前只聚合了基础数据，可继续扩展：
```java
// 帖子数据
List<Post> posts = postService.getUserRecentPosts(userId, 10);
features.put("posts", posts);

// 圈子数据
List<Topic> topics = topicService.getUserJoinedTopics(userId);
features.put("topics", topics);

// 互动数据
Map<String, Object> interactions = interactionService.getUserStats(userId);
features.put("interactions", interactions);
```

### 4. 模糊图片生成

未解锁时展示模糊图：
```java
// 使用高斯模糊
BufferedImage blurred = Thumbnails.of(originalImage)
    .size(750, 1334)
    .addFilter(new BlurFilter())
    .asBufferedImage();
    
// 添加水印
Graphics2D g = blurred.createGraphics();
g.drawString("解锁查看完整画像", centerX, centerY);
```

## 五、切换AI模型步骤

### 1. 使用通义千问

```yaml
ai:
  persona:
    provider: tongyi
  tongyi:
    api-key: sk-xxxxx  # 从阿里云DashScope获取
    model: qwen-turbo  # 或 qwen-plus, qwen-max
```

### 2. 使用智谱GLM

```yaml
ai:
  persona:
    provider: zhipu
  zhipu:
    api-key: xxxxx.xxxxx  # 从智谱开放平台获取
    model: glm-4  # 或 glm-4-flash
```

### 3. 测试模式

```yaml
ai:
  persona:
    provider: mock  # 不消耗API额度，返回固定数据
```

## 六、数据库表

### user_persona_snapshot
存储画像快照，支持多版本

### persona_view_record
记录查看/解锁记录，支持去重判断

### persona_config
系统配置，可运行时热更新

---

**注意**: 
1. 实际接入AI模型前请先申请API Key
2. 积分扣费逻辑需要对接现有积分系统
3. 图片渲染服务建议独立部署
4. 敏感信息(如风险点)要委婉表达，避免负面标签化
