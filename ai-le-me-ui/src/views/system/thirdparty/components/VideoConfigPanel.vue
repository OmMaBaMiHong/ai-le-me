<template>
  <div class="video-config-panel">
    <el-row :gutter="16" class="overview-grid">
      <el-col v-for="item in overviewCards" :key="item.label" :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="overview-card" :class="item.className">
          <div class="overview-label">{{ item.label }}</div>
          <div class="overview-value">{{ item.value }}</div>
          <div class="overview-tip">{{ item.tip }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="summary-grid">
      <el-col :xs="24" :xl="16">
        <el-card shadow="never" class="summary-card">
          <template #header>
            <div class="panel-header">
              <div>
                <div class="panel-title">AI 视频运行总览</div>
                <div class="panel-subtitle">管理端只负责服务商配置与调度方式说明，真实生成链路仍走用户端模板页到发布页。</div>
              </div>
              <div class="panel-actions">
                <el-button plain @click="openQuartzPage">
                  <el-icon><Monitor /></el-icon>
                  打开 Quartz 任务页
                </el-button>
                <el-button plain @click="copyExecutorName">
                  <el-icon><DocumentCopy /></el-icon>
                  复制执行器名
                </el-button>
                <el-button @click="handleRefresh" :loading="loading">
                  <el-icon><Refresh /></el-icon>
                  刷新状态
                </el-button>
              </div>
            </div>
          </template>

          <div class="summary-tags">
            <el-tag effect="dark" type="success">{{ runtimeOverview.enabled ? '业务已启用' : '业务已停用' }}</el-tag>
            <el-tag :type="runtimeOverview.pollingMode === 'spring' ? 'success' : 'warning'">
              {{ pollingModeLabel }}
            </el-tag>
            <el-tag :type="runtimeOverview.allowMockFallback ? 'warning' : 'info'">
              {{ runtimeOverview.allowMockFallback ? '允许 Mock 回退' : '不允许 Mock 回退' }}
            </el-tag>
            <el-tag type="info">任务编码 {{ runtimeOverview.quartzRuntime?.jobCode || 'ai_video_polling' }}</el-tag>
          </div>

          <el-alert
            class="runtime-alert"
            :title="runtimeNotice.title"
            :description="runtimeNotice.description"
            :type="runtimeNotice.type"
            :closable="false"
            show-icon
          />

          <div class="runtime-path">
            <div class="path-item">模板选择</div>
            <div class="path-arrow">/</div>
            <div class="path-item">任务生成</div>
            <div class="path-arrow">/</div>
            <div class="path-item">状态轮询</div>
            <div class="path-arrow">/</div>
            <div class="path-item">结果发布</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="8">
        <el-card shadow="never" class="guide-card">
          <template #header>
            <div class="guide-title">
              <el-icon><VideoPlay /></el-icon>
              服务商建议
            </div>
          </template>

          <div class="guide-list">
            <div v-for="item in providerGuideList" :key="item.code" class="guide-item">
              <div class="guide-top">
                <span class="guide-name">{{ item.name }}</span>
                <el-tag size="small" :type="item.tagType">{{ item.tag }}</el-tag>
              </div>
              <div class="guide-desc">{{ item.description }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-grid">
      <el-col :xs="24" :xl="10">
        <el-card shadow="never" class="provider-card-wrapper">
          <template #header>
            <div class="panel-header">
              <div>
                <div class="panel-title">视频服务商</div>
                <div class="panel-subtitle">当前只保留一套生效配置，切换后任务将按新的服务商发起。</div>
              </div>
              <el-button type="primary" @click="showAddProviderDialog">
                <el-icon><Plus /></el-icon>
                新增服务商
              </el-button>
            </div>
          </template>

          <div class="provider-stack">
            <div
              v-for="provider in sortedProviders"
              :key="provider.code"
              class="provider-item"
              :class="{
                active: currentProvider === provider.code,
                recommended: getProviderMeta(provider.code).emphasis === 'recommended'
              }"
            >
              <div class="provider-main">
                <el-avatar :size="42" :src="provider.icon">
                  {{ provider.name.charAt(0) }}
                </el-avatar>
                <div class="provider-body">
                  <div class="provider-top">
                    <div class="provider-name">{{ provider.name }}</div>
                    <div class="provider-badges">
                      <el-tag size="small" :type="provider.isCurrent ? 'success' : 'info'">
                        {{ provider.isCurrent ? '当前使用中' : '可切换' }}
                      </el-tag>
                      <el-tag size="small" :type="getProviderMeta(provider.code).tagType">
                        {{ getProviderMeta(provider.code).tag }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="provider-desc">
                    {{ provider.description || getProviderMeta(provider.code).description }}
                  </div>
                  <div class="provider-links">
                    <el-link v-if="provider.docUrl" type="primary" :href="provider.docUrl" target="_blank"> 接口文档 </el-link>
                    <el-link v-if="provider.officialWebsite" type="primary" :href="provider.officialWebsite" target="_blank"> 官网 </el-link>
                  </div>
                </div>
              </div>
              <div class="provider-actions">
                <el-button v-if="provider.isCurrent" plain type="success" @click="selectProvider(provider.code)"> 当前服务商 </el-button>
                <el-button v-else type="primary" plain :disabled="!getProviderMeta(provider.code).switchable" @click="selectProvider(provider.code)">
                  {{ getProviderMeta(provider.code).switchable ? '切换使用' : '暂不可启用' }}
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="14">
        <el-card v-loading="loading" shadow="never" class="config-card">
          <template #header>
            <div class="panel-header">
              <div>
                <div class="panel-title">{{ currentProviderName }} 配置参数</div>
                <div class="panel-subtitle">这里只维护当前服务商密钥、模型和超时参数，不额外新增首页内容面板或任务逻辑。</div>
              </div>
              <div class="panel-actions">
                <el-button type="primary" @click="handleSave" :loading="saving">
                  <el-icon><Check /></el-icon>
                  保存配置
                </el-button>
                <el-button type="success" @click="handleTest" :loading="testing">
                  <el-icon><Connection /></el-icon>
                  连通性检查
                </el-button>
                <el-button @click="handleRefresh" :loading="loading">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
              </div>
            </div>
          </template>

          <el-alert
            v-if="providerAdvice"
            class="config-alert"
            :title="`${currentProviderName} 建议`"
            :description="providerAdvice"
            :type="providerAdviceType"
            :closable="false"
            show-icon
          />

          <el-alert
            v-if="missingRequiredConfigLabels.length"
            class="config-alert"
            title="仍有必填配置未补齐"
            :description="`请优先完善：${missingRequiredConfigLabels.join('、')}`"
            type="warning"
            :closable="false"
            show-icon
          />

          <div v-if="currentConfigItems.length === 0" class="empty-config">
            <el-empty description="当前服务商还没有配置项">
              <el-button type="primary" @click="showAddProviderDialog">补充服务商配置</el-button>
            </el-empty>
          </div>

          <el-form v-else :model="{}" label-width="150px" class="config-form">
            <template v-for="(config, index) in currentConfigItems" :key="config.configId || index">
              <el-form-item v-if="config.valueType === 'text'" :label="config.configLabel" :required="config.isRequired === 1">
                <el-input v-model="config.configValue" :placeholder="config.defaultValue || `请输入${config.configLabel}`" />
                <div v-if="config.helpText" class="form-tip">
                  <template v-if="isHttpLink(config.helpText)">
                    <el-link type="primary" :href="config.helpText" target="_blank">
                      {{ config.helpText }}
                    </el-link>
                  </template>
                  <template v-else>{{ config.helpText }}</template>
                </div>
              </el-form-item>

              <el-form-item v-if="config.valueType === 'password'" :label="config.configLabel" :required="config.isRequired === 1">
                <el-input v-model="config.configValue" type="password" show-password :placeholder="`请输入${config.configLabel}`" />
                <div v-if="config.helpText" class="form-tip">
                  <template v-if="isHttpLink(config.helpText)">
                    <el-link type="primary" :href="config.helpText" target="_blank"> 点击获取 </el-link>
                  </template>
                  <template v-else>{{ config.helpText }}</template>
                </div>
              </el-form-item>

              <el-form-item v-if="config.valueType === 'number'" :label="config.configLabel" :required="config.isRequired === 1">
                <el-input-number v-model.number="config.configValue" :min="0" :step="1" controls-position="right" />
                <span v-if="config.helpText" class="form-tip-inline">{{ config.helpText }}</span>
              </el-form-item>

              <el-form-item v-if="config.valueType === 'select'" :label="config.configLabel" :required="config.isRequired === 1">
                <el-select v-model="config.configValue" :placeholder="`请选择${config.configLabel}`">
                  <el-option v-for="option in parseSelectOptions(config.selectOptions)" :key="option" :label="option" :value="option" />
                </el-select>
                <span v-if="config.helpText" class="form-tip-inline">{{ config.helpText }}</span>
              </el-form-item>

              <el-form-item v-if="config.valueType === 'json'" :label="config.configLabel" :required="config.isRequired === 1">
                <el-input v-model="config.configValue" type="textarea" :rows="4" :placeholder="config.defaultValue || 'JSON 格式的配置参数'" />
                <div v-if="config.helpText" class="form-tip">{{ config.helpText }}</div>
              </el-form-item>
            </template>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="addProviderDialogVisible" title="新增视频服务提供商" width="920px">
      <el-form :model="newProviderForm" label-width="120px" :rules="providerRules" ref="providerFormRef">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="提供商代码" prop="providerCode">
              <el-input v-model="newProviderForm.providerCode" placeholder="如：doubao" />
              <div class="form-tip">唯一标识，只能使用小写字母和下划线</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示名称" prop="providerName">
              <el-input v-model="newProviderForm.providerName" placeholder="如：豆包视频" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="官方网站">
              <el-input v-model="newProviderForm.officialWebsite" placeholder="https://" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文档地址">
              <el-input v-model="newProviderForm.docUrl" placeholder="https://" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="描述">
          <el-input v-model="newProviderForm.remark" type="textarea" :rows="2" placeholder="例如：适合正式环境，出图链路稳定" />
        </el-form-item>

        <el-divider content-position="left">
          API 配置项
          <el-button type="text" size="small" @click="addConfigItem">
            <el-icon><Plus /></el-icon>
            添加配置
          </el-button>
        </el-divider>

        <div class="config-items-container">
          <el-card v-for="(config, index) in newProviderForm.configs" :key="index" class="config-item-card" shadow="hover">
            <template #header>
              <div class="config-item-header">
                <span>配置项 {{ index + 1 }}</span>
                <el-button type="danger" size="small" text @click="removeConfigItem(index)"> 删除 </el-button>
              </div>
            </template>

            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="配置键" :prop="'configs.' + index + '.configKey'">
                  <el-input v-model="config.configKey" placeholder="如：api_key" />
                  <div class="form-tip">使用下划线分隔，如：secret_access_key</div>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="显示名称" :prop="'configs.' + index + '.configLabel'">
                  <el-input v-model="config.configLabel" placeholder="如：API Key" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="配置类型" :prop="'configs.' + index + '.valueType'">
                  <el-select v-model="config.valueType" placeholder="选择类型">
                    <el-option label="文本" value="text" />
                    <el-option label="密码" value="password" />
                    <el-option label="数字" value="number" />
                    <el-option label="选择" value="select" />
                    <el-option label="JSON" value="json" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="默认值">
                  <el-input v-model="config.defaultValue" placeholder="配置默认值" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="帮助文本">
                  <el-input v-model="config.helpText" placeholder="配置说明或文档链接" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="是否必填">
                  <el-switch v-model="config.isRequired" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="是否敏感">
                  <el-switch v-model="config.isSensitive" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="显示顺序">
                  <el-input-number v-model="config.displayOrder" :min="1" :max="99" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item v-if="config.valueType === 'select'" label="选项列表">
              <el-input v-model="config.selectOptions" placeholder='JSON 数组格式，如：["v1","v2"]' />
              <div class="form-tip">例如：["5","10","15"]</div>
            </el-form-item>
          </el-card>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="addProviderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddProvider" :loading="addingProvider"> 确认新增 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElNotification } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Check, Connection, DocumentCopy, Monitor, Plus, Refresh, VideoPlay } from '@element-plus/icons-vue';
