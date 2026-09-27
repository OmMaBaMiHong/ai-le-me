# 第三方服务配置化实施方案

## 一、整体架构

```
前端配置页面
    ↓
Admin API接口
    ↓
ConfigService(统一配置服务)
    ↓
sys_config表 / Redis缓存
    ↓
各业务模块动态读取
```

## 二、需要配置化的服务清单

### 1. AI模型服务 (新增重点)
- **通义千问**: API Key、Model版本
- **智谱GLM**: API Key、Model版本
- **策略配置**: 当前使用的提供商、降级策略
- **费用配置**: 生成费用、VIP配额、查看费用

### 2. 云存储服务 (已有部分)
- **腾讯COS**: SecretId、SecretKey、Bucket、Domain
- **阿里OSS**: AccessKey、SecretKey、Bucket、Endpoint
- **七牛云**: AccessKey、SecretKey、Bucket、Domain
- **策略配置**: 当前使用的提供商

### 3. 短信服务 (已有部分)
- **腾讯云**: AppId、SecretId、SecretKey、SignName、模板ID
- **阿里云**: AccessKey、SecretKey、SignName、模板ID
- **策略配置**: 当前使用的提供商

### 4. 实名认证服务 (已有SQL)
- **阿里云**: AccessKeyId、AccessKeySecret、Endpoint
- **腾讯云**: SecretId、SecretKey、Region
- **策略配置**: 当前使用的提供商、启用状态

### 5. 学历认证服务
- **学信网**: AppCode、Endpoint
- **第三方**: 备用渠道配置
- **策略配置**: 启用状态、认证费用

### 6. 支付服务 (已有部分)
- **微信支付**: 商户号、APIKey、证书路径、回调域名
- **支付宝**: AppId、私钥、公钥、回调域名
- **策略配置**: 启用的支付方式

### 7. 推送服务 (建议新增)
- **极光推送**: AppKey、MasterSecret
- **友盟推送**: AppKey、AppSecret
- **UniPush**: AppId、AppKey
- **策略配置**: 当前使用的提供商

### 8. 地图服务 (建议新增)
- **腾讯地图**: Key
- **高德地图**: Key
- **百度地图**: AK
- **策略配置**: 当前使用的提供商

## 三、后端实现

### 1. 统一配置读取服务

```java
@Service
public class ThirdPartyConfigService {
    
    @Autowired
    private ISysConfigService configService;
    
    /**
     * 获取AI模型配置
     */
    public AIConfig getAIConfig() {
        return AIConfig.builder()
            .provider(getConfig("ai.persona.provider", "mock"))
            .tongyiKey(getConfig("ai.tongyi.api-key", ""))
            .tongyiModel(getConfig("ai.tongyi.model", "qwen-turbo"))
            .zhipuKey(getConfig("ai.zhipu.api-key", ""))
            .zhipuModel(getConfig("ai.zhipu.model", "glm-4"))
            .generateCost(getConfigInt("persona.generate.cost.integral", 100))
            .vipFreeMonthly(getConfigInt("persona.generate.cost.vip-free-monthly", 3))
            .viewCost(getConfigInt("persona.view.cost.integral", 50))
            .build();
    }
    
    /**
     * 获取OSS配置
     */
    public OSSConfig getOSSConfig() {
        String provider = getConfig("oss.provider", "tencent");
        return OSSConfig.builder()
            .provider(provider)
            .tencentConfig(getTencentOSSConfig())
            .aliyunConfig(getAliyunOSSConfig())
            .qiniuConfig(getQiniuOSSConfig())
            .build();
    }
    
    /**
     * 获取支付配置
     */
    public PaymentConfig getPaymentConfig() {
        return PaymentConfig.builder()
            .wechatEnabled(getConfigBoolean("pay.wechat.enabled", true))
            .wechatMchId(getConfig("pay.wechat.mchId", ""))
            .wechatApiKey(getConfig("pay.wechat.apiKey", ""))
            .alipayEnabled(getConfigBoolean("pay.alipay.enabled", false))
            .alipayAppId(getConfig("pay.alipay.appId", ""))
            .alipayPrivateKey(getConfig("pay.alipay.privateKey", ""))
            .build();
    }
    
    // 工具方法
    private String getConfig(String key, String defaultValue) {
        return configService.selectConfigByKey(key);
    }
    
    private int getConfigInt(String key, int defaultValue) {
        String value = getConfig(key, String.valueOf(defaultValue));
        return Integer.parseInt(value);
    }
    
    private boolean getConfigBoolean(String key, boolean defaultValue) {
        String value = getConfig(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }
}
```

