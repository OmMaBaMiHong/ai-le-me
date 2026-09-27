<template>
  <div class="config-panel">
    <!-- 提供商选择 -->
    <el-card shadow="never" class="provider-card">
      <template #header>
        <div class="card-header">
          <span>当前使用的AI提供商</span>
          <el-tag :type="getProviderTagType(currentProvider?.providerCode)">
            {{ currentProvider?.providerName || '未配置' }}
          </el-tag>
        </div>
      </template>
      
      <el-radio-group v-model="selectedProvider" size="large" class="provider-group">
        <el-radio-button
          v-for="provider in providers"
          :key="provider.providerCode"
          :label="provider.providerCode"
          :disabled="loading"
        >
          <div class="provider-info">
            <span class="provider-name">{{ provider.providerName }}</span>
            <el-tag v-if="provider.isCurrent" type="success" size="small">使用中</el-tag>
          </div>
        </el-radio-button>
      </el-radio-group>

      <div v-if="selectedProvider !== currentProvider?.providerCode" class="switch-actions">
        <el-button type="primary" :loading="switching" @click="handleSwitchProvider">
          切换到{{ getProviderName(selectedProvider) }}
        </el-button>
        <el-button @click="selectedProvider = currentProvider?.providerCode">取消</el-button>
      </div>
    </el-card>

    <!-- 配置表单 -->
    <el-card shadow="never" class="config-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>{{ getProviderName(selectedProvider) }} 配置</span>
          <div class="header-actions">
            <el-button type="primary" :icon="Plus" @click="handleAdd">
              新增配置
            </el-button>
            <el-button type="primary" :icon="Check" @click="handleSave" :loading="saving">
              保存配置
            </el-button>
            <el-button type="success" :icon="Connection" @click="handleTest" :loading="testing">
              测试连接
            </el-button>
            <el-button :icon="Refresh" @click="handleRefresh" :loading="loading">
              刷新配置
            </el-button>
            <el-button :icon="RefreshLeft" @click="handleReset">
              重置默认值
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="configForm" label-width="150px" class="config-form">
        <template v-for="(config, index) in currentConfigs" :key="config.configId || index">
          <!-- 通用文本输入 -->
          <el-form-item
            v-if="config.valueType === 'text'"
            :label="config.configLabel"
          >
            <el-input
              v-model="config.configValue"
              :placeholder="config.defaultValue || `请输入${config.configLabel}`"
            >
              <template #append>
                <el-button :icon="Delete" type="danger" @click="handleDelete(index)" />
              </template>
            </el-input>
            <div v-if="config.helpText" class="help-text">
              <el-icon><InfoFilled /></el-icon>
              {{ config.helpText }}
            </div>
          </el-form-item>

          <!-- 密码输入 -->
          <el-form-item
            v-if="config.valueType === 'password'"
            :label="config.configLabel"
          >
            <el-input
              v-model="config.configValue"
              type="password"
              show-password
              :placeholder="`请输入${config.configLabel}`"
            >
              <template #append>
                <el-button :icon="Delete" type="danger" @click="handleDelete(index)" />
              </template>
            </el-input>
            <div v-if="config.helpText" class="help-text">
              <el-link type="primary" :href="config.helpText" target="_blank">
                <el-icon><Link /></el-icon>
                获取地址
              </el-link>
            </div>
          </el-form-item>

          <!-- 数字输入 -->
          <el-form-item
            v-if="config.valueType === 'number'"
            :label="config.configLabel"
          >
            <div style="display: flex; align-items: center; width: 100%;">
              <el-input-number
                v-model.number="config.configValue"
                :min="0"
                :step="1"
                controls-position="right"
                style="flex: 1;"
              />
              <span class="unit">{{ config.helpText }}</span>
              <el-button :icon="Delete" type="danger" @click="handleDelete(index)" style="margin-left: 8px;" />
            </div>
          </el-form-item>

          <!-- 下拉选择 -->
          <el-form-item
            v-if="config.valueType === 'select'"
            :label="config.configLabel"
          >
            <el-select v-model="config.configValue" :placeholder="`请选择${config.configLabel}`" style="width: calc(100% - 120px);">
              <el-option
                v-for="option in parseSelectOptions(config.selectOptions)"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
            <el-button :icon="Delete" type="danger" @click="handleDelete(index)" style="margin-left: 8px;" />
          </el-form-item>
        </template>
      </el-form>
    </el-card>

    <!-- 费用配置 (所有提供商共享) -->
    <el-card v-if="commonConfigs.length > 0" shadow="never" class="config-card">
      <template #header>
        <div class="card-header">
          <span>费用与配额配置</span>
          <div class="header-actions">
            <el-button type="primary" size="small" :icon="Check" @click="handleSaveCommon" :loading="saving">
              保存费用配置
            </el-button>
            <el-button size="small" :icon="RefreshLeft" @click="handleResetCommon">
              重置
            </el-button>
          </div>
        </div>
      </template>
      
      <el-form label-width="180px" class="config-form">
        <template v-for="config in commonConfigs" :key="config.configId">
          <el-form-item :label="config.configLabel">
            <el-input-number
              v-model.number="config.configValue"
              :min="0"
              :step="config.configKey === 'vip_free_monthly' ? 1 : 10"
              controls-position="right"
            />
            <span class="unit">{{ config.helpText }}</span>
          </el-form-item>
        </template>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Connection, Refresh, RefreshLeft, Plus, Delete } from '@element-plus/icons-vue'