import {
  addProviderWithConfigs,
  getServiceConfig,
  getVideoRuntimeOverview,
  switchProvider,
  testConnection,
  updateConfigBatch
} from '@/api/system/thirdparty';
import type { VideoRuntimeOverview } from '@/api/system/thirdparty';

interface ProviderMeta {
  tag: string;
  tagType: 'success' | 'warning' | 'danger' | 'info';
  description: string;
  emphasis: 'recommended' | 'candidate' | 'neutral';
  switchable: boolean;
}

interface ProviderItem {
  code: string;
  name: string;
  icon?: string;
  description: string;
  isCurrent: boolean;
  officialWebsite?: string;
  docUrl?: string;
}

const router = useRouter();

const providerMetaMap: Record<string, ProviderMeta> = {
  doubao: {
    tag: '推荐',
    tagType: 'success',
    description: '当前最适合先打通正式环境的视频生成链路，配置清晰，风险最小。',
    emphasis: 'recommended',
    switchable: true
  },
  jimeng: {
    tag: '候选',
    tagType: 'warning',
    description: '可以继续保留，但更适合在 AK/SK 或签名链路稳定后再切换生产。',
    emphasis: 'candidate',
    switchable: true
  },
  kling: {
    tag: '未完成',
    tagType: 'danger',
    description: '后端 Provider 还未接入完成，当前不建议切换为生产服务商。',
    emphasis: 'neutral',
    switchable: false
  },
  mock: {
    tag: '测试',
    tagType: 'info',
    description: '仅适合本地调试或排查流程，不建议作为真实视频生产方案。',
    emphasis: 'neutral',
    switchable: true
  }
};

