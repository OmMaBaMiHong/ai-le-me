<template>
  <div class="hongniang-wechat-group">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="82px">
      <el-form-item label="红娘姓名">
        <el-input v-model="queryParams.hongniangName" clearable placeholder="请输入红娘姓名" style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="群关键词">
        <el-input v-model="queryParams.keyword" clearable placeholder="群名/群主/微信" style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="城市">
        <el-input v-model="queryParams.city" clearable placeholder="城市" style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="群类型">
        <el-select v-model="queryParams.groupType" clearable style="width: 160px">
          <el-option v-for="item in groupTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="同步状态">
        <el-select v-model="queryParams.syncStatus" clearable style="width: 160px">
          <el-option v-for="item in syncStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="群标签">
        <el-input v-model="queryParams.tagKeyword" clearable placeholder="标签关键词" style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['hongniang:wechatGroup:add']">新增群</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="groupList">
      <el-table-column label="群ID" prop="id" width="80" align="center" />
      <el-table-column label="红娘" prop="hongniangName" min-width="120" />
      <el-table-column label="群名称" prop="groupName" min-width="180" />
      <el-table-column label="群类型" width="120">
        <template #default="{ row }">
          <el-tag>{{ groupTypeLabel(row.groupType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="接入方式" width="110">
        <template #default="{ row }">{{ providerTypeLabel(row.providerType) }}</template>
      </el-table-column>
      <el-table-column label="群主" min-width="150">
        <template #default="{ row }">
          <div>{{ row.ownerName || '-' }}</div>
          <div class="sub-text">{{ row.ownerWechat || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="160">
        <template #default="{ row }">
          <div class="tag-wrap">
            <el-tag v-for="item in parseTags(row.tagJson)" :key="item" class="mr8 mb8">{{ item }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="同步状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.syncStatus === 1 ? 'success' : row.syncStatus === 2 ? 'danger' : 'info'">{{ row.syncStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="用户数" prop="boundUserCount" width="88" align="center" />
      <el-table-column label="案件数" prop="boundCaseCount" width="88" align="center" />
      <el-table-column label="最后同步" prop="lastSyncTime" width="170" />
      <el-table-column label="操作" width="400" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="View" @click="handleDetail(row)">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleEdit(row)">修改</el-button>
          <el-button link type="primary" icon="User" @click="handleBindUsers(row)">绑定用户</el-button>
          <el-button link type="primary" icon="Connection" @click="handleBindCases(row)">绑定案件</el-button>
          <el-button link type="primary" icon="Refresh" @click="handleSync(row)">同步</el-button>
          <el-button link type="primary" icon="Promotion" @click="handleCreateTask(row)">任务</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="dialog.title" v-model="dialog.visible" width="760px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="98px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="红娘" prop="hongniangId">
              <el-select v-model="form.hongniangId" style="width: 100%" filterable clearable>
                <el-option v-for="item in hongniangList" :key="item.id" :label="`${item.hongniangName} (${item.phone || '-'})`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="群名称" prop="groupName">
              <el-input v-model="form.groupName" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="群类型" prop="groupType">
              <el-radio-group v-model="form.groupType">
                <el-radio :label="1">现有微信群</el-radio>
                <el-radio :label="2">企微客户群</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="接入方式" prop="providerType">
              <el-radio-group v-model="form.providerType">
                <el-radio :label="0">手工</el-radio>
                <el-radio :label="1">企业微信</el-radio>
                <el-radio :label="2">SCRM</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="群主名称">
              <el-input v-model="form.ownerName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="form.groupType === 2 && form.providerType === 1 ? '群主企微成员ID' : '群主微信'">
              <el-input v-model="form.ownerWechat" :placeholder="form.groupType === 2 && form.providerType === 1 ? '同步后会自动回填，也可手工填写企业微信成员 userid' : ''" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="城市">
              <el-input v-model="form.city" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用途">
              <el-input v-model="form.purpose" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="二维码">
              <el-input v-model="form.qrCodeUrl" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入群链接">
              <el-input v-model="form.joinLink" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="外部群ID" v-if="form.groupType === 2">
              <el-input v-model="form.externalGroupId" placeholder="企微客户群 chat_id / external_group_id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="群标签">
              <el-select v-model="tagValues" multiple filterable allow-create default-first-option style="width: 100%">
                <el-option v-for="item in tagValues" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="绑定群用户" v-model="userDialog.visible" width="620px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="选择用户">
          <el-select
            v-model="userDialog.userIds"
            multiple
            filterable
            remote
            reserve-keyword
            style="width: 100%"
            :remote-method="searchBindUsers"
            :loading="bindUserLoading"
          >
            <el-option v-for="item in bindUserOptions" :key="item.uid" :label="formatBindUser(item)" :value="item.uid" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitBindUsers">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="绑定群案件" v-model="caseDialog.visible" width="620px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="选择案件">
          <el-select
            v-model="caseDialog.caseIds"
            multiple
            filterable
            remote
            reserve-keyword
            style="width: 100%"
            :remote-method="searchBindCases"
            :loading="bindCaseLoading"
          >
            <el-option v-for="item in bindCaseOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="caseDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitBindCases">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="新建触达任务" v-model="taskDialog.visible" width="620px" append-to-body>
      <el-form ref="taskRef" :model="taskForm" :rules="taskRules" label-width="100px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="taskForm.taskName" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="发送范围">
          <el-select v-model="taskForm.sendScopeType" style="width: 100%">
            <el-option label="全群" :value="1" />
            <el-option label="标签" :value="2" />
            <el-option label="案件相关" :value="3" />
            <el-option label="指定用户" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容类型">
          <el-select v-model="taskForm.contentType" style="width: 100%">
            <el-option label="文本" :value="1" />
            <el-option label="图文" :value="2" />
            <el-option label="链接" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划时间">
          <el-date-picker v-model="taskForm.scheduleTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="任务内容">
          <el-input v-model="taskForm.contentPayload" type="textarea" :rows="5" :placeholder="taskPayloadPlaceholder(taskForm.contentType)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitTask">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="微信群详情" size="820px">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="群名称">{{ detailData.groupName }}</el-descriptions-item>
          <el-descriptions-item label="红娘">{{ detailData.hongniangName }}</el-descriptions-item>
          <el-descriptions-item label="群类型">{{ groupTypeLabel(detailData.groupType) }}</el-descriptions-item>
          <el-descriptions-item label="接入方式">{{ providerTypeLabel(detailData.providerType) }}</el-descriptions-item>
          <el-descriptions-item label="群主">{{ detailData.ownerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="群主微信">{{ detailData.ownerWechat || '-' }}</el-descriptions-item>
          <el-descriptions-item label="同步状态">
            <el-tag :type="detailData.syncStatus === 1 ? 'success' : detailData.syncStatus === 2 ? 'danger' : 'info'">{{ detailData.syncStatusLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最后同步">{{ detailData.lastSyncTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <div class="tag-wrap">
              <el-tag v-for="item in parseTags(detailData.tagJson)" :key="item" class="mr8 mb8">{{ item }}</el-tag>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-block">
          <div class="section-title">绑定用户</div>
          <el-table :data="detailData.users || []" size="small" border>
            <el-table-column label="用户ID" prop="userId" width="90" />
            <el-table-column label="姓名" prop="username" />
            <el-table-column label="手机号" prop="mobile" />
            <el-table-column label="入群时间" prop="joinTime" width="160" />
          </el-table>
        </div>

        <div class="section-block">
          <div class="section-title">绑定案件</div>
          <el-table :data="detailData.cases || []" size="small" border>
            <el-table-column label="案件ID" prop="caseId" width="90" />
            <el-table-column label="当前阶段" prop="currentStageLabel" />
          </el-table>
        </div>

        <div class="section-block">
          <div class="section-title">触达任务</div>
          <el-table :data="detailData.tasks || []" size="small" border>
            <el-table-column label="任务ID" prop="id" width="80" />
            <el-table-column label="任务名称" prop="taskName" />
            <el-table-column label="状态" prop="taskStatus" width="100">
              <template #default="{ row }">{{ taskStatusLabel(row.taskStatus) }}</template>
            </el-table-column>
            <el-table-column label="结果" prop="resultSummary" min-width="180" />
          </el-table>
        </div>

        <div class="section-block">
          <div class="section-title">执行记录</div>
          <el-table :data="executionList" size="small" border>
            <el-table-column label="执行ID" prop="id" width="86" />
            <el-table-column label="任务ID" prop="taskId" width="86" />
            <el-table-column label="执行状态" prop="executionStatusLabel" width="110" />
            <el-table-column label="三方任务ID" prop="providerTaskId" min-width="160" show-overflow-tooltip />
            <el-table-column label="结果摘要" prop="resultSummary" min-width="180" show-overflow-tooltip />
            <el-table-column label="执行时间" prop="executeTime" width="168" />
            <el-table-column label="完成时间" prop="finishTime" width="168" />
          </el-table>
        </div>

        <div class="section-block">
          <div class="section-title">最近日志</div>
          <el-table :data="touchLogList" size="small" border>
            <el-table-column label="日志ID" prop="id" width="80" />
            <el-table-column label="状态" prop="sendStatus" width="100">
              <template #default="{ row }">{{ touchLogStatusLabel(row.sendStatus) }}</template>
            </el-table-column>
            <el-table-column label="时间" prop="sentTime" width="170" />
            <el-table-column label="响应" prop="providerResponse" min-width="220" />
          </el-table>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, onMounted, reactive, ref } from 'vue';
import { ElForm } from 'element-plus';
import { getAllHongniangs } from '@/api/hongniang/hongniangInfo';
import { bindHongniangWechatGroupCases, bindHongniangWechatGroupUsers, createHongniangGroupTouchTask, createHongniangWechatGroup, getHongniangWechatGroup, listHongniangGroupTaskExecution, listHongniangGroupTouchLog, listHongniangWechatGroup, searchBindableWechatGroupCases, searchBindableWechatGroupUsers, syncHongniangWechatGroup, updateHongniangWechatGroup } from '@/api/hongniang/wechatGroup';

const { proxy } = getCurrentInstance() as any;
type FormInstance = InstanceType<typeof ElForm>;

const queryRef = ref<FormInstance>();
const formRef = ref<FormInstance>();
const taskRef = ref<FormInstance>();

const loading = ref(false);
const submitLoading = ref(false);
const showSearch = ref(true);
const total = ref(0);
const groupList = ref<any[]>([]);
const hongniangList = ref<any[]>([]);
const bindUserOptions = ref<any[]>([]);
const bindCaseOptions = ref<any[]>([]);
const touchLogList = ref<any[]>([]);
const executionList = ref<any[]>([]);
const bindUserLoading = ref(false);
const bindCaseLoading = ref(false);
const detailVisible = ref(false);
const detailData = ref<any>(null);
const tagValues = ref<string[]>([]);

const groupTypeOptions = [
  { label: '现有微信群', value: 1 },
  { label: '企微客户群', value: 2 }
];

const syncStatusOptions = [
  { label: '未同步', value: 0 },
  { label: '已同步', value: 1 },
  { label: '同步失败', value: 2 }
];

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  hongniangName: undefined as string | undefined,
  keyword: undefined as string | undefined,
  city: undefined as string | undefined,
  groupType: undefined as number | undefined,
  syncStatus: undefined as number | undefined,
  tagKeyword: undefined as string | undefined
});

const dialog = reactive({
  visible: false,
  title: '新增微信群'
});

const form = reactive<any>({
  id: undefined,
  hongniangId: undefined,
  groupName: '',
  groupType: 1,
  providerType: 0,
  ownerName: '',
  ownerWechat: '',
  tagJson: '',
  city: '',
  purpose: '',
  qrCodeUrl: '',
  joinLink: '',
  externalGroupId: '',
  remark: ''
});

const userDialog = reactive<any>({
  visible: false,
  groupId: undefined,
  hongniangId: undefined,
  userIds: []
});

const caseDialog = reactive<any>({
  visible: false,
  groupId: undefined,
  hongniangId: undefined,
  caseIds: []
});

const taskDialog = reactive({ visible: false });
const taskForm = reactive<any>({
  groupId: undefined,
  taskName: '',
  sendScopeType: 1,
  contentType: 1,
  contentPayload: '',
  scheduleTime: undefined
});

const rules = reactive({
  hongniangId: [{ required: true, message: '请选择红娘', trigger: 'change' }],
  groupName: [{ required: true, message: '请填写群名称', trigger: 'blur' }]
});

const taskRules = reactive({
  taskName: [{ required: true, message: '请填写任务名称', trigger: 'blur' }]
});

const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listHongniangWechatGroup(queryParams);
    groupList.value = res.page?.list || [];
    total.value = res.page?.totalCount || 0;
  } finally {
    loading.value = false;
  }
};

const loadHongniangs = async () => {
  const res: any = await getAllHongniangs();
  hongniangList.value = res.page?.list || [];
};

const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryRef.value?.resetFields();
  handleQuery();
};

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    hongniangId: undefined,
    groupName: '',
    groupType: 1,
    providerType: 0,
    ownerName: '',
    ownerWechat: '',
    tagJson: '',
    city: '',
    purpose: '',
    qrCodeUrl: '',
    joinLink: '',
    externalGroupId: '',
    remark: ''
  });
  tagValues.value = [];
};

const handleAdd = async () => {
  resetForm();
  await loadHongniangs();
  dialog.title = '新增微信群';
  dialog.visible = true;
};

const handleEdit = async (row: any) => {
  resetForm();
  await loadHongniangs();
  const res: any = await getHongniangWechatGroup(row.id);
  const data = res.data || {};
  Object.assign(form, {
    id: data.id,
    hongniangId: data.hongniangId,
    groupName: data.groupName,
    groupType: data.groupType,
    providerType: data.providerType,
    ownerName: data.ownerName,
    ownerWechat: data.ownerWechat,
    tagJson: data.tagJson,
    city: data.city,
    purpose: data.purpose,
    qrCodeUrl: data.qrCodeUrl,
    joinLink: data.joinLink,
    externalGroupId: data.externalGroupId,
    remark: data.remark
  });
  tagValues.value = parseTags(data.tagJson);
  dialog.title = '修改微信群';
  dialog.visible = true;
};

const submitForm = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    submitLoading.value = true;
    try {
      form.tagJson = JSON.stringify(tagValues.value || []);
      if (form.id) {
        await updateHongniangWechatGroup(form);
        proxy.$modal.msgSuccess('修改成功');
      } else {
        await createHongniangWechatGroup(form);
        proxy.$modal.msgSuccess('创建成功');
      }
      dialog.visible = false;
      await getList();
    } finally {
      submitLoading.value = false;
    }
  });
};

