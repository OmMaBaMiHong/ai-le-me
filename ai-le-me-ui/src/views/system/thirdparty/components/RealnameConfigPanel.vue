<template>
  <div class="realname-config-panel">
    <!-- 服务商选择 -->
    <el-row :gutter="20" class="provider-list">
      <el-col :span="12" v-for="provider in providers" :key="provider.code">
        <el-card 
          :class="['provider-card', { active: currentProvider === provider.code }]"
          shadow="hover"
          @click="selectProvider(provider.code)"
        >
          <div class="provider-header">
            <el-avatar :size="50" :src="provider.icon">
              {{ provider.name.charAt(0) }}
            </el-avatar>
            <div class="provider-info">
              <h3>{{ provider.name }}</h3>
              <el-tag size="small" :type="provider.enabled ? 'success' : 'info'">
                {{ provider.enabled ? '已启用' : '未启用' }}
              </el-tag>
            </div>
          </div>
          <div class="provider-desc">{{ provider.description }}</div>
          <div class="provider-features">
            <el-tag v-for="feature in provider.features" :key="feature" size="small">
              {{ feature }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 配置面板 -->
    <el-card class="config-form-card" v-if="currentProvider" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>{{ getProviderName(currentProvider) }} 配置参数</span>
          <div class="header-actions">
            <el-button type="primary" size="default" @click="handleSave" :loading="saving">
              <el-icon><Check /></el-icon>
              保存配置
            </el-button>
            <el-button type="success" size="default" @click="handleTest" :loading="testing">
              <el-icon><Connection /></el-icon>
              测试连接
            </el-button>
            <el-button size="default" @click="handleRefresh">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- API基础配置 -->
      <el-divider content-position="left">
        <el-icon><Key /></el-icon>
        API密钥配置
      </el-divider>
      
      <el-form :model="configForm" label-width="150px" class="config-form">
        <el-row :gutter="20">
          <!-- 腾讯云配置 -->
          <template v-if="currentProvider === 'tencent'">
            <el-col :span="12">
              <el-form-item label="SecretId" required>
                <el-input 
                  v-model="configForm.secret_id" 
                  placeholder="请输入腾讯云SecretId"
                >
                  <template #prepend>
                    <el-icon><User /></el-icon>
                  </template>
                </el-input>
                <div class="form-tip">
                  <el-link type="primary" href="https://console.cloud.tencent.com/cam/capi" target="_blank">
                    点击获取SecretId →
                  </el-link>
                </div>
              </el-form-item>
            </el-col>
            
            <el-col :span="12">
              <el-form-item label="SecretKey" required>
                <el-input 
                  v-model="configForm.secret_key" 
                  type="password"
                  show-password
                  placeholder="请输入腾讯云SecretKey"
                >
                  <template #prepend>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
                <div class="form-tip">
                  <el-link type="primary" href="https://console.cloud.tencent.com/cam/capi" target="_blank">
                    点击获取SecretKey →
                  </el-link>
                </div>
              </el-form-item>
            </el-col>
          </template>

          <!-- 阿里云配置 -->
          <template v-if="currentProvider === 'aliyun'">
            <el-col :span="12">
              <el-form-item label="AccessKeyId" required>
                <el-input 
                  v-model="configForm.access_key_id" 
                  placeholder="请输入阿里云AccessKeyId"
                >
                  <template #prepend>
                    <el-icon><User /></el-icon>
                  </template>
                </el-input>
                <div class="form-tip">
                  <el-link type="primary" href="https://ram.console.aliyun.com/manage/ak" target="_blank">
                    点击获取AccessKeyId →
                  </el-link>
                </div>
              </el-form-item>
            </el-col>
            
            <el-col :span="12">
              <el-form-item label="AccessKeySecret" required>
                <el-input 
                  v-model="configForm.access_key_secret" 
                  type="password"
                  show-password
                  placeholder="请输入阿里云AccessKeySecret"
                >
                  <template #prepend>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
                <div class="form-tip">
                  <el-link type="primary" href="https://ram.console.aliyun.com/manage/ak" target="_blank">
                    点击获取AccessKeySecret →
                  </el-link>
                </div>
              </el-form-item>
            </el-col>
          </template>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="服务地域">
              <el-select v-model="configForm.region" placeholder="选择服务地域">
                <el-option 
                  v-for="region in regions" 
                  :key="region.value"
                  :label="region.label"
                  :value="region.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="API端点">
              <el-input 
                v-model="configForm.endpoint" 
                placeholder="API服务地址"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 认证方式配置 -->
      <el-divider content-position="left">
        <el-icon><Stamp /></el-icon>
        认证方式配置
      </el-divider>

      <el-form :model="configForm" label-width="150px" class="config-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="认证方式">
              <el-radio-group v-model="configForm.verify_mode">
                <el-radio-button label="two_factor">二要素核验</el-radio-button>
                <el-radio-button label="face">人脸识别</el-radio-button>
              </el-radio-group>
              <div class="form-tip">
                二要素核验：姓名+身份证号<br/>
                人脸识别：姓名+身份证号+人脸照片
              </div>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="认证费用">
              <el-input-number v-model="configForm.cost_per_verify" :min="0" :step="1" />
              <span class="unit">积分/次</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="VIP免费次数">
              <el-input-number v-model="configForm.vip_free_times" :min="0" />
              <span class="unit">次/月</span>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="重试次数">
              <el-input-number v-model="configForm.retry_count" :min="0" :max="5" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 高级配置 -->
      <el-divider content-position="left">
        <el-icon><Setting /></el-icon>
        高级配置
        <el-button type="text" size="small" @click="showAdvanced = !showAdvanced">
          {{ showAdvanced ? '收起' : '展开' }}
        </el-button>
      </el-divider>

      <el-collapse-transition>
        <div v-show="showAdvanced">
          <el-form :model="configForm" label-width="150px" class="config-form">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="请求超时(秒)">
                  <el-input-number v-model="configForm.timeout" :min="5" :max="60" />
                </el-form-item>
              </el-col>
              
              <el-col :span="12">
                <el-form-item label="并发限制">
                  <el-input-number v-model="configForm.concurrency" :min="1" :max="100" />
                  <div class="form-tip">每秒最大请求数</div>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="24">
                <el-form-item label="回调地址">
                  <el-input 
                    v-model="configForm.callback_url"
                    placeholder="认证结果回调地址(可选)"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>
      </el-collapse-transition>
    </el-card>

    <!-- 测试对话框 -->
    <el-dialog v-model="testDialogVisible" title="实名认证测试" width="500px">
      <el-form :model="testForm" label-width="100px">
        <el-form-item label="测试姓名">
          <el-input v-model="testForm.name" placeholder="张三" />
        </el-form-item>
        <el-form-item label="测试身份证">
          <el-input v-model="testForm.idCard" placeholder="110101199001011234" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="executeTest" :loading="testing">
          开始测试
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Check, Connection, Refresh, Key, Lock, User, Stamp, Setting } from '@element-plus/icons-vue'
import { getServiceConfig, updateConfigBatch, switchProvider, testConnection } from '@/api/system/thirdparty'