const createDefaultRuntimeOverview = (): VideoRuntimeOverview => ({
  enabled: true,
  defaultProviderCode: 'doubao',
  currentProviderCode: '',
  currentProviderName: '',
  allowMockFallback: false,
  pollingMode: 'spring',
  pollingIntervalMs: 10000,
  pollingInitialDelayMs: 15000,
  pollingTimeoutMinutes: 15,
  springSchedulerActive: false,
  quartzRuntime: {
    exists: false,
    enabled: false,
    jobCode: 'ai_video_polling'
  },
  recommendedProviderCode: 'doubao',
  providerAdvice: {
    doubao: '推荐优先打通生产链路',
    jimeng: '保留候选，需要签名链路稳定后再切换',
    kling: '后端 Provider 尚未接入完成'
  }
});

const providers = ref<ProviderItem[]>([]);
const runtimeOverview = ref<VideoRuntimeOverview>(createDefaultRuntimeOverview());
const currentProvider = ref('');
const loading = ref(false);
const saving = ref(false);
const testing = ref(false);
const addProviderDialogVisible = ref(false);
const addingProvider = ref(false);
const providerFormRef = ref<FormInstance>();
const allConfigs = ref<Record<string, any[]>>({});

const newProviderForm = ref({
  providerCode: '',
  providerName: '',
  providerLogo: '',
  officialWebsite: '',
  docUrl: '',
  remark: '',
  configs: [] as any[]
});