const handleDetail = async (row: any) => {
  const [detailRes, logRes, executionRes]: any = await Promise.all([
    getHongniangWechatGroup(row.id),
    listHongniangGroupTouchLog({ pageNum: 1, pageSize: 20, groupId: row.id }),
    listHongniangGroupTaskExecution({ groupId: row.id, limit: 20 })
  ]);
  detailData.value = detailRes.data || {};
  touchLogList.value = logRes.page?.list || [];
  executionList.value = executionRes.list || [];
  detailVisible.value = true;
};

const handleBindUsers = async (row: any) => {
  userDialog.groupId = row.id;
  userDialog.hongniangId = row.hongniangId;
  const res: any = await getHongniangWechatGroup(row.id);
  userDialog.userIds = (res.data?.users || []).map((item: any) => item.userId);
  bindUserOptions.value = [];
  userDialog.visible = true;
  await searchBindUsers('');
};

const searchBindUsers = async (keyword: string) => {
  if (!userDialog.hongniangId) return;
  bindUserLoading.value = true;
  try {
    const res: any = await searchBindableWechatGroupUsers({
      hongniangId: userDialog.hongniangId,
      groupId: userDialog.groupId,
      keyword: keyword?.trim() || undefined,
      limit: 30
    });
    bindUserOptions.value = mergeOptions(bindUserOptions.value, res.list || [], 'uid');
  } finally {
    bindUserLoading.value = false;
  }
};

