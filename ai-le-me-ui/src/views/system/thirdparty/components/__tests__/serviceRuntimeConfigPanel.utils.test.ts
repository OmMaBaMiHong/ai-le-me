import { describe, expect, it } from 'vitest';
import type { ThirdPartyConfigItem } from '@/api/system/thirdparty';
import {
  buildConfigPayload,
  createConfigDraftState,
  getProviderPresetConfigs,
  mergeProviderConfigs,
  summarizeProviderConfigs
} from '../serviceRuntimeConfigPanel.utils';

const sampleConfigs: ThirdPartyConfigItem[] = [
  {
    serviceType: 'sms',
    provider: 'aliyun',
    configId: 1,
    configKey: 'app_id',
    configValue: 'demo-app',
    valueType: 'text',
    isRequired: 1,
    isSensitive: 0
  },
  {
    serviceType: 'sms',
    provider: 'aliyun',
    configId: 2,
    configKey: 'retry_count',
    configValue: '3',
    valueType: 'number',
    isRequired: 0,
    isSensitive: 0
  },
  {
    serviceType: 'sms',
    provider: 'aliyun',
    configId: 3,
    configKey: 'enabled',
    configValue: 'true',
    valueType: 'boolean',
    isRequired: 0,
    isSensitive: 1
  }
];

describe('serviceRuntimeConfigPanel utils', () => {
  it('creates editable draft values by config type', () => {
    const draftState = createConfigDraftState(sampleConfigs);

    expect(draftState.textValues).toEqual({ app_id: 'demo-app' });
    expect(draftState.numberValues).toEqual({ retry_count: 3 });
    expect(draftState.booleanValues).toEqual({ enabled: true });
  });

  it('builds payload with serialized values for the selected provider', () => {
    const draftState = createConfigDraftState(sampleConfigs);
    draftState.textValues.app_id = 'changed-app';
    draftState.numberValues.retry_count = 5;
    draftState.booleanValues.enabled = false;

    const payload = buildConfigPayload(sampleConfigs, draftState, 'sms', 'aliyun');

    expect(payload).toEqual([
      expect.objectContaining({ serviceType: 'sms', provider: 'aliyun', configKey: 'app_id', configValue: 'changed-app' }),
      expect.objectContaining({ serviceType: 'sms', provider: 'aliyun', configKey: 'retry_count', configValue: '5' }),
      expect.objectContaining({ serviceType: 'sms', provider: 'aliyun', configKey: 'enabled', configValue: 'false' })
    ]);
  });

  it('summarizes provider configs as a channel-level overview', () => {
    expect(summarizeProviderConfigs(sampleConfigs)).toBe('共 3 项配置，必填 1 项，敏感 1 项');
    expect(summarizeProviderConfigs([])).toBe('当前渠道还没有配置项');
  });

  it('returns real provider templates instead of an empty dialog', () => {
    expect(getProviderPresetConfigs('oss', 'tencent').map((item) => item.configKey)).toEqual([
      'secret_id',
      'secret_key',
      'bucket',
      'region',
      'access_policy',
      'domain'
    ]);

    expect(getProviderPresetConfigs('oss', 'qiniu').map((item) => item.configKey)).toEqual([
      'access_key',
      'secret_key',
      'bucket',
      'endpoint',
      'access_policy',
      'domain'
    ]);

    expect(getProviderPresetConfigs('sms', 'aliyun').map((item) => item.configKey)).toEqual([
      'access_key_id',
      'access_key_secret',
      'sign_name',
      'template_login',
      'template_notice',
      'region_id'
    ]);

    expect(getProviderPresetConfigs('wechat_mini', 'wechat').map((item) => item.configKey)).toEqual([
      'app_id',
      'app_secret',
      'mch_id',
      'api_v3_key',
      'notify_url'
    ]);

    expect(getProviderPresetConfigs('ai', 'openai').map((item) => item.configKey)).toEqual(['api_key', 'base_url', 'model', 'temperature']);
  });

  it('merges preset fields with saved values for the same provider', () => {
    const mergedConfigs = mergeProviderConfigs('realname', 'tencent', [
      {
        serviceType: 'realname',
        provider: 'tencent',
        configKey: 'secret_id',
        configValue: 'saved-secret-id',
        configLabel: 'SecretId'
      },
      {
        serviceType: 'realname',
        provider: 'tencent',
        configKey: 'retry_count',
        configValue: '2',
        valueType: 'number'
      }
    ]);

    expect(mergedConfigs.map((item) => item.configKey)).toEqual([
      'secret_id',
      'secret_key',
      'region',
      'endpoint',
      'verify_mode',
      'retry_count',
      'cost_per_verify',
      'vip_free_times'
    ]);
    expect(mergedConfigs.find((item) => item.configKey === 'secret_id')?.configValue).toBe('saved-secret-id');
    expect(mergedConfigs.find((item) => item.configKey === 'retry_count')?.valueType).toBe('number');
  });
});