### 2. 修改AI策略管理器(动态读取配置)

```java
@Service
public class AIPersonaStrategyManager {
    
    @Autowired
    private ThirdPartyConfigService thirdPartyConfigService;
    
    public AIPersonaProvider getCurrentProvider() {
        // 从数据库动态读取配置
        AIConfig config = thirdPartyConfigService.getAIConfig();
        String provider = config.getProvider();
        
        switch (provider.toLowerCase()) {
            case "tongyi":
                return createTongyiProvider(config);
            case "zhipu":
                return createZhipuProvider(config);
            default:
                return mockPersonaProvider;
        }
    }
    
    private AIPersonaProvider createTongyiProvider(AIConfig config) {
        TongyiPersonaProvider provider = new TongyiPersonaProvider();
        provider.setApiKey(config.getTongyiKey());
        provider.setModel(config.getTongyiModel());
        return provider;
    }
}
```

### 3. Admin API接口

```java
@RestController
@RequestMapping("/system/thirdparty")
public class ThirdPartyConfigController {
    
    @Autowired
    private ThirdPartyConfigService configService;
    
    /**
     * 获取AI服务配置
     */
    @GetMapping("/ai/config")
    @PreAuthorize("@ss.hasPermi('system:thirdparty:query')")
    public R<AIConfig> getAIConfig() {
        return R.ok(configService.getAIConfig());
    }
    
    /**
     * 更新AI服务配置
     */
    @PutMapping("/ai/config")
    @PreAuthorize("@ss.hasPermi('system:thirdparty:edit')")
    public R<Void> updateAIConfig(@RequestBody AIConfig config) {
        configService.updateAIConfig(config);
        return R.ok();
    }
    
    /**
     * 测试AI连接
     */
    @PostMapping("/ai/test")
    @PreAuthorize("@ss.hasPermi('system:thirdparty:test')")
    public R<String> testAIConnection(@RequestBody Map<String, String> params) {
        String provider = params.get("provider");
        String apiKey = params.get("apiKey");
        // 测试连接逻辑
        return R.ok("连接成功");
    }
    
    // OSS配置接口
    @GetMapping("/oss/config")
    public R<OSSConfig> getOSSConfig() { ... }
    
    // 支付配置接口
    @GetMapping("/payment/config")
    public R<PaymentConfig> getPaymentConfig() { ... }
    
    // 其他服务配置接口...
}
```

## 四、前端实现(Vue3 + Element Plus)

### 1. 菜单结构

```
系统管理
└── 第三方服务配置
    ├── AI模型配置
    ├── 云存储配置
    ├── 短信服务配置
    ├── 实名认证配置
    ├── 学历认证配置
    ├── 支付服务配置
    ├── 推送服务配置
    └── 地图服务配置
```

### 2. AI模型配置页面示例