import { getAIConfig, switchProvider, updateConfigBatch, testConnection, addConfig, deleteConfig } from '@/api/system/thirdparty'

interface Provider {
  providerCode: string
  providerName: string
  isCurrent: number
}

interface Config {
  configId: number
  provider: string
  configKey: string
  configValue: string
  configLabel: string
  valueType: string
  helpText: string
  defaultValue: string
  selectOptions: string
  isSensitive: number
}

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const switching = ref(false)

const providers = ref<Provider[]>([])
const currentProvider = ref<Provider | null>(null)
const selectedProvider = ref('')
const configs = ref<Record<string, Config[]>>({})
const configForm = ref({})

// 当前选中提供商的配置
const currentConfigs = computed(() => {
  return configs.value[selectedProvider.value]?.filter(c => c.provider !== 'common') || []
})

// 通用配置（费用配置）
const commonConfigs = computed(() => {
  return configs.value['common'] || []
})

// 保存原始配置（用于重置）
const originalConfigs = ref<Record<string, Config[]>>({})

// 获取配置数据
const fetchConfig = async () => {
  loading.value = true
  try {
    const res = await getAIConfig()
    if (res.code === 200) {
      providers.value = res.data.providers || []
      currentProvider.value = res.data.currentProvider
      selectedProvider.value = currentProvider.value?.providerCode || 'mock'
      configs.value = res.data.configs || {}
      // 深拷贝保存原始配置
      originalConfigs.value = JSON.parse(JSON.stringify(configs.value))
    }
  } catch (error) {
    ElMessage.error('获取配置失败')
  } finally {
    loading.value = false
  }
}

// 新增配置项
const handleAdd = () => {
  ElMessageBox.prompt('请输入配置键名（如: api_key, model等）', '新增配置项', {
    confirmButtonText: '下一步',
    cancelButtonText: '取消',
    inputPattern: /^[a-z_]+$/,
    inputErrorMessage: '配置键只能包含小写字母和下划线'
  }).then(({ value: configKey }) => {
    // 检查是否已存在
    const exists = currentConfigs.value.some(c => c.configKey === configKey)
    if (exists) {
      ElMessage.error('该配置键已存在')
      return
    }

    // 继续输入配置标签
    ElMessageBox.prompt('请输入配置显示名称', '新增配置项', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: configKey
    }).then(({ value: configLabel }) => {
      // 创建新配置项
      const newConfig: Config = {
        configId: 0, // 新增时为0
        provider: selectedProvider.value,
        configKey: configKey,
        configValue: '',
        configLabel: configLabel || configKey,
        valueType: 'text',
        helpText: '',
        defaultValue: '',
        selectOptions: '',
        isSensitive: 0
      }

      // 添加到当前配置列表
      if (!configs.value[selectedProvider.value]) {
        configs.value[selectedProvider.value] = []
      }
      configs.value[selectedProvider.value].push(newConfig)
      ElMessage.success('已添加新配置项，请填写配置值后保存')
    }).catch(() => {})
  }).catch(() => {})
}

