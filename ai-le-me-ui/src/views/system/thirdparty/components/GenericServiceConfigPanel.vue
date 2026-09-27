<template>
  <div class="generic-service-panel">
    <el-row :gutter="16">
      <el-col :xs="24" :xl="8">
        <el-card shadow="never" class="provider-card">
          <template #header>
            <div class="card-header">
              <div>
                <div class="title">{{ serviceLabel }} 服务商</div>
                <div class="subtitle">统一管理 provider 基本信息与启用状态</div>
              </div>
              <div class="actions">
                <el-button type="primary" @click="openProviderDialog()">新增</el-button>
                <el-button @click="loadData">刷新</el-button>
              </div>
            </div>
          </template>

          <div v-if="providers.length" class="provider-list">
            <div
              v-for="provider in providers"
              :key="provider.providerId"
              class="provider-item"
              :class="{ active: provider.providerCode === selectedProviderCode }"
              @click="selectProvider(provider.providerCode)"
            >
              <div class="provider-main">
                <div>
                  <div class="provider-name">{{ provider.providerName }}</div>
                  <div class="provider-code">{{ provider.providerCode }}</div>
                </div>
                <div class="provider-tags">
                  <el-tag size="small" :type="provider.isCurrent === 1 ? 'success' : 'info'">
                    {{ provider.isCurrent === 1 ? '当前' : '候选' }}
                  </el-tag>
                  <el-tag size="small" :type="provider.isEnabled === 1 ? 'success' : 'danger'">
                    {{ provider.isEnabled === 1 ? '启用' : '停用' }}
                  </el-tag>
                </div>
              </div>
              <div class="provider-meta">
                <span>排序 {{ provider.displayOrder || 0 }}</span>
                <div class="provider-ops">
                  <el-button link type="primary" @click.stop="openProviderDialog(provider)">编辑</el-button>
                  <el-button
                    v-if="provider.isCurrent !== 1"
                    link
                    type="success"
                    @click.stop="handleSwitchProvider(provider)"
                  >
                    设为当前
                  </el-button>
                  <el-button link type="danger" @click.stop="handleDeleteProvider(provider)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="还没有服务商配置" />
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="16">
        <el-card shadow="never" class="config-card">
          <template #header>
            <div class="card-header">
              <div>
                <div class="title">{{ selectedProvider?.providerName || serviceLabel }} 配置项</div>
                <div class="subtitle">配置保存后将直接写入 provider.config_json</div>
              </div>
              <div class="actions">
                <el-button type="primary" :disabled="!selectedProvider" @click="handleSaveConfigs">保存</el-button>
                <el-button :disabled="!selectedProvider" @click="openConfigDialog">新增配置项</el-button>
                <el-button :disabled="!selectedProvider" @click="handleTest">连通性测试</el-button>
              </div>
            </div>
          </template>

          <div v-if="selectedProvider && currentConfigs.length" class="config-list">
            <div v-for="config in currentConfigs" :key="config.configId" class="config-item">
              <div class="config-top">
                <div>
                  <div class="config-label">{{ config.configLabel || config.configKey }}</div>
                  <div class="config-key">{{ config.configKey }}</div>
                </div>
                <div class="config-ops">
                  <el-tag size="small" v-if="config.isSensitive === 1" type="warning">敏感</el-tag>
                  <el-tag size="small" v-if="config.isRequired === 1" type="danger">必填</el-tag>
                  <el-button link type="danger" @click="handleDeleteConfig(config)">删除</el-button>
                </div>
              </div>

              <component
                :is="resolveFieldComponent(config)"
                v-model="config.configValue"
                v-bind="resolveFieldProps(config)"
                class="config-field"
              >
                <template v-if="config.valueType === 'select'">
                  <el-option
                    v-for="option in parseOptions(config.selectOptions)"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </template>
              </component>

              <div v-if="config.helpText" class="config-help">{{ config.helpText }}</div>
            </div>
          </div>
          <el-empty v-else description="请选择或新增服务商后再维护配置项" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="providerDialog.visible" :title="providerDialog.mode === 'create' ? '新增服务商' : '编辑服务商'" width="640px">
      <el-form ref="providerFormRef" :model="providerForm" :rules="providerRules" label-width="108px">
        <el-form-item label="服务商编码" prop="providerCode">
          <el-input v-model="providerForm.providerCode" :disabled="providerDialog.mode === 'edit'" />
        </el-form-item>
        <el-form-item label="服务商名称" prop="providerName">
          <el-input v-model="providerForm.providerName" />
        </el-form-item>
        <el-form-item label="Logo" prop="providerLogo">
          <el-input v-model="providerForm.providerLogo" />
        </el-form-item>
        <el-form-item label="官网" prop="officialWebsite">
          <el-input v-model="providerForm.officialWebsite" />
        </el-form-item>
        <el-form-item label="文档" prop="docUrl">
          <el-input v-model="providerForm.docUrl" />
        </el-form-item>
        <el-form-item label="排序" prop="displayOrder">
          <el-input-number v-model="providerForm.displayOrder" :min="1" />
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

    <el-dialog v-model="configDialog.visible" title="新增配置项" width="640px">
      <el-form ref="configFormRef" :model="configForm" :rules="configRules" label-width="112px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="configForm.configKey" />
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
          <el-input-number v-model="configForm.displayOrder" :min="1" />
        </el-form-item>
        <el-form-item label="帮助说明">
          <el-input v-model="configForm.helpText" type="textarea" :rows="2" />
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
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  addConfig,
  addProvider,
  deleteConfig,
  deleteProvider,
  getServiceConfig,
  switchProvider,
  testConnection,
  updateConfigBatch,
  updateProvider
} from '@/api/system/thirdparty';
import type { ThirdPartyConfigItem, ThirdPartyProvider } from '@/api/system/thirdparty';

