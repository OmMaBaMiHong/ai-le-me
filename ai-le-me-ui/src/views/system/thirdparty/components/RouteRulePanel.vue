<template>
  <div class="route-rule-panel">
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <div>
            <div class="title">路由规则管理</div>
            <div class="subtitle">统一维护 AI / 图片 / 视频等服务的 provider 路由优先级</div>
          </div>
          <div class="actions">
            <el-select v-model="query.serviceType" placeholder="全部服务" clearable style="width: 160px" @change="loadRules">
              <el-option v-for="item in serviceTypes" :key="item" :label="serviceLabel(item)" :value="item" />
            </el-select>
            <el-button type="primary" @click="openDialog()">新增规则</el-button>
            <el-button @click="loadRules">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="rules" v-loading="loading" border>
        <el-table-column label="服务类型" prop="serviceType" width="120">
          <template #default="{ row }">{{ serviceLabel(row.serviceType) }}</template>
        </el-table-column>
        <el-table-column label="场景" prop="sceneCode" min-width="140" />
        <el-table-column label="模板" prop="templateCode" min-width="120" />
        <el-table-column label="内容模式" prop="contentMode" min-width="120" />
        <el-table-column label="功能类型" prop="functionType" min-width="150" />
        <el-table-column label="服务商" prop="providerCode" width="140" />
        <el-table-column label="Profile" prop="profileCode" min-width="140" />
        <el-table-column label="优先级" prop="priority" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'danger'">{{ row.isEnabled === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新增路由规则' : '编辑路由规则'" width="720px">
      <el-form ref="formRef" :model="form" :rules="rulesForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="服务类型" prop="serviceType">
              <el-select v-model="form.serviceType" style="width: 100%" @change="handleServiceTypeChange">
                <el-option v-for="item in serviceTypes" :key="item" :label="serviceLabel(item)" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务商" prop="providerCode">
              <el-select v-model="form.providerCode" style="width: 100%">
                <el-option v-for="item in providerOptions" :key="item.providerCode" :label="item.providerName" :value="item.providerCode" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景编码">
              <el-input v-model="form.sceneCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板编码">
              <el-input v-model="form.templateCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内容模式">
              <el-input v-model="form.contentMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="功能类型">
              <el-input v-model="form.functionType" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Profile">
              <el-input v-model="form.profileCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-input-number v-model="form.priority" :min="1" :max="999" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.isEnabled">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="匹配条件JSON">
              <el-input v-model="form.matchJson" type="textarea" :rows="4" placeholder='如 {"gender":"female"}' />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { addRouteRule, deleteRouteRule, listProviders, listRouteRules, updateRouteRule } from '@/api/system/thirdparty';
import type { ThirdPartyProvider, ThirdPartyRouteRule } from '@/api/system/thirdparty';

const props = defineProps<{ serviceTypes: string[] }>();

const loading = ref(false);
const formRef = ref<FormInstance>();
const rules = ref<ThirdPartyRouteRule[]>([]);
const providerCache = reactive<Record<string, ThirdPartyProvider[]>>({});
const query = reactive({ serviceType: '' });
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit' });

const form = reactive<ThirdPartyRouteRule>({
  serviceType: 'video',
  providerCode: '',
  sceneCode: '',
  templateCode: '',
  contentMode: '',
  functionType: '',
  profileCode: '',
  matchJson: '',
  priority: 100,
  isEnabled: 1,
  remark: ''
});

const rulesForm: FormRules = {
  serviceType: [{ required: true, message: '请选择服务类型', trigger: 'change' }],
  providerCode: [{ required: true, message: '请选择服务商', trigger: 'change' }]
};

const providerOptions = computed(() => providerCache[form.serviceType] || []);

const serviceLabel = (serviceType: string) => {
  const map: Record<string, string> = {
    ai: 'AI对话',
    image: '图片生成',
    video: '视频生成',
    oss: '云存储',
    sms: '短信',
    payment: '支付',
    realname: '实名认证',
    map: '地图',
    push: '推送',
    education: '学历认证',
    wechat_mini: '微信小程序',
    wechat_mp: '微信公众号',
    wechat_app: '微信开放平台'
  };
  return map[serviceType] || serviceType;
};

const loadProviders = async (serviceType: string) => {
  if (!serviceType || providerCache[serviceType]) {
    return;
  }
  const response: any = await listProviders(serviceType);
  providerCache[serviceType] = response.data || [];
};

const loadRules = async () => {
  loading.value = true;
  try {
    const response: any = await listRouteRules(query.serviceType || undefined);
    rules.value = response.data || [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  Object.assign(form, {
    routeRuleId: undefined,
    serviceType: query.serviceType || props.serviceTypes[0] || 'video',
    providerCode: '',
    sceneCode: '',
    templateCode: '',
    contentMode: '',
    functionType: '',
    profileCode: '',
    matchJson: '',
    priority: 100,
    isEnabled: 1,
    remark: ''
  });
  formRef.value?.clearValidate();
};

const handleServiceTypeChange = async () => {
  await loadProviders(form.serviceType);
  if (!providerOptions.value.some((item) => item.providerCode === form.providerCode)) {
    form.providerCode = providerOptions.value[0]?.providerCode || '';
  }
};

const openDialog = async (row?: ThirdPartyRouteRule) => {
  dialog.visible = true;
  dialog.mode = row ? 'edit' : 'create';
  if (row) {
    Object.assign(form, { ...row });
  } else {
    resetForm();
  }
  await handleServiceTypeChange();
};

const submit = async () => {
  await formRef.value?.validate();
  if (dialog.mode === 'create') {
    await addRouteRule({ ...form });
    ElMessage.success('路由规则已新增');
  } else if (form.routeRuleId) {
    await updateRouteRule(form.routeRuleId, { ...form });
    ElMessage.success('路由规则已更新');
  }
  dialog.visible = false;
  await loadRules();
};

const handleDelete = async (row: ThirdPartyRouteRule) => {
  if (!row.routeRuleId) {
    return;
  }
  await ElMessageBox.confirm('确定删除这条路由规则吗？', '删除确认', { type: 'warning' });
  await deleteRouteRule(row.routeRuleId);
  ElMessage.success('路由规则已删除');
  await loadRules();
};

onMounted(async () => {
  await Promise.all(props.serviceTypes.slice(0, 3).map((item) => loadProviders(item)));
  await loadRules();
});
</script>

<style scoped lang="scss">
.route-rule-panel {
  .table-card {
    border-radius: 18px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
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
    align-items: center;
  }
}
</style>
