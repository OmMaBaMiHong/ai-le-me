<template>
  <div class="service-runtime-panel generic-service-panel" v-loading="loading">
    <div class="panel-hero">
      <div>
        <div class="hero-title">{{ resolvedTitle }}</div>
        <div class="hero-desc">{{ resolvedDescription }}</div>
      </div>
      <div class="hero-actions">
        <el-button plain @click="loadData">刷新</el-button>
        <el-button type="primary" @click="openProviderDialog()">新增渠道</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="panel-body">
      <el-col :xs="24" :xl="10">
        <el-card shadow="never" class="provider-card">
          <template #header>
            <div class="card-header">
              <div>
                <div class="card-title">渠道列表</div>
                <div class="card-subtitle">每个渠道就是一组完整配置，点击“配置渠道”统一弹窗维护。</div>
              </div>
            </div>
          </template>

          <div v-if="sortedProviders.length" class="provider-list">
            <div
              v-for="provider in sortedProviders"
              :key="provider.providerId || provider.providerCode"
              class="provider-item"
              :class="{ active: provider.providerCode === selectedProviderCode }"
              @click="selectProvider(provider.providerCode)"
            >
              <div class="provider-main">
                <div>
                  <div class="provider-name">{{ provider.providerName || provider.providerCode }}</div>
                  <div class="provider-code">{{ provider.providerCode }}</div>
                </div>
                <div class="provider-tags">
                  <el-tag size="small" :type="provider.isCurrent === 1 ? 'success' : 'info'">
                    {{ provider.isCurrent === 1 ? '当前' : '候选' }}
                  </el-tag>
                  <el-tag v-if="hasPresetTemplate(provider.providerCode)" size="small" type="warning">模板</el-tag>
                  <el-tag size="small" :type="provider.isEnabled === 1 ? 'primary' : 'danger'">
                    {{ provider.isEnabled === 1 ? '启用' : '停用' }}
                  </el-tag>
                </div>
              </div>
              <div class="provider-remark">{{ provider.remark || resolveProviderSummary(provider.providerCode) }}</div>
              <div class="provider-summary">{{ resolveProviderSummary(provider.providerCode) }}</div>
              <div class="provider-actions">
                <el-button link type="primary" @click.stop="openSettingsDialog(provider)">配置渠道</el-button>
                <el-button link type="primary" @click.stop="openProviderDialog(provider)">编辑</el-button>
                <el-button
                  v-if="provider.isCurrent !== 1"
                  link
                  type="success"
                  :disabled="provider.isEnabled !== 1"
                  @click.stop="handleSwitchProvider(provider)"
                >
                  设为当前
                </el-button>
                <el-button link type="danger" @click.stop="handleDeleteProvider(provider)">删除</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="还没有配置任何渠道">
            <el-button type="primary" @click="openProviderDialog()">新增渠道</el-button>
          </el-empty>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="14">
        <el-card shadow="never" class="config-card">
          <template #header>
            <div class="card-header config-header">
              <div>
                <div class="card-title">{{ selectedProvider?.providerName || serviceLabel }} 配置概览</div>
                <div class="card-subtitle">按渠道模板直接展示真实字段，不需要后台先手动造 key。</div>
              </div>
              <div class="config-actions">
                <el-button :disabled="!selectedProvider" @click="openConfigDialog">新增配置项</el-button>
                <el-button type="primary" :disabled="!selectedProvider" @click="openSettingsDialog()"> 配置渠道 </el-button>
                <el-button v-if="showTest" type="success" :disabled="!selectedProvider" :loading="testing" @click="handleTest"> 测试连接 </el-button>
              </div>
            </div>
          </template>

          <template v-if="selectedProvider">
            <div class="summary-banner">
              <div class="summary-title">{{ resolveProviderSummary(selectedProvider.providerCode) }}</div>
              <div class="summary-desc">配置值会集中在弹窗里编辑，保存后整组写回当前渠道；缺省字段会按模板自动带出。</div>
            </div>

            <div v-if="currentConfigs.length" class="preview-list">
              <div v-for="config in currentConfigs" :key="config.configId || config.configKey" class="preview-item">
                <div class="preview-head">
                  <div>
                    <div class="config-label">{{ resolveConfigLabel(config) }}</div>
                    <div class="config-key">{{ config.configKey }}</div>
                  </div>
                  <div class="config-tags">
                    <el-tag v-if="config.isRequired === 1" size="small" type="danger">必填</el-tag>
                    <el-tag v-if="config.isSensitive === 1" size="small" type="warning">敏感</el-tag>
                    <el-tag size="small" type="info">{{ resolveTypeLabel(config.valueType) }}</el-tag>
                  </div>
                </div>
                <div class="preview-value">{{ maskPreviewValue(config) }}</div>
                <div v-if="config.helpText" class="config-help">{{ config.helpText }}</div>
              </div>
            </div>
            <el-empty v-else description="当前渠道还没有配置项">
              <el-button type="primary" @click="openConfigDialog">新增配置项</el-button>
            </el-empty>
          </template>
          <el-empty v-else description="请先新增渠道，再维护配置值" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="providerDialog.visible" :title="providerDialog.mode === 'create' ? '新增渠道' : '编辑渠道'" width="640px">
      <el-form ref="providerFormRef" :model="providerForm" :rules="providerRules" label-width="108px">
        <el-form-item label="渠道编码" prop="providerCode">
          <el-input v-model="providerForm.providerCode" :disabled="providerDialog.mode === 'edit'" />
        </el-form-item>
        <el-form-item label="渠道名称" prop="providerName">
          <el-input v-model="providerForm.providerName" />
        </el-form-item>
        <el-form-item label="Logo">
          <el-input v-model="providerForm.providerLogo" />
        </el-form-item>
        <el-form-item label="官网地址">
          <el-input v-model="providerForm.officialWebsite" />
        </el-form-item>
        <el-form-item label="文档地址">
          <el-input v-model="providerForm.docUrl" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="providerForm.displayOrder" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="providerForm.isEnabled">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="providerForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="providerDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitProvider">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="settingsDialog.visible" :title="settingsDialogTitle" width="900px" top="5vh" destroy-on-close>
      <div class="settings-toolbar">
        <div>
          <div class="settings-title">{{ resolveProviderSummary(selectedProviderCode) }}</div>
          <div class="settings-desc">这里是当前渠道的整组配置，保存时会一次性写回。</div>
        </div>
        <div class="config-actions">
          <el-button @click="openConfigDialog">新增配置项</el-button>
          <el-button v-if="showTest" type="success" :disabled="!selectedProvider" :loading="testing" @click="handleTest"> 测试连接 </el-button>
        </div>
      </div>

      <div v-if="currentConfigs.length" class="config-list">
        <div v-for="config in currentConfigs" :key="config.configId || config.configKey" class="config-item">
          <div class="config-meta">
            <div>
              <div class="config-label">{{ resolveConfigLabel(config) }}</div>
              <div class="config-key">{{ config.configKey }}</div>
            </div>
            <div class="config-tags">
              <el-tag v-if="config.isRequired === 1" size="small" type="danger">必填</el-tag>
              <el-tag v-if="config.isSensitive === 1" size="small" type="warning">敏感</el-tag>
              <el-tag size="small" type="info">{{ resolveTypeLabel(config.valueType) }}</el-tag>
            </div>
          </div>

          <el-form-item :required="config.isRequired === 1" class="config-form-item">
            <el-select
              v-if="isSelectType(config)"
              v-model="editorTextValues[config.configKey]"
              clearable
              filterable
              style="width: 100%"
              :placeholder="`请选择${resolveConfigLabel(config)}`"
            >
              <el-option v-for="option in parseOptions(config.selectOptions)" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
            <el-input-number
              v-else-if="isNumberType(config)"
              v-model="editorNumberValues[config.configKey]"
              :min="0"
              controls-position="right"
              style="width: 100%"
            />
            <el-switch
              v-else-if="isBooleanType(config)"
              v-model="editorBooleanValues[config.configKey]"
              inline-prompt
              active-text="开"
              inactive-text="关"
            />
            <el-input
              v-else
              v-model="editorTextValues[config.configKey]"
              :type="isTextareaType(config) ? 'textarea' : isPasswordType(config) ? 'password' : 'text'"
              :show-password="isPasswordType(config)"
              :rows="isTextareaType(config) ? 4 : undefined"
              :autosize="isTextareaType(config) ? { minRows: 3, maxRows: 8 } : undefined"
              :placeholder="config.helpText || `请输入${resolveConfigLabel(config)}`"
            />
          </el-form-item>

          <div v-if="config.helpText" class="config-help">{{ config.helpText }}</div>
        </div>
      </div>
      <el-empty v-else description="当前渠道还没有配置项">
        <el-button type="primary" @click="openConfigDialog">新增配置项</el-button>
      </el-empty>

      <template #footer>
        <el-button @click="settingsDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存整组配置</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="configDialog.visible" title="新增配置项" width="640px">
      <el-form ref="configFormRef" :model="configForm" :rules="configRules" label-width="112px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="configForm.configKey" placeholder="例如 app_id / app_secret / endpoint" />
        </el-form-item>
        <el-form-item label="显示名称" prop="configLabel">
          <el-input v-model="configForm.configLabel" />
        </el-form-item>
        <el-form-item label="值类型" prop="valueType">
          <el-select v-model="configForm.valueType" style="width: 100%">
            <el-option label="文本" value="text" />
            <el-option label="密码" value="password" />
            <el-option label="数字" value="number" />
            <el-option label="布尔" value="boolean" />
            <el-option label="下拉" value="select" />
            <el-option label="多行文本" value="textarea" />
            <el-option label="JSON" value="json" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认值">
          <el-input v-model="configForm.defaultValue" />
        </el-form-item>
        <el-form-item label="当前值">
          <el-input v-model="configForm.configValue" />
        </el-form-item>
        <el-form-item label="下拉选项">
          <el-input v-model="configForm.selectOptions" placeholder='JSON数组或 "a,b,c"' />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="configForm.displayOrder" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="是否必填">
          <el-switch v-model="configRequiredBool" inline-prompt active-text="是" inactive-text="否" />
        </el-form-item>
        <el-form-item label="敏感字段">
          <el-switch v-model="configSensitiveBool" inline-prompt active-text="是" inactive-text="否" />
        </el-form-item>
        <el-form-item label="帮助说明">
          <el-input v-model="configForm.helpText" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  addConfig,
  addProvider,
  deleteProvider,
  getServiceConfig,
  switchProvider,
  testConnection,
  updateConfigBatch,
  updateProvider
} from '@/api/system/thirdparty';
import type { ThirdPartyConfigItem, ThirdPartyProvider } from '@/api/system/thirdparty';
import {
  buildConfigPayload,
  createConfigDraftState,
  getProviderPresetConfigs,
  isBooleanType,
  isNumberType,
  isPasswordType,
  isSelectType,
  isTextareaType,
  mergeProviderConfigs,
  parseOptions,
  resolveTypeLabel,
  summarizeProviderConfigs
} from './serviceRuntimeConfigPanel.utils';