const props = defineProps<{
  serviceType: string;
  serviceLabel: string;
}>();

const providerFormRef = ref<FormInstance>();
const configFormRef = ref<FormInstance>();
const loading = ref(false);
const providers = ref<ThirdPartyProvider[]>([]);
const configMap = ref<Record<string, ThirdPartyConfigItem[]>>({});
const selectedProviderCode = ref('');

const providerDialog = reactive({
  visible: false,
  mode: 'create' as 'create' | 'edit'
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
  providerCode: [{ required: true, message: '请输入服务商编码', trigger: 'blur' }],
  providerName: [{ required: true, message: '请输入服务商名称', trigger: 'blur' }]
};

const configRules: FormRules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configLabel: [{ required: true, message: '请输入显示名称', trigger: 'blur' }]
};

const selectedProvider = computed(() => providers.value.find((item) => item.providerCode === selectedProviderCode.value));
const currentConfigs = computed(() => {
  if (!selectedProviderCode.value) {
    return [];
  }
  return configMap.value[selectedProviderCode.value] || [];
});

const resetProviderForm = () => {
  Object.assign(providerForm, {
    providerId: undefined,
    serviceType: props.serviceType,
    providerCode: '',
    providerName: '',
    providerLogo: '',
    isCurrent: 0,
    isEnabled: 1,
    displayOrder: providers.value.length + 1,
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

const loadData = async () => {
  loading.value = true;
  try {
    const response: any = await getServiceConfig(props.serviceType);
    providers.value = response.data?.providers || [];
    configMap.value = response.data?.configs || {};
    const currentProvider = response.data?.currentProvider?.providerCode;
    selectedProviderCode.value = currentProvider || providers.value[0]?.providerCode || '';
  } finally {
    loading.value = false;
  }
};

const selectProvider = (providerCode: string) => {
  selectedProviderCode.value = providerCode;
};

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
  const payload = { ...providerForm, serviceType: props.serviceType };
  if (providerDialog.mode === 'create') {
    await addProvider(props.serviceType, payload);
    ElMessage.success('服务商已新增');
  } else if (providerForm.providerId) {
    await updateProvider(providerForm.providerId, payload);
    ElMessage.success('服务商已更新');
  }
  providerDialog.visible = false;
  await loadData();
};

const handleDeleteProvider = async (provider: ThirdPartyProvider) => {
  if (!provider.providerId) {
    return;
  }
  await ElMessageBox.confirm(`确定删除服务商「${provider.providerName}」吗？`, '删除确认', { type: 'warning' });
  await deleteProvider(provider.providerId);
  ElMessage.success('服务商已删除');
  await loadData();
};

const handleSwitchProvider = async (provider: ThirdPartyProvider) => {
  await switchProvider(props.serviceType, { providerCode: provider.providerCode });
  ElMessage.success('当前服务商已切换');
  await loadData();
};

const handleSaveConfigs = async () => {
  if (!selectedProvider.value) {
    return;
  }
  await updateConfigBatch(currentConfigs.value.map((item) => ({ ...item, serviceType: props.serviceType, provider: selectedProvider.value!.providerCode })));
  ElMessage.success('配置已保存');
  await loadData();
};

const handleDeleteConfig = async (config: ThirdPartyConfigItem) => {
  if (!config.configId) {
    return;
  }
  await ElMessageBox.confirm(`确定删除配置项「${config.configLabel || config.configKey}」吗？`, '删除确认', { type: 'warning' });
  await deleteConfig(config.configId);
  ElMessage.success('配置项已删除');
  await loadData();
};

const openConfigDialog = () => {
  resetConfigForm();
  configDialog.visible = true;
};

const submitConfig = async () => {
  await configFormRef.value?.validate();
  await addConfig({ ...configForm, serviceType: props.serviceType, provider: selectedProviderCode.value });
  ElMessage.success('配置项已新增');
  configDialog.visible = false;
  await loadData();
};

const handleTest = async () => {
  if (!selectedProvider.value) {
    return;
  }
  const payload = currentConfigs.value.reduce<Record<string, string>>((result, item) => {
    result[item.configKey] = item.configValue || '';
    return result;
  }, {});
  const response: any = await testConnection(props.serviceType, selectedProvider.value.providerCode, payload);
  ElMessage.success(response.msg || response.data || '连接测试已完成');
};

const parseOptions = (raw?: string) => {
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed.map((item) => {
        if (typeof item === 'object') {
          return {
            label: item.label ?? item.value ?? '',
            value: item.value ?? item.label ?? ''
          };
        }
        return { label: String(item), value: String(item) };
      });
    }
  } catch {
    return raw.split(',').map((item) => ({ label: item.trim(), value: item.trim() })).filter((item) => item.value);
  }
  return [];
};

