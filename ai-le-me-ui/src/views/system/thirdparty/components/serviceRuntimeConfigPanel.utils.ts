import type { ThirdPartyConfigItem } from '@/api/system/thirdparty';

export interface ConfigDraftState {
  textValues: Record<string, string>;
  numberValues: Record<string, number>;
  booleanValues: Record<string, boolean>;
}

interface ProviderPresetConfig extends Partial<ThirdPartyConfigItem> {
  configKey: string;
}

const normalizeType = (valueType?: string) => String(valueType || 'text').toLowerCase();

export const isNumberType = (config: ThirdPartyConfigItem) => normalizeType(config.valueType) === 'number';

export const isBooleanType = (config: ThirdPartyConfigItem) => normalizeType(config.valueType) === 'boolean';

export const isSelectType = (config: ThirdPartyConfigItem) => normalizeType(config.valueType) === 'select';

export const isPasswordType = (config: ThirdPartyConfigItem) => ['password', 'secret'].includes(normalizeType(config.valueType));

export const isTextareaType = (config: ThirdPartyConfigItem) => ['json', 'textarea', 'multiline'].includes(normalizeType(config.valueType));

export const resolveTypeLabel = (valueType?: string) => {
  const type = normalizeType(valueType);
  const labelMap: Record<string, string> = {
    text: '文本',
    password: '密码',
    number: '数字',
    boolean: '布尔',
    select: '下拉',
    textarea: '多行',
    json: 'JSON'
  };
  return labelMap[type] || '文本';
};

export const parseOptions = (raw?: string) => {
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed.map((item) => {
        if (typeof item === 'object' && item !== null) {
          return {
            label: String((item as Record<string, unknown>).label ?? (item as Record<string, unknown>).value ?? ''),
            value: String((item as Record<string, unknown>).value ?? (item as Record<string, unknown>).label ?? '')
          };
        }
        return { label: String(item), value: String(item) };
      });
    }
  } catch {
    return raw
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
      .map((item) => ({ label: item, value: item }));
  }
  return [];
};

export const createConfigDraftState = (configs: ThirdPartyConfigItem[]): ConfigDraftState => {
  const draftState: ConfigDraftState = {
    textValues: {},
    numberValues: {},
    booleanValues: {}
  };

  configs.forEach((item) => {
    const rawValue = item.configValue ?? item.defaultValue ?? '';
    if (isNumberType(item)) {
      draftState.numberValues[item.configKey] = Number(rawValue || 0);
      return;
    }
    if (isBooleanType(item)) {
      const value = String(rawValue).toLowerCase();
      draftState.booleanValues[item.configKey] = ['true', '1', 'yes', 'on'].includes(value);
      return;
    }
    draftState.textValues[item.configKey] = rawValue;
  });

  return draftState;
};

export const buildConfigPayload = (configs: ThirdPartyConfigItem[], draftState: ConfigDraftState, serviceType: string, providerCode: string) =>
  configs.map((item) => {
    let configValue = draftState.textValues[item.configKey] ?? '';
    if (isNumberType(item)) {
      configValue = String(draftState.numberValues[item.configKey] ?? 0);
    }
    if (isBooleanType(item)) {
      configValue = draftState.booleanValues[item.configKey] ? 'true' : 'false';
    }
    return {
      ...item,
      serviceType,
      provider: providerCode,
      configValue
    };
  });

export const summarizeProviderConfigs = (configs: ThirdPartyConfigItem[]) => {
  if (!configs.length) {
    return '当前渠道还没有配置项';
  }
  const requiredCount = configs.filter((item) => item.isRequired === 1).length;
  const sensitiveCount = configs.filter((item) => item.isSensitive === 1).length;
  return `共 ${configs.length} 项配置，必填 ${requiredCount} 项，敏感 ${sensitiveCount} 项`;
};

const createPresetConfig = (preset: ProviderPresetConfig, serviceType: string, providerCode: string, order: number): ThirdPartyConfigItem => ({
  serviceType,
  provider: providerCode,
  configKey: preset.configKey,
  configValue: preset.configValue ?? '',
  defaultValue: preset.defaultValue ?? '',
  valueType: preset.valueType ?? 'text',
  selectOptions: preset.selectOptions ?? '',
  isRequired: preset.isRequired ?? 0,
  isSensitive: preset.isSensitive ?? 0,
  isEnabled: 1,
  displayOrder: preset.displayOrder ?? order,
  configLabel: preset.configLabel ?? preset.configKey,
  helpText: preset.helpText ?? '',
  remark: preset.remark ?? ''
});