const props = withDefaults(
  defineProps<{
    serviceType: string;
    serviceLabel: string;
    title?: string;
    description?: string;
    showTest?: boolean;
  }>(),
  {
    title: '',
    description: '',
    showTest: true
  }
);

const providerFormRef = ref<FormInstance>();
const configFormRef = ref<FormInstance>();
const loading = ref(false);
const saving = ref(false);
const testing = ref(false);
const providers = ref<ThirdPartyProvider[]>([]);
const configMap = ref<Record<string, ThirdPartyConfigItem[]>>({});
const selectedProviderCode = ref('');

const editorTextValues = reactive<Record<string, string>>({});
const editorNumberValues = reactive<Record<string, number>>({});
const editorBooleanValues = reactive<Record<string, boolean>>({});

const providerDialog = reactive({
  visible: false,
  mode: 'create' as 'create' | 'edit'
});

const settingsDialog = reactive({
  visible: false
});

const configDialog = reactive({
  visible: false
});

const providerForm = reactive<ThirdPartyProvider>({
  serviceType: props.serviceType,
  providerCode: '',
  providerName: '',
  providerLogo: '',
  isCurrent: 0,
  isEnabled: 1,
  displayOrder: 1,
  officialWebsite: '',
  docUrl: '',
  remark: ''
});