const providerRules = ref<FormRules>({
  providerCode: [
    { required: true, message: '请输入提供商代码', trigger: 'blur' },
    { pattern: /^[a-z_]+$/, message: '只能使用小写字母和下划线', trigger: 'blur' }
  ],
  providerName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }]
});

const currentConfigItems = computed(() => {
  const items = allConfigs.value[currentProvider.value] || [];
  return [...items].sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
});

const currentProviderName = computed(() => getProviderName(currentProvider.value));

const pollingModeLabel = computed(() => {
  return runtimeOverview.value.pollingMode === 'quartz' ? 'Quartz 调度' : '应用内轮询';
});

const providerAdvice = computed(() => {
  return runtimeOverview.value.providerAdvice?.[currentProvider.value] || getProviderMeta(currentProvider.value).description;
});

const providerAdviceType = computed(() => {
  const tagType = getProviderMeta(currentProvider.value).tagType;
  if (tagType === 'danger') {
    return 'error';
  }
  return tagType;
});

const overviewCards = computed(() => {
  const intervalSeconds = Math.max(1, Math.floor(runtimeOverview.value.pollingIntervalMs / 1000));
  const delaySeconds = Math.max(1, Math.floor(runtimeOverview.value.pollingInitialDelayMs / 1000));
  return [
    {
      label: '当前服务商',
      value: currentProviderName.value,
      tip: `默认推荐 ${getProviderName(runtimeOverview.value.recommendedProviderCode)}`,
      className: 'is-provider'
    },
    {
      label: '轮询方式',
      value: pollingModeLabel.value,
      tip: runtimeOverview.value.pollingMode === 'quartz' ? '主服务启动后自动同步并执行' : '当前未启用 Quartz 轮询',
      className: 'is-mode'
    },
    {
      label: '轮询频率',
      value: `${intervalSeconds}s`,
      tip: `首次延迟 ${delaySeconds}s`,
      className: 'is-interval'
    },
    {
      label: '超时与回退',
      value: `${runtimeOverview.value.pollingTimeoutMinutes} 分钟`,
      tip: runtimeOverview.value.allowMockFallback ? '异常时允许回退到 Mock' : '配置错误将直接暴露',
      className: 'is-timeout'
    }
  ];
});