const ossAccessPolicyOptions = JSON.stringify([
  { label: '公共读写', value: '1' },
  { label: '私有读写', value: '0' },
  { label: '自定义策略', value: '2' }
]);

const providerPresetLibrary: Record<string, Record<string, ProviderPresetConfig[]>> = {
  oss: {
    aliyun: [
      { configKey: 'access_key_id', configLabel: 'AccessKeyId', isRequired: 1, isSensitive: 1, helpText: '阿里云 RAM AccessKey ID' },
      {
        configKey: 'access_key_secret',
        configLabel: 'AccessKeySecret',
        valueType: 'password',
        isRequired: 1,
        isSensitive: 1,
        helpText: '阿里云 RAM AccessKey Secret'
      },
      { configKey: 'bucket', configLabel: 'Bucket', isRequired: 1, helpText: '对象存储 Bucket 名称' },
      { configKey: 'endpoint', configLabel: 'Endpoint', isRequired: 1, helpText: 'OSS SDK 接入点，如 oss-cn-shanghai.aliyuncs.com' },
      {
        configKey: 'access_policy',
        configLabel: '访问策略',
        valueType: 'select',
        defaultValue: '1',
        selectOptions: ossAccessPolicyOptions,
        helpText: '公共桶选公共读写，私有桶选私有读写'
      },
      { configKey: 'domain', configLabel: '访问域名', helpText: '静态访问域名，可直接填写 https:// 开头的 CDN/回源域名' }
    ],
    tencent: [
      { configKey: 'secret_id', configLabel: 'SecretId', isRequired: 1, isSensitive: 1, helpText: '腾讯云 API 密钥 SecretId' },
      {
        configKey: 'secret_key',
        configLabel: 'SecretKey',
        valueType: 'password',
        isRequired: 1,
        isSensitive: 1,
        helpText: '腾讯云 API 密钥 SecretKey'
      },
      { configKey: 'bucket', configLabel: 'Bucket', isRequired: 1, helpText: 'COS Bucket，建议包含 AppId 后缀' },
      { configKey: 'region', configLabel: 'Region', isRequired: 1, helpText: '如 ap-shanghai' },
      {
        configKey: 'access_policy',
        configLabel: '访问策略',
        valueType: 'select',
        defaultValue: '1',
        selectOptions: ossAccessPolicyOptions,
        helpText: '公共桶选公共读写，私有桶选私有读写'
      },
      { configKey: 'domain', configLabel: '访问域名', helpText: '静态访问域名，可直接填写 https:// 开头的加速域名或回源域名' }
    ],
    qiniu: [
      { configKey: 'access_key', configLabel: 'AccessKey', isRequired: 1, isSensitive: 1, helpText: '七牛云 AccessKey' },
      { configKey: 'secret_key', configLabel: 'SecretKey', valueType: 'password', isRequired: 1, isSensitive: 1, helpText: '七牛云 SecretKey' },
      { configKey: 'bucket', configLabel: 'Bucket', isRequired: 1, helpText: '七牛云存储空间名称' },
      { configKey: 'endpoint', configLabel: 'Endpoint', isRequired: 1, helpText: '七牛 S3 兼容接入点，不要填你的业务访问域名' },
      {
        configKey: 'access_policy',
        configLabel: '访问策略',
        valueType: 'select',
        defaultValue: '1',
        selectOptions: ossAccessPolicyOptions,
        helpText: '公共桶选公共读写，私有桶选私有读写'
      },
      { configKey: 'domain', configLabel: '访问域名', helpText: '静态访问域名，可直接填写 https:// 开头的外链域名或 CDN 域名' }
    ],
    minio: [
      { configKey: 'endpoint', configLabel: 'Endpoint', isRequired: 1, helpText: '如 http://127.0.0.1:9000' },
      { configKey: 'access_key', configLabel: 'AccessKey', isRequired: 1, isSensitive: 1, helpText: 'MinIO AccessKey' },
      { configKey: 'secret_key', configLabel: 'SecretKey', valueType: 'password', isRequired: 1, isSensitive: 1, helpText: 'MinIO SecretKey' },
      { configKey: 'bucket', configLabel: 'Bucket', isRequired: 1, helpText: '桶名称' },
      {
        configKey: 'access_policy',
        configLabel: '访问策略',
        valueType: 'select',
        defaultValue: '1',
        selectOptions: ossAccessPolicyOptions,
        helpText: '公共桶选公共读写，私有桶选私有读写'
      },
      { configKey: 'domain', configLabel: '访问域名', helpText: '可选，静态资源访问域名，可填写 https:// 开头' }
    ]
  },
  sms: {
    aliyun: [
      { configKey: 'access_key_id', configLabel: 'AccessKeyId', isRequired: 1, isSensitive: 1, helpText: '阿里云短信 AccessKey ID' },
      {
        configKey: 'access_key_secret',
        configLabel: 'AccessKeySecret',
        valueType: 'password',
        isRequired: 1,
        isSensitive: 1,
        helpText: '阿里云短信 AccessKey Secret'
      },
      { configKey: 'sign_name', configLabel: '短信签名', isRequired: 1, helpText: '短信签名名称' },
      { configKey: 'template_login', configLabel: '登录模板', isRequired: 1, helpText: '登录验证码模板 CODE' },
      { configKey: 'template_notice', configLabel: '通知模板', helpText: '业务通知短信模板 CODE' },
      { configKey: 'region_id', configLabel: 'RegionId', defaultValue: 'cn-hangzhou', helpText: '阿里云短信默认地域' }
    ],
    tencent: [
      { configKey: 'secret_id', configLabel: 'SecretId', isRequired: 1, isSensitive: 1, helpText: '腾讯云短信 SecretId' },
      { configKey: 'secret_key', configLabel: 'SecretKey', valueType: 'password', isRequired: 1, isSensitive: 1, helpText: '腾讯云短信 SecretKey' },
      { configKey: 'sdk_app_id', configLabel: 'SdkAppId', isRequired: 1, helpText: '腾讯云短信应用 ID' },
      { configKey: 'sign_name', configLabel: '短信签名', isRequired: 1, helpText: '短信签名内容' },
      { configKey: 'template_login', configLabel: '登录模板', isRequired: 1, helpText: '登录验证码模板 ID' },
      { configKey: 'template_notice', configLabel: '通知模板', helpText: '业务通知模板 ID' }
    ]
  },
  realname: {
    tencent: [
      { configKey: 'secret_id', configLabel: 'SecretId', isRequired: 1, isSensitive: 1, helpText: '腾讯云实名认证 SecretId' },
      {
        configKey: 'secret_key',
        configLabel: 'SecretKey',
        valueType: 'password',
        isRequired: 1,
        isSensitive: 1,
        helpText: '腾讯云实名认证 SecretKey'
      },
      { configKey: 'region', configLabel: 'Region', defaultValue: 'ap-beijing', helpText: '腾讯云服务地域' },
      { configKey: 'endpoint', configLabel: 'Endpoint', defaultValue: 'faceid.tencentcloudapi.com', helpText: 'API 接入域名' },
      {
        configKey: 'verify_mode',
        configLabel: '认证方式',
        valueType: 'select',
        defaultValue: 'two_factor',
        selectOptions: '[{\"label\":\"二要素核验\",\"value\":\"two_factor\"},{\"label\":\"人脸识别\",\"value\":\"face\"}]'
      },
      { configKey: 'retry_count', configLabel: '重试次数', valueType: 'number', defaultValue: '2' },
      { configKey: 'cost_per_verify', configLabel: '认证费用', valueType: 'number', defaultValue: '0' },
      { configKey: 'vip_free_times', configLabel: 'VIP 免费次数', valueType: 'number', defaultValue: '0' }
    ],
    aliyun: [
      { configKey: 'access_key_id', configLabel: 'AccessKeyId', isRequired: 1, isSensitive: 1, helpText: '阿里云实名认证 AccessKey ID' },
      {
        configKey: 'access_key_secret',
        configLabel: 'AccessKeySecret',
        valueType: 'password',
        isRequired: 1,
        isSensitive: 1,
        helpText: '阿里云实名认证 AccessKey Secret'
      },
      { configKey: 'region', configLabel: 'Region', defaultValue: 'cn-shanghai', helpText: '阿里云服务地域' },
      { configKey: 'endpoint', configLabel: 'Endpoint', defaultValue: 'cloudauth.aliyuncs.com', helpText: 'API 接入域名' },
      {
        configKey: 'verify_mode',
        configLabel: '认证方式',
        valueType: 'select',
        defaultValue: 'two_factor',
        selectOptions: '[{\"label\":\"二要素核验\",\"value\":\"two_factor\"},{\"label\":\"人脸识别\",\"value\":\"face\"}]'
      },
      { configKey: 'retry_count', configLabel: '重试次数', valueType: 'number', defaultValue: '2' },
      { configKey: 'cost_per_verify', configLabel: '认证费用', valueType: 'number', defaultValue: '0' },
      { configKey: 'vip_free_times', configLabel: 'VIP 免费次数', valueType: 'number', defaultValue: '0' }
    ]
  },
  education: {
    '*': [
      { configKey: 'app_id', configLabel: 'AppId', helpText: '学历认证服务 AppId' },
      { configKey: 'app_secret', configLabel: 'AppSecret', valueType: 'password', isSensitive: 1, helpText: '学历认证服务 AppSecret' },
      { configKey: 'api_key', configLabel: 'ApiKey', valueType: 'password', isSensitive: 1, helpText: '第三方认证 ApiKey' },
      { configKey: 'endpoint', configLabel: 'Endpoint', isRequired: 1, helpText: '学历认证接口地址' },
      { configKey: 'notify_url', configLabel: '回调地址', helpText: '异步结果回调地址' }
    ]
  },
  wechat_mini: {
    '*': [
      { configKey: 'app_id', configLabel: 'AppId', isRequired: 1, helpText: '微信小程序 AppId' },
      { configKey: 'app_secret', configLabel: 'AppSecret', valueType: 'password', isRequired: 1, isSensitive: 1, helpText: '微信小程序 AppSecret' },
      { configKey: 'mch_id', configLabel: '商户号', helpText: '微信支付商户号' },
      { configKey: 'api_v3_key', configLabel: 'APIv3 Key', valueType: 'password', isSensitive: 1, helpText: '支付 APIv3 密钥' },
      { configKey: 'notify_url', configLabel: '支付回调地址', helpText: '支付回调地址' }
    ]
  },
  wechat_mp: {
    '*': [
      { configKey: 'app_id', configLabel: 'AppId', isRequired: 1, helpText: '公众号 AppId' },
      { configKey: 'app_secret', configLabel: 'AppSecret', valueType: 'password', isRequired: 1, isSensitive: 1, helpText: '公众号 AppSecret' },
      { configKey: 'token', configLabel: 'Token', helpText: '服务器配置 Token' },
      { configKey: 'aes_key', configLabel: '消息加解密 Key', valueType: 'password', isSensitive: 1, helpText: '消息推送 EncodingAESKey' },
      { configKey: 'match_request_template_id', configLabel: '牵线模板ID', helpText: '公众号模板消息 ID，用于发送牵线申请通知' },
      { configKey: 'match_request_detail_url', configLabel: '牵线详情地址', helpText: '用户点击公众号通知后回到系统处理牵线申请的页面地址' },
      { configKey: 'notify_url', configLabel: '业务回调地址', helpText: '消息或支付回调地址' }
    ]
  },
  wechat_app: {
    '*': [
      { configKey: 'app_id', configLabel: 'AppId', isRequired: 1, helpText: '微信开放平台 AppId' },
      { configKey: 'app_secret', configLabel: 'AppSecret', valueType: 'password', isRequired: 1, isSensitive: 1, helpText: '微信开放平台 AppSecret' },
      { configKey: 'universal_link', configLabel: 'Universal Link', helpText: 'iOS 回跳域名配置' },
      { configKey: 'mch_id', configLabel: '商户号', helpText: '微信支付商户号' },
      { configKey: 'notify_url', configLabel: '支付回调地址', helpText: '支付结果回调地址' }
    ]
  },
  wecom_customer: {
    '*': [
      { configKey: 'corpId', configLabel: 'CorpId', isRequired: 1, helpText: '企业微信 CorpId' },
      { configKey: 'corpSecret', configLabel: 'CorpSecret', valueType: 'password', isRequired: 1, isSensitive: 1, helpText: '企业微信客户联系应用 Secret' },
      { configKey: 'agentId', configLabel: 'AgentId', helpText: '企业微信应用 AgentId，可用于后续扩展能力' },
      { configKey: 'token', configLabel: 'Token', helpText: '企微回调 Token' },
      { configKey: 'aesKey', configLabel: 'AESKey', valueType: 'password', isSensitive: 1, helpText: '企微回调 AESKey' }
    ]
  },
  scrm_vendor: {
    '*': [
      { configKey: 'baseUrl', configLabel: 'BaseUrl', isRequired: 1, helpText: 'SCRM Bridge 服务根地址' },
      { configKey: 'appKey', configLabel: 'AppKey', isRequired: 1, helpText: 'SCRM 接入 AppKey' },
      { configKey: 'appSecret', configLabel: 'AppSecret', valueType: 'password', isRequired: 1, isSensitive: 1, helpText: 'SCRM 接入 AppSecret' },
      { configKey: 'vendorCode', configLabel: 'VendorCode', helpText: 'SCRM 厂商编码，可选' }
    ]
  },
  push: {
    '*': [
      { configKey: 'app_id', configLabel: 'AppId', isRequired: 1, helpText: '推送服务应用 ID' },
      { configKey: 'app_key', configLabel: 'AppKey', isRequired: 1, isSensitive: 1, helpText: '推送服务 AppKey' },
      { configKey: 'master_secret', configLabel: 'MasterSecret', valueType: 'password', isSensitive: 1, helpText: '推送服务 MasterSecret' },
      { configKey: 'endpoint', configLabel: 'Endpoint', helpText: '推送服务接入地址' }
    ]
  },
  map: {
    '*': [
      { configKey: 'api_key', configLabel: 'ApiKey', isRequired: 1, helpText: '地图服务 Key' },
      { configKey: 'security_js_code', configLabel: 'SecurityJsCode', valueType: 'password', isSensitive: 1, helpText: '前端安全密钥或安全码' },
      { configKey: 'endpoint', configLabel: 'Endpoint', helpText: '地图服务接口地址' },
      { configKey: 'region', configLabel: 'Region', helpText: '默认地域或行政区编码' }
    ]
  },
  ai: {
    openai: [
      { configKey: 'api_key', configLabel: 'ApiKey', isRequired: 1, isSensitive: 1, helpText: 'OpenAI API Key' },
      { configKey: 'base_url', configLabel: 'Base URL', defaultValue: 'https://api.openai.com/v1', helpText: '兼容接口地址' },
      { configKey: 'model', configLabel: '默认模型', defaultValue: 'gpt-5.4', helpText: '默认使用的对话模型' },
      { configKey: 'temperature', configLabel: 'Temperature', valueType: 'number', defaultValue: '1' }
    ],
    deepseek: [
      { configKey: 'api_key', configLabel: 'ApiKey', isRequired: 1, isSensitive: 1, helpText: 'DeepSeek API Key' },
      { configKey: 'base_url', configLabel: 'Base URL', defaultValue: 'https://api.deepseek.com', helpText: 'DeepSeek 接口地址' },
      { configKey: 'model', configLabel: '默认模型', defaultValue: 'deepseek-chat', helpText: '默认对话模型' }
    ],
    qwen: [
      { configKey: 'api_key', configLabel: 'ApiKey', isRequired: 1, isSensitive: 1, helpText: '通义千问 API Key' },
      {
        configKey: 'base_url',
        configLabel: 'Base URL',
        defaultValue: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
        helpText: 'DashScope 兼容地址'
      },
      { configKey: 'model', configLabel: '默认模型', defaultValue: 'qwen-plus', helpText: '默认对话模型' }
    ],
    doubao: [
      { configKey: 'api_key', configLabel: 'ApiKey', isRequired: 1, isSensitive: 1, helpText: '豆包 API Key' },
      { configKey: 'endpoint', configLabel: 'Endpoint', isRequired: 1, helpText: '豆包推理接入点' },
      { configKey: 'model', configLabel: '默认模型', helpText: '豆包模型标识' }
    ]
  },
  image: {
    openai: [
      { configKey: 'api_key', configLabel: 'ApiKey', isRequired: 1, isSensitive: 1, helpText: 'OpenAI 图片生成 Key' },
      { configKey: 'base_url', configLabel: 'Base URL', defaultValue: 'https://api.openai.com/v1', helpText: '图片生成接口地址' },
      { configKey: 'model', configLabel: '默认模型', defaultValue: 'gpt-image-1', helpText: '默认图片模型' },
      { configKey: 'size', configLabel: '默认尺寸', defaultValue: '1024x1024', helpText: '默认出图尺寸' }
    ],
    doubao: [
      { configKey: 'api_key', configLabel: 'ApiKey', isRequired: 1, isSensitive: 1, helpText: '豆包图片 API Key' },
      { configKey: 'endpoint', configLabel: 'Endpoint', isRequired: 1, helpText: '豆包图片接口地址' },
      { configKey: 'model', configLabel: '默认模型', helpText: '图片模型标识' },
      { configKey: 'size', configLabel: '默认尺寸', defaultValue: '1024x1024' }
    ]
  },
  video: {
    jimeng: [
      { configKey: 'access_key_id', configLabel: 'AccessKeyId', isSensitive: 1, helpText: '即梦 AccessKeyId，可与 ApiKey 二选一' },
      { configKey: 'api_key', configLabel: 'ApiKey', valueType: 'password', isSensitive: 1, helpText: '即梦 ApiKey，可与 AccessKeyId 二选一' },
      { configKey: 'endpoint', configLabel: 'Endpoint', isRequired: 1, helpText: '视频生成接口地址' },
      { configKey: 'model', configLabel: '默认模型', helpText: '视频模型标识' },
      { configKey: 'timeout', configLabel: '超时时间', valueType: 'number', defaultValue: '300' }
    ],
    doubao: [
      { configKey: 'api_key', configLabel: 'ApiKey', isRequired: 1, isSensitive: 1, helpText: '豆包视频 API Key' },
      { configKey: 'endpoint', configLabel: 'Endpoint', isRequired: 1, helpText: '视频生成接口地址' },
      { configKey: 'model', configLabel: '默认模型', helpText: '视频模型标识' },
      { configKey: 'timeout', configLabel: '超时时间', valueType: 'number', defaultValue: '300' }
    ]
  },
  payment: {
    wechat: [
      { configKey: 'app_id', configLabel: 'AppId', helpText: '公众号/小程序 AppId' },
      { configKey: 'mch_id', configLabel: '商户号', isRequired: 1, helpText: '微信支付商户号' },
      { configKey: 'api_key', configLabel: 'API Key', valueType: 'password', isRequired: 1, isSensitive: 1, helpText: '微信支付 API Key(v2)' },
      { configKey: 'api_v3_key', configLabel: 'APIv3 Key', valueType: 'password', isSensitive: 1, helpText: '微信支付 APIv3 Key' },
      { configKey: 'notify_url', configLabel: '支付回调地址', isRequired: 1, helpText: '支付回调通知地址' }
    ],
    alipay: [
      { configKey: 'app_id', configLabel: 'AppId', isRequired: 1, helpText: '支付宝应用 ID' },
      { configKey: 'private_key', configLabel: '应用私钥', valueType: 'textarea', isRequired: 1, isSensitive: 1, helpText: 'RSA/RSA2 应用私钥' },
      {
        configKey: 'alipay_public_key',
        configLabel: '支付宝公钥',
        valueType: 'textarea',
        isRequired: 1,
        isSensitive: 1,
        helpText: '支付宝开放平台公钥'
      },
      { configKey: 'notify_url', configLabel: '支付回调地址', isRequired: 1, helpText: '支付成功异步通知地址' },
      { configKey: 'return_url', configLabel: '同步跳转地址', helpText: '用户支付完成后前端回跳地址' }
    ]
  }
};