const configForm = reactive<ThirdPartyConfigItem>({
  serviceType: props.serviceType,
  provider: '',
  configKey: '',
  configValue: '',
  defaultValue: '',
  valueType: 'text',
  selectOptions: '',
  isRequired: 0,
  isSensitive: 0,
  isEnabled: 1,
  displayOrder: 1,
  configLabel: '',
  helpText: '',
  remark: ''
});

const providerRules: FormRules = {
  providerCode: [{ required: true, message: '请输入渠道编码', trigger: 'blur' }],
  providerName: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }]
};

const configRules: FormRules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configLabel: [{ required: true, message: '请输入显示名称', trigger: 'blur' }]
};

const builtinLabelMap: Record<string, string> = {
  appid: 'AppId',
  app_id: 'AppId',
  app_secret: 'AppSecret',
  secret: 'Secret',
  secret_key: 'SecretKey',
  secret_id: 'SecretId',
  mch_id: '商户号',
  merchant_id: '商户号',
  api_key: 'API Key',
  api_v3_key: 'APIv3 Key',
  notify_url: '回调地址',
  endpoint: '接口地址',
  bucket_name: 'Bucket',
  region: '地域',
  access_key_id: 'AccessKeyId',
  access_key_secret: 'AccessKeySecret'
};

const resolvedTitle = computed(() => props.title || `${props.serviceLabel}配置`);
const resolvedDescription = computed(() => props.description || `${props.serviceLabel} 的 provider、密钥与运行参数统一在这里维护。`);
const sortedProviders = computed(() => [...providers.value].sort((left, right) => (left.displayOrder || 0) - (right.displayOrder || 0)));
const selectedProvider = computed(() => providers.value.find((item) => item.providerCode === selectedProviderCode.value));
const currentConfigs = computed(() => {
  return mergeProviderConfigs(props.serviceType, selectedProviderCode.value, configMap.value[selectedProviderCode.value] || []);
});
const settingsDialogTitle = computed(() => `${selectedProvider.value?.providerName || props.serviceLabel} 渠道配置`);