const resolveFieldComponent = (config: ThirdPartyConfigItem) => {
  if (config.valueType === 'select') {
    return 'el-select';
  }
  if (config.valueType === 'json') {
    return 'el-input';
  }
  return 'el-input';
};

const resolveFieldProps = (config: ThirdPartyConfigItem) => {
  if (config.valueType === 'select') {
    return { placeholder: `请选择${config.configLabel || config.configKey}`, clearable: true };
  }
  if (config.valueType === 'json') {
    return { type: 'textarea', rows: 3, placeholder: `请输入${config.configLabel || config.configKey}` };
  }
  return {
    type: config.valueType === 'password' ? 'password' : 'text',
    placeholder: `请输入${config.configLabel || config.configKey}`
  };
};

onMounted(loadData);
</script>

<style scoped lang="scss">
.generic-service-panel {
  .provider-card,
  .config-card {
    border-radius: 18px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    align-items: center;
  }

  .title {
    font-size: 18px;
    font-weight: 600;
    color: #111827;
  }

  .subtitle {
    margin-top: 6px;
    font-size: 12px;
    color: #6b7280;
  }

  .actions {
    display: flex;
    gap: 8px;
  }

  .provider-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .provider-item {
    padding: 14px 16px;
    border: 1px solid #e5e7eb;
    border-radius: 16px;
    cursor: pointer;
    transition: all 0.2s ease;

    &.active {
      border-color: #60a5fa;
      background: linear-gradient(135deg, #f8fbff 0%, #f3fff8 100%);
    }
  }

  .provider-main,
  .provider-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
  }

  .provider-name {
    font-size: 15px;
    font-weight: 600;
    color: #111827;
  }

  .provider-code {
    margin-top: 4px;
    font-size: 12px;
    color: #6b7280;
  }

  .provider-tags,
  .provider-ops {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  .provider-meta {
    margin-top: 10px;
    font-size: 12px;
    color: #6b7280;
  }

  .config-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .config-item {
    padding: 14px 16px;
    border-radius: 16px;
    border: 1px solid #e5e7eb;
    background: #fff;
  }

  .config-top {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    align-items: center;
    margin-bottom: 12px;
  }

  .config-label {
    font-size: 14px;
    font-weight: 600;
    color: #111827;
  }

  .config-key {
    margin-top: 4px;
    font-size: 12px;
    color: #94a3b8;
  }

  .config-ops {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  .config-field {
    width: 100%;
  }

  .config-help {
    margin-top: 8px;
    font-size: 12px;
    color: #6b7280;
    line-height: 1.6;
  }
}
</style>