const providers = ref<any[]>([])

const currentProvider = ref('tencent')
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const showAdvanced = ref(false)
const testDialogVisible = ref(false)

// 配置项原始数据
const configItems = ref<any[]>([])

const configForm = ref<Record<string, any>>({})

const testForm = ref({
  name: '',
  idCard: ''
})

const regions = ref([
  { label: '广州', value: 'ap-guangzhou' },
  { label: '上海', value: 'ap-shanghai' },
  { label: '北京', value: 'ap-beijing' },
  { label: '成都', value: 'ap-chengdu' }
])

const selectProvider = async (code: string) => {
  try {
    await switchProvider('realname', { providerCode: code })
    currentProvider.value = code
    providers.value.forEach(p => {
      p.enabled = p.code === code
    })
    await loadData()
    ElMessage.success(`已切换到 ${getProviderName(code)}`)
  } catch (error) {
    ElMessage.error('切换提供商失败')
  }
}

// 加载所有数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getServiceConfig('realname') as any
    
    if (res.data?.providers) {
      providers.value = res.data.providers.map((p: any) => ({
        code: p.providerCode,
        name: p.providerName,
        icon: p.providerLogo,
        description: p.remark || '',
        enabled: p.isCurrent === 1,
        features: ['二要素核验', '权威数据源', '快速响应']
      }))
      
      const current = res.data.providers.find((p: any) => p.isCurrent === 1)
      if (current) {
        currentProvider.value = current.providerCode
      }
    }
    
    if (res.data?.configs) {
      configItems.value = res.data.configs[currentProvider.value] || []
      
      const form: Record<string, any> = {}
      configItems.value.forEach((item: any) => {
        const key = item.configKey
        const formKey = key.replace(/_([a-z])/g, (g: string) => g[1].toUpperCase())
        form[formKey] = item.configValue || item.defaultValue
      })
      configForm.value = form
    }
  } catch (error) {
    console.error('加载配置失败:', error)
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    const configs = configItems.value.map((item: any) => {
      const key = item.configKey
      const formKey = key.replace(/_([a-z])/g, (g: string) => g[1].toUpperCase())
      return {
        configId: item.configId,
        configKey: key,
        configValue: configForm.value[formKey] || ''
      }
    })
    
    await updateConfigBatch(configs)
    
    ElNotification({
      title: '保存成功',
      message: `${getProviderName(currentProvider.value)} 配置已保存`,
      type: 'success'
    })
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleTest = () => {
  testDialogVisible.value = true
}

const executeTest = async () => {
  testing.value = true
  try {
    const configs: Record<string, string> = {}
    configItems.value.forEach((item: any) => {
      const key = item.configKey
      const formKey = key.replace(/_([a-z])/g, (g: string) => g[1].toUpperCase())
      configs[key] = configForm.value[formKey] || ''
    })
    configs['test_name'] = testForm.value.name
    configs['test_id_card'] = testForm.value.idCard
    
    const res = await testConnection('realname', currentProvider.value, configs) as any
    
    ElNotification({
      title: '测试成功',
      message: res.msg || '实名认证API连接正常',
      type: 'success'
    })
    testDialogVisible.value = false
  } catch (error: any) {
    ElNotification({
      title: '测试失败',
      message: error.msg || '实名认证API异常',
      type: 'error'
    })
  } finally {
    testing.value = false
  }
}

const handleRefresh = () => {
  loadData()
}

const getProviderName = (code: string) => {
  return providers.value.find(p => p.code === code)?.name || code
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.realname-config-panel {
  .provider-list {
    margin-bottom: 20px;

    .provider-card {
      cursor: pointer;
      transition: all 0.3s;
      border: 2px solid transparent;

      &.active {
        border-color: var(--el-color-primary);
        background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
      }

      &:hover {
        transform: translateY(-2px);
      }

      .provider-header {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 12px;

        .provider-info {
          flex: 1;
          
          h3 {
            margin: 0 0 4px 0;
            font-size: 16px;
          }
        }
      }

      .provider-desc {
        font-size: 13px;
        color: #909399;
        margin-bottom: 12px;
        min-height: 40px;
      }

      .provider-features {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
      }
    }
  }

  .config-form-card {
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
      .form-tip {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
        line-height: 1.5;
      }

      .unit {
        margin-left: 8px;
        color: #909399;
      }
    }
  }
}
</style>