const configRequiredBool = computed({
  get: () => configForm.isRequired === 1,
  set: (value: boolean) => {
    configForm.isRequired = value ? 1 : 0;
  }
});

const configSensitiveBool = computed({
  get: () => configForm.isSensitive === 1,
  set: (value: boolean) => {
    configForm.isSensitive = value ? 1 : 0;
  }
});

const clearEditorValues = () => {
  Object.keys(editorTextValues).forEach((key) => delete editorTextValues[key]);
  Object.keys(editorNumberValues).forEach((key) => delete editorNumberValues[key]);
  Object.keys(editorBooleanValues).forEach((key) => delete editorBooleanValues[key]);
};

const syncEditorValues = () => {
  const draftState = createConfigDraftState(currentConfigs.value);
  clearEditorValues();
  Object.assign(editorTextValues, draftState.textValues);
  Object.assign(editorNumberValues, draftState.numberValues);
  Object.assign(editorBooleanValues, draftState.booleanValues);
};

const buildEditorDraftState = () => ({
  textValues: { ...editorTextValues },
  numberValues: { ...editorNumberValues },
  booleanValues: { ...editorBooleanValues }
});

const resolveConfigLabel = (config: ThirdPartyConfigItem) => {
  if (config.configLabel) {
    return config.configLabel;
  }
  if (builtinLabelMap[config.configKey]) {
    return builtinLabelMap[config.configKey];
  }
  return config.configKey
    .split(/[_-]/g)
    .filter(Boolean)
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(' ');
};

const resolveProviderSummary = (providerCode?: string) =>
  summarizeProviderConfigs(mergeProviderConfigs(props.serviceType, providerCode || '', configMap.value[providerCode || ''] || []));