const runtimeNotice = computed(() => {
  if (!runtimeOverview.value.enabled) {
    return {
      type: 'warning' as const,
      title: 'AI 视频业务当前处于关闭态',
      description: '请先确认后端开关、服务商密钥和轮询链路都已准备好，再恢复用户端入口。'
    };
  }
  if (runtimeOverview.value.pollingMode === 'quartz') {
    return {
      type: 'warning' as const,
      title: '当前由 Quartz 接管视频任务轮询',
      description: `请确认任务编码 ${runtimeOverview.value.quartzRuntime?.jobCode || 'ai_video_polling'} 已同步且日志持续产出，否则生成任务会停留在处理中。`
    };
  }
  return {
    type: 'success' as const,
    title: '当前默认由单体服务内部轮询处理视频任务',
    description: '主服务启动后即可自动轮询，无需依赖额外外部调度平台也能完成视频生成闭环。'
  };
});

const providerGuideList = computed(() => {
  return [
    {
      code: 'doubao',
      name: '豆包视频',
      tag: '推荐先打通',
      tagType: 'success' as const,
      description: '适合先跑通模板生成到发布的整条产品链路，配置和排障路径都更直接。'
    },
    {
      code: 'jimeng',
      name: '即梦视频',
      tag: '候选方案',
      tagType: 'warning' as const,
      description: '更适合作为第二服务商保留，前提是签名和鉴权能力已经稳定落地。'
    },
    {
      code: 'kling',
      name: '可灵视频',
      tag: '暂不启用',
      tagType: 'danger' as const,
      description: '当前后端 Provider 尚未接入完成，先不要让运营在这里误切。'
    }
  ];
});

const sortedProviders = computed(() => {
  const priority: Record<string, number> = { doubao: 0, jimeng: 1, kling: 2, mock: 9 };
  return [...providers.value].sort((left, right) => {
    if (left.isCurrent) return -1;
    if (right.isCurrent) return 1;
    return (priority[left.code] ?? 99) - (priority[right.code] ?? 99);
  });
});

const missingRequiredConfigLabels = computed(() => {
  return currentConfigItems.value
    .filter((item: any) => item.isRequired === 1 && isBlankValue(item.configValue))
    .map((item: any) => item.configLabel || item.configKey);
});

const getProviderMeta = (code: string): ProviderMeta => {
  return (
    providerMetaMap[code] || {
      tag: '自定义',
      tagType: 'info',
      description: '这是自定义接入的视频服务商，请自行验证接口协议和任务轮询行为。',
      emphasis: 'neutral',
      switchable: true
    }
  );
};

const getProviderName = (code: string) => {
  return providers.value.find((item) => item.code === code)?.name || code || '未配置';
};

const isBlankValue = (value: unknown) => {
  return value === undefined || value === null || String(value).trim() === '';
};

const isHttpLink = (value?: string) => {
  return !!value && /^https?:\/\//.test(value);
};

const parseSelectOptions = (options: string) => {
  try {
    if (!options) {
      return [];
    }
    return JSON.parse(options);
  } catch {
    return [];
  }
};

const collectCurrentConfigs = () => {
  const configs: Record<string, string> = {};
  currentConfigItems.value.forEach((item: any) => {
    configs[item.configKey] = item.configValue != null ? String(item.configValue) : '';
  });
  return configs;
};