const submitBindUsers = async () => {
  await bindHongniangWechatGroupUsers({
    groupId: userDialog.groupId,
    userIds: userDialog.userIds
  });
  proxy.$modal.msgSuccess('绑定用户成功');
  userDialog.visible = false;
  await getList();
  if (detailVisible.value && detailData.value?.id === userDialog.groupId) {
    await handleDetail({ id: userDialog.groupId });
  }
};

const handleBindCases = async (row: any) => {
  caseDialog.groupId = row.id;
  caseDialog.hongniangId = row.hongniangId;
  const res: any = await getHongniangWechatGroup(row.id);
  caseDialog.caseIds = (res.data?.cases || []).map((item: any) => item.caseId);
  bindCaseOptions.value = [];
  caseDialog.visible = true;
  await searchBindCases('');
};

const searchBindCases = async (keyword: string) => {
  if (!caseDialog.hongniangId) return;
  bindCaseLoading.value = true;
  try {
    const res: any = await searchBindableWechatGroupCases({
      hongniangId: caseDialog.hongniangId,
      groupId: caseDialog.groupId,
      keyword: keyword?.trim() || undefined,
      limit: 30
    });
    bindCaseOptions.value = mergeOptions(bindCaseOptions.value, res.list || [], 'id');
  } finally {
    bindCaseLoading.value = false;
  }
};