const maskPreviewValue = (config: ThirdPartyConfigItem) => {
  const rawValue = config.configValue ?? config.defaultValue ?? '';
  if (!rawValue) {
    return '未配置';
  }
  if (config.isSensitive === 1) {
    return '******';
  }
  if (rawValue.length > 72) {
    return `${rawValue.slice(0, 72)}...`;
  }
  return rawValue;
};

const hasPresetTemplate = (providerCode?: string) => getProviderPresetConfigs(props.serviceType, providerCode || '').length > 0;

const resetProviderForm = () => {
  Object.assign(providerForm, {
    providerId: undefined,
    serviceType: props.serviceType,
    providerCode: '',
    providerName: '',
    providerLogo: '',
    isCurrent: 0,
    isEnabled: 1,
    displayOrder: sortedProviders.value.length + 1,
    officialWebsite: '',
    docUrl: '',
    remark: ''
  });
  providerFormRef.value?.clearValidate();
};

const resetConfigForm = () => {
  Object.assign(configForm, {
    configId: undefined,
    serviceType: props.serviceType,
    provider: selectedProviderCode.value,
    configKey: '',
    configValue: '',
    defaultValue: '',
    valueType: 'text',
    selectOptions: '',
    isRequired: 0,
    isSensitive: 0,
    isEnabled: 1,
    displayOrder: currentConfigs.value.length + 1,
    configLabel: '',
    helpText: '',
    remark: ''
  });
  configFormRef.value?.clearValidate();
};

const resetPanelState = () => {
  providers.value = [];
  configMap.value = {};
  selectedProviderCode.value = '';
  clearEditorValues();
  providerDialog.visible = false;
  settingsDialog.visible = false;
  configDialog.visible = false;
  resetProviderForm();
  resetConfigForm();
};

const loadData = async () => {
  loading.value = true;
  try {
    const response: any = await getServiceConfig(props.serviceType);
    providers.value = response.data?.providers || [];
    configMap.value = response.data?.configs || {};
    const currentProviderCode = response.data?.currentProvider?.providerCode;
    selectedProviderCode.value = currentProviderCode || providers.value[0]?.providerCode || '';
    if (settingsDialog.visible) {
      syncEditorValues();
    }
  } finally {
    loading.value = false;
  }
};

const selectProvider = (providerCode: string) => {
  selectedProviderCode.value = providerCode;
};

watch(
  () => props.serviceType,
  async () => {
    resetPanelState();
    await loadData();
  },
  { immediate: true }
);

watch(selectedProviderCode, () => {
  if (settingsDialog.visible) {
    syncEditorValues();
  }
});

const openProviderDialog = (provider?: ThirdPartyProvider) => {
  providerDialog.visible = true;
  providerDialog.mode = provider ? 'edit' : 'create';
  if (provider) {
    Object.assign(providerForm, { ...provider });
  } else {
    resetProviderForm();
  }
};

const submitProvider = async () => {
  await providerFormRef.value?.validate();
  const payload = {
    ...providerForm,
    serviceType: props.serviceType
  };
  if (providerDialog.mode === 'create') {
    await addProvider(props.serviceType, payload);
    ElMessage.success('渠道已新增');
  } else if (providerForm.providerId) {
    await updateProvider(providerForm.providerId, payload);
    ElMessage.success('渠道已更新');
  }
  providerDialog.visible = false;
  await loadData();
};

const handleDeleteProvider = async (provider: ThirdPartyProvider) => {
  if (!provider.providerId) {
    return;
  }
  await ElMessageBox.confirm(`确定删除渠道「${provider.providerName || provider.providerCode}」吗？`, '删除确认', {
    type: 'warning'
  });
  await deleteProvider(provider.providerId);
  ElMessage.success('渠道已删除');
  await loadData();
};

const handleSwitchProvider = async (provider: ThirdPartyProvider) => {
  await switchProvider(props.serviceType, { providerCode: provider.providerCode });
  ElMessage.success('当前渠道已切换');
  await loadData();
};