const loadData = async (showSuccess = false) => {
  loading.value = true;
  try {
    const [configResult, runtimeResult] = await Promise.allSettled([getServiceConfig('video'), getVideoRuntimeOverview()]);

    if (configResult.status !== 'fulfilled') {
      throw configResult.reason;
    }

    const configData = (configResult.value as any).data || {};
    providers.value = (configData.providers || []).map((provider: any) => ({
      code: provider.providerCode,
      name: provider.providerName,
      icon: provider.providerLogo,
      description: provider.remark || '',
      isCurrent: provider.isCurrent === 1,
      officialWebsite: provider.officialWebsite,
      docUrl: provider.docUrl
    }));
    allConfigs.value = configData.configs || {};

    const current = providers.value.find((item) => item.isCurrent);
    currentProvider.value = current?.code || providers.value[0]?.code || currentProvider.value;

    if (runtimeResult.status === 'fulfilled') {
      runtimeOverview.value = {
        ...createDefaultRuntimeOverview(),
        ...((runtimeResult.value as any).data || {})
      };
    }

    if (!runtimeOverview.value.currentProviderCode && currentProvider.value) {
      runtimeOverview.value.currentProviderCode = currentProvider.value;
    }
    if (!runtimeOverview.value.currentProviderName && currentProvider.value) {
      runtimeOverview.value.currentProviderName = getProviderName(currentProvider.value);
    }

    if (showSuccess) {
      ElMessage.success('已刷新 AI 视频运行状态');
    }
  } catch (error) {
    console.error('加载 AI 视频配置失败:', error);
    ElMessage.error('加载 AI 视频配置失败');
  } finally {
    loading.value = false;
  }
};

const selectProvider = async (code: string) => {
  if (!code) {
    return;
  }
  const meta = getProviderMeta(code);
  if (!meta.switchable) {
    ElMessage.warning(`${getProviderName(code)} 暂未接入完成，先不要切换到生产链路`);
    return;
  }
  if (code === currentProvider.value) {
    ElMessage.info(`${getProviderName(code)} 已经是当前服务商`);
    return;
  }

  try {
    await switchProvider('video', { providerCode: code });
    ElMessage.success(`已切换到 ${getProviderName(code)}`);
    await loadData();
  } catch (error) {
    console.error('切换视频服务商失败:', error);
    ElMessage.error('切换视频服务商失败');
  }
};

const handleSave = async () => {
  if (!currentConfigItems.value.length) {
    ElMessage.warning('当前服务商没有可保存的配置项');
    return;
  }
  if (missingRequiredConfigLabels.value.length) {
    ElMessage.warning(`请先完善必填配置：${missingRequiredConfigLabels.value.join('、')}`);
    return;
  }

  saving.value = true;
  try {
    const configs = currentConfigItems.value.map((item: any) => ({
      configId: item.configId,
      configKey: item.configKey,
      configValue: item.configValue != null ? String(item.configValue) : ''
    }));
    await updateConfigBatch(configs);
    ElNotification({
      title: '保存成功',
      message: `${currentProviderName.value} 配置已更新`,
      type: 'success'
    });
    await loadData();
  } catch (error) {
    console.error('保存视频服务商配置失败:', error);
    ElMessage.error('保存视频服务商配置失败');
  } finally {
    saving.value = false;
  }
};

const handleTest = async () => {
  if (!currentProvider.value) {
    ElMessage.warning('请先选择一个视频服务商');
    return;
  }
  if (missingRequiredConfigLabels.value.length) {
    ElMessage.warning(`请先完善必填配置：${missingRequiredConfigLabels.value.join('、')}`);
    return;
  }

  testing.value = true;
  try {
    const response = (await testConnection('video', currentProvider.value, collectCurrentConfigs())) as any;
    ElNotification({
      title: '连通性检查通过',
      message: response.msg || `${currentProviderName.value} 当前配置可正常访问`,
      type: 'success'
    });
  } catch (error: any) {
    console.error('视频服务商连通性检查失败:', error);
    ElNotification({
      title: '连通性检查失败',
      message: error?.message || error?.msg || '请检查当前 provider 配置与鉴权参数',
      type: 'error'
    });
  } finally {
    testing.value = false;
  }
};

const handleRefresh = async () => {
  await loadData(true);
};

const openQuartzPage = () => {
  router.push('/monitor/snailjob');
};

const copyExecutorName = async () => {
  const executorName = runtimeOverview.value.quartzRuntime?.jobCode || 'ai_video_polling';
  try {
    await navigator.clipboard.writeText(executorName);
    ElMessage.success(`任务编码已复制：${executorName}`);
  } catch (error) {
    console.error('复制任务编码失败:', error);
    ElMessage.warning(`复制失败，请手动使用：${executorName}`);
  }
};