const submitBindCases = async () => {
  await bindHongniangWechatGroupCases({
    groupId: caseDialog.groupId,
    caseIds: caseDialog.caseIds
  });
  proxy.$modal.msgSuccess('绑定案件成功');
  caseDialog.visible = false;
  await getList();
  if (detailVisible.value && detailData.value?.id === caseDialog.groupId) {
    await handleDetail({ id: caseDialog.groupId });
  }
};

const handleSync = async (row: any) => {
  const res: any = await syncHongniangWechatGroup(row.id);
  proxy.$modal.msgSuccess(res.data?.msg || '同步请求已提交');
  await getList();
  if (detailVisible.value && detailData.value?.id === row.id) {
    await handleDetail(row);
  }
};

const handleCreateTask = (row: any) => {
  Object.assign(taskForm, {
    groupId: row.id,
    taskName: '',
    sendScopeType: 1,
    contentType: 1,
    contentPayload: '',
    scheduleTime: undefined
  });
  taskDialog.visible = true;
};

const submitTask = () => {
  taskRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    await createHongniangGroupTouchTask(taskForm);
    proxy.$modal.msgSuccess('任务已创建');
    taskDialog.visible = false;
    await getList();
    if (detailVisible.value && detailData.value?.id === taskForm.groupId) {
      await handleDetail({ id: taskForm.groupId });
    }
  });
};