const openSettingsDialog = (provider?: ThirdPartyProvider) => {
  if (provider?.providerCode) {
    selectedProviderCode.value = provider.providerCode;
  }
  if (!selectedProvider.value) {
    return;
  }
  syncEditorValues();
  settingsDialog.visible = true;
};

const handleSave = async () => {
  if (!selectedProvider.value) {
    return;
  }
  saving.value = true;
  try {
    await updateConfigBatch(buildConfigPayload(currentConfigs.value, buildEditorDraftState(), props.serviceType, selectedProviderCode.value));
    ElMessage.success('渠道配置已保存');
    settingsDialog.visible = false;
    await loadData();
  } finally {
    saving.value = false;
  }
};

const handleTest = async () => {
  if (!selectedProvider.value) {
    return;
  }
  testing.value = true;
  try {
    const sourceDraft = settingsDialog.visible ? buildEditorDraftState() : createConfigDraftState(currentConfigs.value);
    const payload = buildConfigPayload(currentConfigs.value, sourceDraft, props.serviceType, selectedProvider.value.providerCode).reduce(
      (result: Record<string, string>, item) => {
        result[item.configKey] = item.configValue || '';
        return result;
      },
      {}
    );
    const response: any = await testConnection(props.serviceType, selectedProvider.value.providerCode, payload);
    ElMessage.success(response.data || response.msg || '测试完成');
  } finally {
    testing.value = false;
  }
};

const openConfigDialog = () => {
  if (!selectedProvider.value) {
    return;
  }
  resetConfigForm();
  configDialog.visible = true;
};

const submitConfig = async () => {
  await configFormRef.value?.validate();
  await addConfig({
    ...configForm,
    serviceType: props.serviceType,
    provider: selectedProviderCode.value
  });
  ElMessage.success('配置项已新增');
  configDialog.visible = false;
  await loadData();
  if (settingsDialog.visible) {
    syncEditorValues();
  }
};
</script>

<style scoped lang="scss">
.service-runtime-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid #e5edf7;
  background:
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.14), transparent 34%), linear-gradient(135deg, #fffaf2 0%, #f7fbff 45%, #f3fff8 100%);
}

.hero-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.hero-desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.panel-body {
  margin-top: 0;
}

.provider-card,
.config-card {
  border-radius: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.config-header {
  align-items: flex-start;
}

.card-title,
.settings-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.card-subtitle,
.settings-desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #6b7280;
}

.provider-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.config-list,
.preview-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.provider-item,
.config-item,
.preview-item {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid #e5e7eb;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(249, 251, 255, 0.98));
  transition: all 0.2s ease;
}

.provider-item {
  cursor: pointer;
  min-height: 188px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.1), transparent 36%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.98));
}

.provider-item.active {
  border-color: #22c55e;
  box-shadow: 0 14px 28px rgba(34, 197, 94, 0.12);
}

.provider-main,
.config-meta,
.preview-head,
.settings-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.provider-name,
.config-label {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.provider-code,
.config-key {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
  word-break: break-all;
}

.provider-tags,
.config-tags,
.config-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.provider-remark,
.provider-summary,
.preview-value,
.config-help {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.provider-summary {
  color: #111827;
  font-weight: 500;
}

.provider-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.provider-actions :deep(.el-button) {
  margin-left: 0;
  padding: 0 10px;
  height: 28px;
  border-radius: 999px;
  background: #f3f7fb;
}

.summary-banner {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid #dbe7f5;
  background: linear-gradient(135deg, #f9fbff 0%, #f6fff8 100%);
}

.summary-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.summary-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
}

.preview-list {
  margin-top: 16px;
}

.preview-value {
  word-break: break-all;
  color: #111827;
}

.settings-toolbar {
  margin-bottom: 16px;
}

.config-form-item {
  margin: 14px 0 0;
}

@media (max-width: 1200px) {
  .provider-card {
    margin-bottom: 16px;
  }
}

@media (max-width: 768px) {
  .panel-hero,
  .card-header,
  .config-header,
  .provider-main,
  .config-meta,
  .preview-head,
  .settings-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .config-actions {
    width: 100%;
  }
}
</style>