const showAddProviderDialog = () => {
  newProviderForm.value = {
    providerCode: '',
    providerName: '',
    providerLogo: '',
    officialWebsite: '',
    docUrl: '',
    remark: '',
    configs: []
  };
  addDefaultConfigs();
  addProviderDialogVisible.value = true;
};

const addDefaultConfigs = () => {
  newProviderForm.value.configs = [
    {
      configKey: 'endpoint',
      configLabel: 'API Endpoint',
      valueType: 'text',
      defaultValue: '',
      isRequired: true,
      isSensitive: false,
      displayOrder: 1,
      helpText: 'API 服务地址'
    },
    {
      configKey: 'api_key',
      configLabel: 'API Key',
      valueType: 'password',
      defaultValue: '',
      isRequired: true,
      isSensitive: true,
      displayOrder: 2,
      helpText: '从服务商控制台获取'
    },
    {
      configKey: 'model',
      configLabel: '默认模型',
      valueType: 'text',
      defaultValue: '',
      isRequired: true,
      isSensitive: false,
      displayOrder: 3,
      helpText: '视频生成模型版本'
    },
    {
      configKey: 'timeout',
      configLabel: '请求超时(秒)',
      valueType: 'number',
      defaultValue: '30',
      isRequired: false,
      isSensitive: false,
      displayOrder: 4,
      helpText: '接口请求超时时间'
    }
  ];
};

const addConfigItem = () => {
  newProviderForm.value.configs.push({
    configKey: '',
    configLabel: '',
    valueType: 'text',
    defaultValue: '',
    selectOptions: '',
    isRequired: false,
    isSensitive: false,
    displayOrder: newProviderForm.value.configs.length + 1,
    helpText: ''
  });
};

const removeConfigItem = (index: number) => {
  newProviderForm.value.configs.splice(index, 1);
};