export const getProviderPresetConfigs = (serviceType: string, providerCode: string) => {
  const servicePresets = providerPresetLibrary[serviceType];
  if (!servicePresets) {
    return [] as ThirdPartyConfigItem[];
  }
  const normalizedCode = String(providerCode || '').toLowerCase();
  const presetConfigs = servicePresets[normalizedCode] || servicePresets['*'] || [];
  return presetConfigs.map((item, index) => createPresetConfig(item, serviceType, providerCode, index + 1));
};

export const mergeProviderConfigs = (serviceType: string, providerCode: string, existingConfigs: ThirdPartyConfigItem[]) => {
  const presetConfigs = getProviderPresetConfigs(serviceType, providerCode);
  const mergedMap = new Map<string, ThirdPartyConfigItem>();

  presetConfigs.forEach((item) => {
    mergedMap.set(item.configKey, item);
  });

  existingConfigs.forEach((item, index) => {
    const previous = mergedMap.get(item.configKey);
    mergedMap.set(item.configKey, {
      ...previous,
      ...item,
      displayOrder: item.displayOrder ?? previous?.displayOrder ?? presetConfigs.length + index + 1
    });
  });

  return [...mergedMap.values()].sort((left, right) => (left.displayOrder || 0) - (right.displayOrder || 0));
};