// 删除配置项
const handleDelete = async (index: number) => {
  try {
    const config = currentConfigs.value[index]
    
    await ElMessageBox.confirm(
      `确认删除配置项 "${config.configLabel}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 如果是新增的配置（configId=0），直接从列表删除
    if (!config.configId || config.configId === 0) {
      configs.value[selectedProvider.value].splice(index, 1)
      ElMessage.success('已删除')
    } else {
      // 如果是已存在的配置，调用删除API
      const res = await deleteConfig(config.configId)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        await fetchConfig()
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 切换提供商
const handleSwitchProvider = async () => {
  try {
    await ElMessageBox.confirm(
      `确认切换到 ${getProviderName(selectedProvider.value)} 吗？`,
      '提示',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    switching.value = true
    const res = await switchProvider('ai', { providerCode: selectedProvider.value })
    if (res.code === 200) {
      ElMessage.success('切换成功')
      await fetchConfig()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('切换失败')
    }
  } finally {
    switching.value = false
  }
}

// 保存普通配置
const handleSave = async () => {
  try {
    saving.value = true
    
    // 分离新增和更新的配置
    const newConfigs = currentConfigs.value.filter(c => !c.configId || c.configId === 0)
    const updateConfigs = currentConfigs.value.filter(c => c.configId && c.configId > 0)
    
    // 先处理新增
    for (const config of newConfigs) {
      await addConfig(config)
    }
    
    // 再处理更新
    if (updateConfigs.length > 0) {
      await updateConfigBatch(updateConfigs)
    }
    
    ElMessage.success('保存成功')
    await fetchConfig()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 保存费用配置
const handleSaveCommon = async () => {
  try {
    saving.value = true
    const res = await updateConfigBatch(commonConfigs.value)
    if (res.code === 200) {
      ElMessage.success('费用配置保存成功')
      await fetchConfig()
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 重置费用配置
const handleResetCommon = async () => {
  try {
    await ElMessageBox.confirm(
      '确认将费用配置重置为默认值吗？',
      '提示',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (originalConfigs.value['common']) {
      configs.value['common'] = JSON.parse(
        JSON.stringify(originalConfigs.value['common'])
      )
      ElMessage.success('已重置，请点击保存')
    }
  } catch (error) {
    // 用户取消
  }
}

// 测试连接
const handleTest = async () => {
  const apiKeyConfig = currentConfigs.value.find(c => c.configKey === 'api_key')
  if (!apiKeyConfig?.configValue) {
    ElMessage.warning('请先配置API Key')
    return
  }

  testing.value = true
  try {
    const testConfigs: Record<string, string> = {}
    currentConfigs.value.forEach(config => {
      testConfigs[config.configKey] = config.configValue
    })

    const res = await testConnection('ai', selectedProvider.value, testConfigs)
    if (res.code === 200) {
      ElMessage.success(res.data || '连接成功')
    }
  } catch (error) {
    ElMessage.error('连接测试失败')
  } finally {
    testing.value = false
  }
}

// 刷新配置
const handleRefresh = async () => {
  try {
    await fetchConfig()
    ElMessage.success('刷新成功')
  } catch (error) {
    ElMessage.error('刷新失败')
  }
}

// 重置到默认值
const handleReset = async () => {
  try {
    await ElMessageBox.confirm(
      '确认将所有配置重置为默认值吗？此操作不可恢复！',
      '警告',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 重置为原始配置
    if (originalConfigs.value[selectedProvider.value]) {
      configs.value[selectedProvider.value] = JSON.parse(
        JSON.stringify(originalConfigs.value[selectedProvider.value])
      )
      ElMessage.success('已重置为默认值，请点击保存配置')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重置失败')
    }
  }
}

// 辅助方法
const getProviderName = (code: string) => {
  return providers.value.find(p => p.providerCode === code)?.providerName || code
}

const getProviderTagType = (code?: string) => {
  if (code === 'mock') return 'info'
  if (code === 'tongyi') return 'success'
  if (code === 'zhipu') return 'primary'
  return 'info'
}

const parseSelectOptions = (options: string) => {
  try {
    return JSON.parse(options)
  } catch {
    return []
  }
}

onMounted(() => {
  fetchConfig()
})
</script>

<style scoped lang="scss">
.config-panel {
  .provider-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .provider-group {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;

      :deep(.el-radio-button__inner) {
        padding: 12px 24px;
        border-radius: 8px;
      }

      .provider-info {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;

        .provider-name {
          font-size: 14px;
          font-weight: 500;
        }
      }
    }

    .switch-actions {
      margin-top: 20px;
      display: flex;
      gap: 12px;
    }
  }

  .config-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-actions {
        display: flex;
        gap: 8px;
      }
    }

    .config-form {
      .help-text {
        margin-top: 4px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
        display: flex;
        align-items: center;
        gap: 4px;
      }

      .unit {
        margin-left: 8px;
        color: var(--el-text-color-secondary);
        font-size: 14px;
      }
    }
  }
}
</style>