const handleAddProvider = async () => {
  if (!providerFormRef.value) {
    return;
  }

  try {
    await providerFormRef.value.validate();
    if (!newProviderForm.value.configs.length) {
      ElMessage.warning('请至少添加一个配置项');
      return;
    }

    const invalidConfig = newProviderForm.value.configs.find((config) => {
      return !config.configKey || !config.configLabel || !config.valueType;
    });
    if (invalidConfig) {
      ElMessage.warning('请完善所有配置项的必填字段');
      return;
    }

    addingProvider.value = true;
    await addProviderWithConfigs('video', {
      provider: {
        providerCode: newProviderForm.value.providerCode,
        providerName: newProviderForm.value.providerName,
        providerLogo: newProviderForm.value.providerLogo,
        officialWebsite: newProviderForm.value.officialWebsite,
        docUrl: newProviderForm.value.docUrl,
        remark: newProviderForm.value.remark
      },
      configs: newProviderForm.value.configs
    });

    ElNotification({
      title: '新增成功',
      message: `已新增视频服务商 ${newProviderForm.value.providerName}`,
      type: 'success'
    });
    addProviderDialogVisible.value = false;
    await loadData();
  } catch (error: any) {
    console.error('新增视频服务商失败:', error);
    ElMessage.error(error?.message || error?.msg || '新增视频服务商失败');
  } finally {
    addingProvider.value = false;
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.video-config-panel {
  .overview-grid,
  .summary-grid,
  .content-grid {
    margin-bottom: 16px;
  }

  .overview-card {
    border: 1px solid #ebeef5;
    border-radius: 18px;
    min-height: 140px;
    background: radial-gradient(circle at top right, rgba(64, 158, 255, 0.12), transparent 42%), linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);

    :deep(.el-card__body) {
      padding: 22px;
    }

    .overview-label {
      font-size: 13px;
      color: #6b7280;
      margin-bottom: 14px;
    }

    .overview-value {
      font-size: 28px;
      line-height: 1.2;
      font-weight: 600;
      color: #1f2937;
    }

    .overview-tip {
      margin-top: 12px;
      font-size: 13px;
      color: #8a94a6;
    }

    &.is-provider {
      background: radial-gradient(circle at top right, rgba(250, 204, 21, 0.18), transparent 42%), linear-gradient(180deg, #ffffff 0%, #fffaf1 100%);
    }

    &.is-mode {
      background: radial-gradient(circle at top right, rgba(251, 146, 60, 0.18), transparent 42%), linear-gradient(180deg, #ffffff 0%, #fff7ed 100%);
    }

    &.is-interval {
      background: radial-gradient(circle at top right, rgba(34, 197, 94, 0.18), transparent 42%), linear-gradient(180deg, #ffffff 0%, #f1fcf5 100%);
    }

    &.is-timeout {
      background: radial-gradient(circle at top right, rgba(236, 72, 153, 0.14), transparent 42%), linear-gradient(180deg, #ffffff 0%, #fff7fb 100%);
    }
  }

  .summary-card,
  .guide-card,
  .provider-card-wrapper,
  .config-card {
    border-radius: 20px;

    :deep(.el-card__header) {
      padding: 20px 22px 0;
      border-bottom: none;
    }

    :deep(.el-card__body) {
      padding: 18px 22px 22px;
    }
  }

  .panel-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
  }

  .panel-title {
    font-size: 18px;
    font-weight: 600;
    color: #111827;
  }

  .panel-subtitle {
    margin-top: 8px;
    font-size: 13px;
    line-height: 1.6;
    color: #6b7280;
  }

  .panel-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .summary-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 16px;
  }

  .runtime-alert,
  .config-alert {
    margin-bottom: 16px;
    border-radius: 16px;
  }

  .runtime-path {
    display: grid;
    grid-template-columns: repeat(7, minmax(0, 1fr));
    align-items: center;
    gap: 10px;

    .path-item {
      text-align: center;
      padding: 14px 10px;
      border-radius: 14px;
      font-size: 13px;
      color: #374151;
      background: linear-gradient(180deg, #f8fafc 0%, #eef4ff 100%);
      border: 1px solid #e5edff;
    }

    .path-arrow {
      text-align: center;
      color: #9ca3af;
      font-size: 18px;
    }
  }

  .guide-title {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
  }

  .guide-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .guide-item {
    padding: 14px 16px;
    border-radius: 16px;
    background: #f8fafc;
    border: 1px solid #eef2f7;
  }

  .guide-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .guide-name {
    font-size: 15px;
    font-weight: 600;
    color: #111827;
  }

  .guide-desc {
    margin-top: 8px;
    font-size: 13px;
    line-height: 1.7;
    color: #6b7280;
  }

  .provider-stack {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .provider-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 16px;
    border: 1px solid #ebeef5;
    border-radius: 18px;
    transition: all 0.25s ease;
    background: #ffffff;

    &.active {
      border-color: #60a5fa;
      box-shadow: 0 10px 24px rgba(59, 130, 246, 0.08);
      background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
    }

    &.recommended {
      background: radial-gradient(circle at top right, rgba(250, 204, 21, 0.16), transparent 40%), linear-gradient(180deg, #ffffff 0%, #fffbf3 100%);
    }
  }

  .provider-main {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    flex: 1;
    min-width: 0;
  }

  .provider-body {
    flex: 1;
    min-width: 0;
  }

  .provider-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }

  .provider-name {
    font-size: 16px;
    font-weight: 600;
    color: #111827;
  }

  .provider-badges,
  .provider-links,
  .provider-actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  .provider-desc {
    margin-top: 10px;
    font-size: 13px;
    line-height: 1.7;
    color: #6b7280;
  }

  .provider-links {
    margin-top: 10px;
  }

  .empty-config {
    padding: 36px 0 12px;
  }

  .config-form {
    .form-tip {
      margin-top: 6px;
      font-size: 12px;
      line-height: 1.6;
      color: #6b7280;
    }

    .form-tip-inline {
      margin-left: 10px;
      font-size: 12px;
      color: #6b7280;
    }
  }

  .config-items-container {
    max-height: 440px;
    overflow-y: auto;
    padding: 4px;
  }

  .config-item-card {
    margin-bottom: 16px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .config-item-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    font-weight: 600;
  }

  @media (max-width: 1200px) {
    .runtime-path {
      grid-template-columns: repeat(4, minmax(0, 1fr));

      .path-arrow {
        display: none;
      }
    }

    .provider-item {
      flex-direction: column;
      align-items: stretch;
    }

    .provider-actions {
      justify-content: flex-end;
    }
  }

  @media (max-width: 768px) {
    .runtime-path {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .overview-card {
      min-height: auto;
    }
  }
}
</style>