```vue
<template>
  <div class="third-party-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>AI模型配置</span>
          <el-button type="primary" @click="handleSave">保存配置</el-button>
        </div>
      </template>
      
      <!-- 策略选择 -->
      <el-form :model="form" label-width="150px">
        <el-form-item label="当前使用提供商">
          <el-radio-group v-model="form.provider">
            <el-radio label="mock">测试模式(Mock)</el-radio>
            <el-radio label="tongyi">通义千问</el-radio>
            <el-radio label="zhipu">智谱GLM</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-divider>通义千问配置</el-divider>
        <el-form-item label="API Key">
          <el-input v-model="form.tongyiKey" type="password" show-password 
                    placeholder="sk-xxxxx">
            <template #append>
              <el-button @click="testConnection('tongyi')">测试连接</el-button>
            </template>
          </el-input>
          <div class="form-tip">
            获取地址: <a href="https://dashscope.console.aliyun.com/apiKey" target="_blank">
              阿里云DashScope
            </a>
          </div>
        </el-form-item>
        <el-form-item label="模型版本">
          <el-select v-model="form.tongyiModel">
            <el-option label="qwen-turbo (快速)" value="qwen-turbo"/>
            <el-option label="qwen-plus (平衡)" value="qwen-plus"/>
            <el-option label="qwen-max (高质量)" value="qwen-max"/>
          </el-select>
        </el-form-item>
        
        <el-divider>智谱GLM配置</el-divider>
        <el-form-item label="API Key">
          <el-input v-model="form.zhipuKey" type="password" show-password>
            <template #append>
              <el-button @click="testConnection('zhipu')">测试连接</el-button>
            </template>
          </el-input>
          <div class="form-tip">
            获取地址: <a href="https://open.bigmodel.cn/usercenter/apikeys" target="_blank">
              智谱开放平台
            </a>
          </div>
        </el-form-item>
        <el-form-item label="模型版本">
          <el-select v-model="form.zhipuModel">
            <el-option label="glm-4 (推荐)" value="glm-4"/>
            <el-option label="glm-4-flash (快速)" value="glm-4-flash"/>
          </el-select>
        </el-form-item>
        
        <el-divider>费用配置</el-divider>
        <el-form-item label="生成画像费用">
          <el-input-number v-model="form.generateCost" :min="0" :step="10"/>
          <span class="unit">积分/次</span>
        </el-form-item>
        <el-form-item label="VIP月免费次数">
          <el-input-number v-model="form.vipFreeMonthly" :min="0" :max="10"/>
          <span class="unit">次/月</span>
        </el-form-item>
        <el-form-item label="查看他人画像">
          <el-input-number v-model="form.viewCost" :min="0" :step="10"/>
          <span class="unit">积分/次</span>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 配置历史记录 -->
    <el-card class="mt-3">
      <template #header>配置变更记录</template>
      <el-table :data="historyList">
        <el-table-column prop="configKey" label="配置项"/>
        <el-table-column prop="oldValue" label="原值"/>
        <el-table-column prop="newValue" label="新值"/>
        <el-table-column prop="operator" label="操作人"/>
        <el-table-column prop="updateTime" label="变更时间"/>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAIConfig, updateAIConfig, testAIConnection } from '@/api/system/thirdparty'

const form = ref({
  provider: 'mock',
  tongyiKey: '',
  tongyiModel: 'qwen-turbo',
  zhipuKey: '',
  zhipuModel: 'glm-4',
  generateCost: 100,
  vipFreeMonthly: 3,
  viewCost: 50
})

onMounted(async () => {
  const res = await getAIConfig()
  form.value = res.data
})

const handleSave = async () => {
  await updateAIConfig(form.value)
  ElMessage.success('保存成功')
}

const testConnection = async (provider) => {
  const params = {
    provider,
    apiKey: provider === 'tongyi' ? form.value.tongyiKey : form.value.zhipuKey
  }
  const res = await testAIConnection(params)
  ElMessage.success(res.msg)
}
</script>
```

## 五、实施步骤

### Phase 1: 数据库设计(1天)
- [ ] 扩展sys_config表或创建新表
- [ ] 编写配置初始化SQL
- [ ] 添加配置分组和敏感字段标记

### Phase 2: 后端开发(3天)
- [ ] ThirdPartyConfigService统一配置服务
- [ ] 修改各Provider动态读取配置
- [ ] Admin API接口开发
- [ ] 连接测试功能
- [ ] 配置缓存策略(Redis)

### Phase 3: 前端开发(3天)
- [ ] 配置管理页面(8个服务)
- [ ] 敏感信息脱敏显示
- [ ] 连接测试交互
- [ ] 配置变更记录展示
- [ ] 表单验证和提示

### Phase 4: 测试与优化(1天)
- [ ] 配置热更新测试
- [ ] 多提供商切换测试
- [ ] 敏感信息安全测试
- [ ] 性能测试

## 六、安全策略

### 1. 敏感信息处理
```java
// 查询时脱敏
public String maskSensitiveValue(String value) {
    if (value == null || value.length() < 8) {
        return "******";
    }
    return value.substring(0, 4) + "******" + value.substring(value.length() - 4);
}

// 更新时只更新非空值
if (StringUtils.isNotBlank(newApiKey)) {
    configService.updateConfig("ai.tongyi.api-key", newApiKey);
}
```

### 2. 权限控制
```java
@PreAuthorize("@ss.hasPermi('system:thirdparty:edit')")  // 编辑权限
@PreAuthorize("@ss.hasPermi('system:thirdparty:sensitive')")  // 查看敏感信息
```

### 3. 操作日志
- 记录所有配置变更
- 记录敏感信息访问
- 异常连接告警

## 七、优势

1. **无需重启**: 配置修改实时生效
2. **多环境支持**: 开发/测试/生产环境独立配置
3. **安全可控**: 敏感信息脱敏、权限控制、审计日志
4. **易于切换**: 一键切换服务提供商
5. **成本优化**: 可视化配置便于调整策略降低成本
6. **故障恢复**: 配置历史记录支持快速回滚

## 八、配置项完整清单

见附录: `third_party_config_list.xlsx`

包含:
- 服务名称
- 配置Key
- 默认值
- 是否必填
- 是否敏感
- 获取方式
- 备注说明