const groupTypeLabel = (value?: number) => groupTypeOptions.find(item => item.value === value)?.label || '-';
const providerTypeLabel = (value?: number) => {
  if (value === 0) return '手工';
  if (value === 1) return '企业微信';
  if (value === 2) return 'SCRM';
  return '-';
};
const taskStatusLabel = (value?: number) => {
  if (value === 0) return '待发送';
  if (value === 1) return '发送中';
  if (value === 2) return '已完成';
  if (value === 3) return '失败';
  if (value === 4) return '仅登记';
  return '-';
};
const taskPayloadPlaceholder = (contentType?: number) => {
  if (contentType === 1) {
    return '文本消息可直接填写文案；如需指定 sender，可填 JSON：{\"sender\":\"zhangsan\",\"content\":\"今晚八点线上破冰\"}';
  }
  if (contentType === 2) {
    return '图文消息请填 JSON：{\"title\":\"活动通知\",\"desc\":\"今晚八点线上破冰\",\"url\":\"https://...\",\"picUrl\":\"https://...\"}';
  }
  if (contentType === 3) {
    return '链接消息可直接填写 URL，或填 JSON：{\"title\":\"查看详情\",\"url\":\"https://...\",\"desc\":\"点击查看\"}';
  }
  return '支持直接录入文案，必要时也可填写 JSON';
};
const touchLogStatusLabel = (value?: number) => {
  if (value === 1) return '成功';
  if (value === 2) return '失败';
  if (value === 3) return '仅登记';
  return '待处理';
};

const parseTags = (value?: string) => {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return value.split(',').map(item => item.trim()).filter(Boolean);
  }
};

const formatBindUser = (item: any) => `${item.username || '未命名'} | UID:${item.uid} | ${item.mobile || '-'} | 编号:${item.hongniangUserNo || '-'}`;
const mergeOptions = (origin: any[], incoming: any[], key: string) => {
  const map = new Map<string | number, any>();
  [...origin, ...incoming].forEach(item => {
    map.set(item[key], item);
  });
  return Array.from(map.values());
};

onMounted(async () => {
  await Promise.all([loadHongniangs(), getList()]);
});
</script>

<style scoped>
.hongniang-wechat-group {
  padding: 20px;
}

.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.section-block {
  margin-top: 24px;
}

.section-title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
}

.mr8 {
  margin-right: 8px;
}

.mb8 {
  margin-bottom: 8px;
}
</style>
