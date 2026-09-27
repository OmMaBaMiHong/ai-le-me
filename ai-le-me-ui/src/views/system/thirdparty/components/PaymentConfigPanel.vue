<template>
  <div class="payment-config-panel">
    <!-- 支付方式选择 -->
    <el-row :gutter="20" class="provider-list">
      <el-col :span="12" v-for="provider in providers" :key="provider.code">
        <el-card 
          :class="['provider-card', { active: provider.enabled }]"
          shadow="hover"
        >
          <div class="provider-header">
            <el-avatar :size="50" :src="provider.icon">
              {{ provider.name.charAt(0) }}
            </el-avatar>
            <div class="provider-info">
              <h3>{{ provider.name }}</h3>
              <el-switch 
                v-model="provider.enabled" 
                @change="handleToggleProvider(provider.code)"
                active-text="启用"
                inactive-text="禁用"
              />
            </div>
          </div>
          <div class="provider-desc">{{ provider.description }}</div>
          <div class="provider-status">
            <el-tag :type="provider.enabled ? 'success' : 'info'" size="small">
              {{ provider.enabled ? '已启用' : '未启用' }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 微信支付配置 -->
    <el-card v-if="providers.find(p => p.code === 'wechat')?.enabled" class="config-form-card" v-loading="loadingWechat">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon class="wechat-icon"><Promotion /></el-icon>
            <span>微信支付配置</span>
          </div>
          <div class="header-actions">
            <el-button type="primary" size="default" @click="handleSaveWechat" :loading="savingWechat">
              <el-icon><Check /></el-icon>
              保存配置
            </el-button>
            <el-button type="success" size="default" @click="handleTestWechat" :loading="testingWechat">
              <el-icon><Connection /></el-icon>
              测试连接
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="wechatForm" label-width="150px" class="config-form">
        <!-- 基础配置 -->
        <el-divider content-position="left">
          <el-icon><Setting /></el-icon>
          基础配置
        </el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商户号" required>
              <el-input 
                v-model="wechatForm.mch_id" 
                placeholder="微信支付商户号"
              >
                <template #prepend>
                  <el-icon><Shop /></el-icon>
                </template>
              </el-input>
              <div class="form-tip">
                <el-link type="primary" href="https://pay.weixin.qq.com" target="_blank">
                  登录微信商户平台 →
                </el-link>
              </div>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="AppId" required>
              <el-input 
                v-model="wechatForm.app_id" 
                placeholder="公众号/小程序AppId"
              >
                <template #prepend>
                  <el-icon><Cellphone /></el-icon>
                </template>
              </el-input>
              <div class="form-tip">
                <el-link type="primary" href="https://mp.weixin.qq.com" target="_blank">
                  登录微信公众平台 →
                </el-link>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="API密钥" required>
              <el-input 
                v-model="wechatForm.api_key" 
                type="password"
                show-password
                placeholder="API密钥(v2)"
              >
                <template #prepend>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
              <div class="form-tip">在商户平台 → 账户中心 → API安全中设置</div>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="APIv3密钥">
              <el-input 
                v-model="wechatForm.api_v3_key" 
                type="password"
                show-password
                placeholder="APIv3密钥(可选)"
              >
                <template #prepend>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
              <div class="form-tip">用于新版API和回调验签</div>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 回调配置 -->
        <el-divider content-position="left">
          <el-icon><Bell /></el-icon>
          回调配置
        </el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="支付回调地址" required>
              <el-input 
                v-model="wechatForm.notify_url" 
                placeholder="https://your-domain.com/api/pay/wechat/notify"
              >
                <template #prepend>
                  <el-icon><Link /></el-icon>
                </template>
              </el-input>
              <div class="form-tip">支付成功后微信回调此地址，必须是公网可访问的HTTPS地址</div>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="退款回调地址">
              <el-input 
                v-model="wechatForm.refund_notify_url" 
                placeholder="https://your-domain.com/api/pay/wechat/refund"
              >
                <template #prepend>
                  <el-icon><Link /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="H5支付域名">
              <el-input 
                v-model="wechatForm.h5_domain" 
                placeholder="www.example.com"
              />
              <div class="form-tip">H5支付时的跳转域名</div>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="H5跳转地址">
              <el-input
                v-model="wechatForm.h5_redirect_url"
                placeholder="https://www.example.com/#/subpages/account/account?payStatus=success"
              />
              <div class="form-tip">微信 H5 支付完成后的回跳地址，未配置时默认回到当前站点</div>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 证书配置 -->
        <el-divider content-position="left">
          <el-icon><Document /></el-icon>
          证书配置
        </el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商户证书">
              <el-upload
                class="cert-upload"
                action="#"
                :auto-upload="false"
                :on-change="handleCertChange"
                accept=".p12,.pem"
              >
                <el-button type="primary">上传证书</el-button>
                <template #tip>
                  <div class="form-tip">退款、企业付款等操作需要商户证书(apiclient_cert.p12)</div>
                </template>
              </el-upload>
              <el-input 
                v-if="wechatForm.cert_path"
                v-model="wechatForm.cert_path"
                disabled
                style="margin-top: 8px;"
              />
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="证书序列号">
              <el-input 
                v-model="wechatForm.cert_serial_no" 
                placeholder="证书序列号(可选)"
              />
              <div class="form-tip">用于APIv3验签</div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 支付宝配置 -->
    <el-card v-if="providers.find(p => p.code === 'alipay')?.enabled" class="config-form-card" v-loading="loadingAlipay" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon class="alipay-icon"><Wallet /></el-icon>
            <span>支付宝配置</span>
          </div>
          <div class="header-actions">
            <el-button type="primary" size="default" @click="handleSaveAlipay" :loading="savingAlipay">
              <el-icon><Check /></el-icon>
              保存配置
            </el-button>
            <el-button type="success" size="default" @click="handleTestAlipay" :loading="testingAlipay">
              <el-icon><Connection /></el-icon>
              测试连接
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="alipayForm" label-width="150px" class="config-form">
        <!-- 基础配置 -->
        <el-divider content-position="left">
          <el-icon><Setting /></el-icon>
          基础配置
        </el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="应用ID" required>
              <el-input 
                v-model="alipayForm.app_id" 
                placeholder="支付宝应用ID"
              >
                <template #prepend>
                  <el-icon><Cellphone /></el-icon>
                </template>
              </el-input>
              <div class="form-tip">
                <el-link type="primary" href="https://open.alipay.com" target="_blank">
                  登录支付宝开放平台 →
                </el-link>
              </div>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="签名方式">
              <el-select v-model="alipayForm.sign_type" placeholder="选择签名方式">
                <el-option label="RSA2(推荐)" value="RSA2" />
                <el-option label="RSA" value="RSA" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="应用私钥" required>
              <el-input 
                v-model="alipayForm.private_key" 
                type="textarea"
                :rows="4"
                placeholder="应用私钥(RSA2格式)"
              />
              <div class="form-tip">使用支付宝密钥生成工具生成密钥对</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="支付宝公钥" required>
              <el-input 
                v-model="alipayForm.alipay_public_key" 
                type="textarea"
                :rows="4"
                placeholder="支付宝公钥"
              />
              <div class="form-tip">在开放平台获取支付宝公钥</div>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 回调配置 -->
        <el-divider content-position="left">
          <el-icon><Bell /></el-icon>
          回调配置
        </el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="支付回调地址" required>
              <el-input 
                v-model="alipayForm.notify_url" 
                placeholder="https://your-domain.com/api/pay/alipay/notify"
              >
                <template #prepend>
                  <el-icon><Link /></el-icon>
                </template>
              </el-input>
              <div class="form-tip">必须是公网可访问的HTTP/HTTPS地址</div>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="返回地址">
              <el-input 
                v-model="alipayForm.return_url" 
                placeholder="支付成功后跳转地址"
              >
                <template #prepend>
                  <el-icon><Link /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 支付场景 -->
        <el-divider content-position="left">
          <el-icon><Grid /></el-icon>
          支付场景
        </el-divider>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="支持场景">
              <el-checkbox-group v-model="alipayForm.scenarios">
                <el-checkbox label="app">APP支付</el-checkbox>
                <el-checkbox label="h5">H5支付</el-checkbox>
                <el-checkbox label="web">网页支付</el-checkbox>
                <el-checkbox label="mini">小程序支付</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { 
  Check, Connection, Setting, Lock, Link, Bell, Document,
  Promotion, Wallet, Shop, Cellphone, Grid
} from '@element-plus/icons-vue'
import { getServiceConfig, updateConfigBatch, testConnection } from '@/api/system/thirdparty'

const providers = ref<any[]>([])

// 配置项原始数据
const wechatConfigItems = ref<any[]>([])
const alipayConfigItems = ref<any[]>([])

const loadingWechat = ref(false)
const savingWechat = ref(false)
const testingWechat = ref(false)
const loadingAlipay = ref(false)
const savingAlipay = ref(false)
const testingAlipay = ref(false)

const wechatForm = ref<Record<string, any>>({})
const alipayForm = ref<Record<string, any>>({})

// 加载配置数据
const loadData = async () => {
  loadingWechat.value = true
  loadingAlipay.value = true
  
  try {
    const res = await getServiceConfig('payment') as any
    
    if (res.data?.providers) {
      providers.value = res.data.providers.map((p: any) => ({
        code: p.providerCode,
        name: p.providerName,
        icon: p.providerLogo,
        description: p.remark || '',
        enabled: p.isCurrent === 1
      }))
    }
    
    // 微信支付配置
    if (res.data?.configs?.wechat) {
      wechatConfigItems.value = res.data.configs.wechat
      const form: Record<string, any> = {}
      wechatConfigItems.value.forEach((item: any) => {
        const key = item.configKey
        const formKey = key.replace(/_([a-z])/g, (g: string) => g[1].toUpperCase())
        form[formKey] = item.configValue || item.defaultValue
      })
      wechatForm.value = form
    }
    
    // 支付宝配置
    if (res.data?.configs?.alipay) {
      alipayConfigItems.value = res.data.configs.alipay
      const form: Record<string, any> = {}
      alipayConfigItems.value.forEach((item: any) => {
        const key = item.configKey
        const formKey = key.replace(/_([a-z])/g, (g: string) => g[1].toUpperCase())
        form[formKey] = item.configValue || item.defaultValue
      })
      alipayForm.value = form
    }
  } catch (error) {
    console.error('加载配置失败:', error)
    ElMessage.error('加载配置失败')
  } finally {
    loadingWechat.value = false
    loadingAlipay.value = false
  }
}

const handleToggleProvider = (code: string) => {
  // 切换支付方式的启用状态
}

const handleCertChange = (file: any) => {
  // 处理证书上传
}

const handleSaveWechat = async () => {
  savingWechat.value = true
  try {
    const configs = wechatConfigItems.value.map((item: any) => {
      const key = item.configKey
      const formKey = key.replace(/_([a-z])/g, (g: string) => g[1].toUpperCase())
      return {
        configId: item.configId,
        configKey: key,
        configValue: wechatForm.value[formKey] || ''
      }
    })
    
    await updateConfigBatch(configs)
    
    ElNotification({
      title: '保存成功',
      message: '微信支付配置已保存',
      type: 'success'
    })
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    savingWechat.value = false
  }
}

const handleTestWechat = async () => {
  testingWechat.value = true
  try {
    const configs: Record<string, string> = {}
    wechatConfigItems.value.forEach((item: any) => {
      const key = item.configKey
      const formKey = key.replace(/_([a-z])/g, (g: string) => g[1].toUpperCase())
      configs[key] = wechatForm.value[formKey] || ''
    })
    
    const res = await testConnection('payment', 'wechat', configs) as any
    
    ElNotification({
      title: '测试成功',
      message: res.msg || '微信支付API连接正常',
      type: 'success'
    })
  } catch (error: any) {
    ElNotification({
      title: '测试失败',
      message: error.msg || '微信支付API异常',
      type: 'error'
    })
  } finally {
    testingWechat.value = false
  }
}

const handleSaveAlipay = async () => {
  savingAlipay.value = true
  try {
    const configs = alipayConfigItems.value.map((item: any) => {
      const key = item.configKey
      const formKey = key.replace(/_([a-z])/g, (g: string) => g[1].toUpperCase())
      return {
        configId: item.configId,
        configKey: key,
        configValue: alipayForm.value[formKey] || ''
      }
    })
    
    await updateConfigBatch(configs)
    
    ElNotification({
      title: '保存成功',
      message: '支付宝配置已保存',
      type: 'success'
    })
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    savingAlipay.value = false
  }
}

const handleTestAlipay = async () => {
  testingAlipay.value = true
  try {
    const configs: Record<string, string> = {}
    alipayConfigItems.value.forEach((item: any) => {
      const key = item.configKey
      const formKey = key.replace(/_([a-z])/g, (g: string) => g[1].toUpperCase())
      configs[key] = alipayForm.value[formKey] || ''
    })
    
    const res = await testConnection('payment', 'alipay', configs) as any
    
    ElNotification({
      title: '测试成功',
      message: res.msg || '支付宝API连接正常',
      type: 'success'
    })
  } catch (error: any) {
    ElNotification({
      title: '测试失败',
      message: error.msg || '支付宝API异常',
      type: 'error'
    })
  } finally {
    testingAlipay.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.payment-config-panel {
  .provider-list {
    margin-bottom: 20px;

    .provider-card {
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
        justify-content: space-between;
        gap: 12px;
        margin-bottom: 12px;

        .provider-info {
          flex: 1;
          display: flex;
          align-items: center;
          gap: 12px;
          
          h3 {
            margin: 0;
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

      .provider-status {
        display: flex;
        justify-content: flex-end;
      }
    }
  }

  .config-form-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        font-weight: 500;

        .wechat-icon {
          color: #07c160;
          font-size: 20px;
        }

        .alipay-icon {
          color: #1677ff;
          font-size: 20px;
        }
      }
      
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

      .cert-upload {
        width: 100%;
      }
    }
  }
}
</style>
