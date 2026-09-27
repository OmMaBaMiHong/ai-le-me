<template>
  <div class="quartz-job-page">
    <el-row :gutter="16" class="summary-row">
      <el-col :xs="24" :sm="8">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">任务总数</div>
          <div class="summary-value">{{ total }}</div>
          <div class="summary-tip">已接入 Quartz 的业务任务</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">启用中</div>
          <div class="summary-value success">{{ enabledCount }}</div>
          <div class="summary-tip">当前页已启用任务</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">暂停中</div>
          <div class="summary-value warning">{{ pausedCount }}</div>
          <div class="summary-tip">当前页已暂停任务</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <div class="title-block">
          <div class="page-title">Quartz 任务管理</div>
          <div class="page-subtitle">统一管理任务定义、启停状态、立即执行与运行日志，当前直接对接业务库 `sys_quartz_job`。</div>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            新增任务
          </el-button>
          <el-button :disabled="selectedIds.length === 0" @click="handleBatchDelete">
            <el-icon><Delete /></el-icon>
            删除选中
          </el-button>
          <el-button @click="loadJobs">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>

      <div class="robot-ops-panel">
        <div class="robot-ops-head">
          <div>
            <div class="robot-ops-title">机器人造数</div>
            <div class="robot-ops-subtitle">手动补充用户与内容数据，并维护默认暂停的单条生成任务。</div>
          </div>
          <el-tag type="warning" effect="dark">批量任务默认手动触发</el-tag>
        </div>

        <div class="robot-ops-body">
          <el-form :inline="true" :model="robotForm" class="robot-form">
            <el-form-item label="用户数">
              <el-input-number v-model="robotForm.userCount" :min="1" :max="500" />
            </el-form-item>
            <el-form-item label="每人最少动态">
              <el-input-number v-model="robotForm.minPostsPerUser" :min="1" :max="10" />
            </el-form-item>
            <el-form-item label="每人最多动态">
              <el-input-number v-model="robotForm.maxPostsPerUser" :min="1" :max="10" />
            </el-form-item>
          </el-form>

          <div class="robot-ops-actions">
            <el-button type="primary" :loading="robotActionLoading.batch" @click="handleRobotBatchSeed">
              一键批量造数
            </el-button>
            <el-button :loading="robotActionLoading.single" @click="handleRobotSinglePost">
              生成单条内容
            </el-button>
            <el-button :loading="robotActionLoading.ensure" @click="handleEnsureRobotJob">
              同步默认任务
            </el-button>
          </div>
        </div>

        <div v-if="robotLastResult" class="robot-ops-result">
          {{ robotLastResult }}
        </div>
      </div>

      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.key"
            placeholder="任务名称 / 编码 / 分组"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="启用" value="1" />
            <el-option label="暂停" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="jobList"
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column label="任务名称" min-width="180">
          <template #default="{ row }">
            <div class="job-name">{{ row.jobName }}</div>
            <div class="job-meta">{{ row.jobCode }}</div>
          </template>
        </el-table-column>
        <el-table-column label="分组" prop="jobGroup" width="110" align="center" />
        <el-table-column label="Cron" prop="cronExpression" min-width="170" />
        <el-table-column label="并发" width="92" align="center">
          <template #default="{ row }">
            <el-tag :type="row.allowConcurrent === 1 ? 'success' : 'info'">
              {{ row.allowConcurrent === 1 ? '允许' : '串行' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              inline-prompt
              active-text="启用"
              inactive-text="暂停"
              @change="(value) => handleStatusChange(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="下次执行" min-width="160">
          <template #default="{ row }">
            {{ row.nextFireTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="上次执行" min-width="160">
          <template #default="{ row }">
            {{ row.previousFireTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="280" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="primary" @click="handleRunOnce(row)">执行一次</el-button>
            <el-button link type="primary" @click="openLogs(row)">日志</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="loadJobs"
      />
    </el-card>

    <el-dialog
      v-model="dialog.visible"
      :title="dialog.mode === 'create' ? '新增 Quartz 任务' : '编辑 Quartz 任务'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="任务名称" prop="jobName">
              <el-input v-model="form.jobName" placeholder="例如：AI视频任务轮询" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务分组" prop="jobGroup">
              <el-input v-model="form.jobGroup" placeholder="例如：AI / SYSTEM" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务编码" prop="jobCode">
              <el-select v-model="form.jobCode" placeholder="请选择已注册任务编码" filterable style="width: 100%">
                <el-option
                  v-for="handler in handlers"
                  :key="handler.jobCode"
                  :label="`${handler.jobName} (${handler.jobCode})`"
                  :value="handler.jobCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Cron表达式" prop="cronExpression">
              <el-input v-model="form.cronExpression" placeholder="例如：0/10 * * * * ?" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="并发执行" prop="allowConcurrent">
              <el-radio-group v-model="form.allowConcurrent">
                <el-radio :value="0">串行</el-radio>
                <el-radio :value="1">允许并发</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="默认状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">暂停</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="任务参数" prop="jobParams">
              <el-input
                v-model="form.jobParams"
                type="textarea"
                :rows="5"
                placeholder='请输入 JSON 字符串，留空可填 {}'
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="3"
                placeholder="描述任务作用、风险点或值班提示"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div v-if="currentHandler" class="handler-tip">
        <div class="handler-name">{{ currentHandler.jobName }}</div>
        <div class="handler-desc">{{ currentHandler.description || '该任务由后端注册表提供，保存后将自动同步到 Quartz。' }}</div>
      </div>

      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="logDrawer.visible" size="60%" destroy-on-close>
      <template #header>
        <div class="drawer-title">执行日志{{ logDrawer.jobName ? ` · ${logDrawer.jobName}` : '' }}</div>
      </template>

      <el-table v-loading="logDrawer.loading" :data="logDrawer.list" border>
        <el-table-column label="开始时间" prop="startTime" min-width="160" />
        <el-table-column label="结束时间" prop="endTime" min-width="160" />
        <el-table-column label="耗时" width="90" align="center">
          <template #default="{ row }">
            {{ row.durationMs != null ? `${row.durationMs}ms` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.executeStatus === 1 ? 'success' : 'danger'">
              {{ row.executeStatus === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="执行摘要" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.resultSummary || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="错误信息" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.errorMessage || '-' }}
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="logDrawer.total > 0"
        v-model:page="logDrawer.query.pageNum"
        v-model:limit="logDrawer.query.pageSize"
        :total="logDrawer.total"
        @pagination="loadLogs"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Plus, Refresh } from '@element-plus/icons-vue';
import {
  changeQuartzJobStatus,
  deleteQuartzJobs,
  ensureRobotSinglePostJob,
  getQuartzJobInfo,
  listQuartzJobHandlers,
  listQuartzJobLogs,
  listQuartzJobs,
  runRobotBatchSeed,
  runRobotSinglePost,
  runQuartzJobOnce,
  saveQuartzJob,
  updateQuartzJob
} from '@/api/shejiao/quartzJob';
import type {
  QuartzJobHandlerVO,
  QuartzJobLogVO,
  QuartzJobQuery,
  QuartzJobVO
} from '@/api/shejiao/quartzJob/types';

type DialogMode = 'create' | 'edit';

const createEmptyForm = (): QuartzJobVO => ({
  jobName: '',
  jobGroup: 'SYSTEM',
  jobCode: '',
  cronExpression: '',
  jobParams: '{}',
  allowConcurrent: 0,
  status: 1,
  remark: ''
});

const loading = ref(false);
const total = ref(0);
const jobList = ref<QuartzJobVO[]>([]);
const selectedIds = ref<number[]>([]);
const handlers = ref<QuartzJobHandlerVO[]>([]);
const formRef = ref<FormInstance>();
const robotLastResult = ref('');

const queryParams = reactive<QuartzJobQuery>({
  pageNum: 1,
  pageSize: 10,
  key: '',
  status: ''
});

const dialog = reactive({
  visible: false,
  mode: 'create' as DialogMode,
  submitting: false
});

const robotForm = reactive({
  userCount: 100,
  minPostsPerUser: 1,
  maxPostsPerUser: 2
});

const robotActionLoading = reactive({
  batch: false,
  single: false,
  ensure: false
});

const form = reactive<QuartzJobVO>(createEmptyForm());

const logDrawer = reactive({
  visible: false,
  loading: false,
  total: 0,
  jobName: '',
  list: [] as QuartzJobLogVO[],
  query: {
    pageNum: 1,
    pageSize: 10,
    jobId: undefined as number | undefined
  }
});

const rules: FormRules = {
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  jobGroup: [{ required: true, message: '请输入任务分组', trigger: 'blur' }],
  jobCode: [{ required: true, message: '请选择任务编码', trigger: 'change' }],
  cronExpression: [{ required: true, message: '请输入 Cron 表达式', trigger: 'blur' }],
  jobParams: [
    {
      validator: (_rule, value, callback) => {
        const content = String(value || '').trim();
        if (!content) {
          callback();
          return;
        }
        try {
          JSON.parse(content);
          callback();
        } catch (error) {
          callback(new Error('任务参数必须是合法 JSON'));
        }
      },
      trigger: 'blur'
    }
  ]
};

const currentHandler = computed(() => handlers.value.find((item) => item.jobCode === form.jobCode));
const enabledCount = computed(() => jobList.value.filter((item) => item.status === 1).length);
const pausedCount = computed(() => jobList.value.filter((item) => item.status === 0).length);

const resetForm = () => {
  Object.assign(form, createEmptyForm());
  formRef.value?.clearValidate();
};

const applyHandlerDefaults = (handler?: QuartzJobHandlerVO) => {
  if (!handler) {
    return;
  }
  if (!form.jobName) {
    form.jobName = handler.jobName || '';
  }
  if (!form.jobGroup) {
    form.jobGroup = handler.defaultJobGroup || 'SYSTEM';
  }
  if (!form.cronExpression) {
    form.cronExpression = handler.defaultCronExpression || '';
  }
  if (form.allowConcurrent == null) {
    form.allowConcurrent = handler.defaultAllowConcurrent ?? 0;
  }
};

const loadHandlers = async () => {
  const response = (await listQuartzJobHandlers()) as any;
  handlers.value = response.list || [];
};

const loadJobs = async () => {
  loading.value = true;
  try {
    const response = (await listQuartzJobs(queryParams)) as any;
    jobList.value = response.page?.list || [];
    total.value = response.page?.totalCount || 0;
  } catch (error) {
    console.error('加载 Quartz 任务列表失败:', error);
    ElMessage.error('加载 Quartz 任务列表失败');
  } finally {
    loading.value = false;
  }
};

const loadLogs = async () => {
  if (!logDrawer.query.jobId) {
    return;
  }
  logDrawer.loading = true;
  try {
    const response = (await listQuartzJobLogs(logDrawer.query)) as any;
    logDrawer.list = response.page?.list || [];
    logDrawer.total = response.page?.totalCount || 0;
  } catch (error) {
    console.error('加载任务日志失败:', error);
    ElMessage.error('加载任务日志失败');
  } finally {
    logDrawer.loading = false;
  }
};

const handleQuery = () => {
  queryParams.pageNum = 1;
  loadJobs();
};

const handleReset = () => {
  queryParams.key = '';
  queryParams.status = '';
  queryParams.pageNum = 1;
  loadJobs();
};

const handleSelectionChange = (rows: QuartzJobVO[]) => {
  selectedIds.value = rows.map((item) => Number(item.id)).filter(Boolean);
};

const openCreateDialog = () => {
  dialog.mode = 'create';
  dialog.visible = true;
  resetForm();
};

const openEditDialog = async (row: QuartzJobVO) => {
  if (!row.id) {
    return;
  }
  dialog.mode = 'edit';
  dialog.visible = true;
  resetForm();
  try {
    const response = (await getQuartzJobInfo(row.id)) as any;
    Object.assign(form, response.job || createEmptyForm());
    form.jobParams = form.jobParams || '{}';
  } catch (error) {
    console.error('加载任务详情失败:', error);
    ElMessage.error('加载任务详情失败');
  }
};

const submitForm = async () => {
  await formRef.value?.validate();
  dialog.submitting = true;
  try {
    const payload = {
      ...form,
      jobParams: String(form.jobParams || '{}').trim() || '{}'
    };
    if (dialog.mode === 'create') {
      await saveQuartzJob(payload);
      ElMessage.success('任务创建成功');
    } else {
      await updateQuartzJob(payload);
      ElMessage.success('任务更新成功');
    }
    dialog.visible = false;
    await loadJobs();
  } catch (error) {
    console.error('保存 Quartz 任务失败:', error);
    ElMessage.error('保存 Quartz 任务失败');
  } finally {
    dialog.submitting = false;
  }
};

const handleDelete = async (row: QuartzJobVO) => {
  if (!row.id) {
    return;
  }
  try {
    await ElMessageBox.confirm(`确定删除任务「${row.jobName}」吗？`, '删除确认', {
      type: 'warning'
    });
    await deleteQuartzJobs([row.id]);
    ElMessage.success('任务已删除');
    await loadJobs();
  } catch (error) {
    // ignore cancel
  }
};

const handleBatchDelete = async () => {
  if (!selectedIds.value.length) {
    return;
  }
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个任务吗？`, '批量删除确认', {
      type: 'warning'
    });
    await deleteQuartzJobs(selectedIds.value);
    ElMessage.success('已删除选中任务');
    selectedIds.value = [];
    await loadJobs();
  } catch (error) {
    // ignore cancel
  }
};

const handleStatusChange = async (row: QuartzJobVO, enabled: boolean | string | number) => {
  if (!row.id) {
    return;
  }
  const nextStatus = enabled ? 1 : 0;
  try {
    await changeQuartzJobStatus(row.id, nextStatus);
    row.status = nextStatus;
    ElMessage.success(nextStatus === 1 ? '任务已启用' : '任务已暂停');
    await loadJobs();
  } catch (error) {
    console.error('切换任务状态失败:', error);
    ElMessage.error('切换任务状态失败');
    await loadJobs();
  }
};

const handleRunOnce = async (row: QuartzJobVO) => {
  if (!row.id) {
    return;
  }
  try {
    await ElMessageBox.confirm(`立即执行一次「${row.jobName}」？`, '执行确认', {
      type: 'warning'
    });
    await runQuartzJobOnce(row.id);
    ElMessage.success('任务已触发执行');
    await loadJobs();
    logDrawer.query.jobId = row.id;
    logDrawer.jobName = row.jobName;
    logDrawer.visible = true;
    logDrawer.query.pageNum = 1;
    await loadLogs();
  } catch (error) {
    // ignore cancel
  }
};

const openLogs = async (row: QuartzJobVO) => {
  logDrawer.visible = true;
  logDrawer.jobName = row.jobName;
  logDrawer.query.jobId = Number(row.id);
  logDrawer.query.pageNum = 1;
  await loadLogs();
};

const handleEnsureRobotJob = async () => {
  robotActionLoading.ensure = true;
  try {
    const response = (await ensureRobotSinglePostJob()) as any;
    const result = response.data || {};
    robotLastResult.value = `默认任务已同步：jobId=${result.jobId || '-'}，状态=${result.status === 1 ? '启用' : '暂停'}，Cron=${result.cronExpression || '-'}`;
    ElMessage.success(result.created ? '默认任务已创建' : '默认任务已同步');
    await loadJobs();
  } catch (error) {
    console.error('同步机器人默认任务失败:', error);
    ElMessage.error('同步机器人默认任务失败');
  } finally {
    robotActionLoading.ensure = false;
  }
};

const handleRobotSinglePost = async () => {
  robotActionLoading.single = true;
  try {
    const response = (await runRobotSinglePost(true)) as any;
    const result = response.data || {};
    robotLastResult.value = `已生成单条机器人内容：用户 ${result.username || result.uid || '-'}，帖子ID ${result.postId || '-'}`;
    ElMessage.success('单条机器人内容已生成');
  } catch (error) {
    console.error('生成单条机器人内容失败:', error);
    ElMessage.error('生成单条机器人内容失败');
  } finally {
    robotActionLoading.single = false;
  }
};

const handleRobotBatchSeed = async () => {
  if (robotForm.maxPostsPerUser < robotForm.minPostsPerUser) {
    ElMessage.warning('每人最多动态不能小于最少动态');
    return;
  }
  robotActionLoading.batch = true;
  try {
    const response = (await runRobotBatchSeed(
      robotForm.userCount,
      robotForm.minPostsPerUser,
      robotForm.maxPostsPerUser
    )) as any;
    const result = response.data || {};
    robotLastResult.value = `批量造数完成：新增 ${result.usersCreated || 0} 个机器人用户，新增 ${result.postsCreated || 0} 条动态。`;
    ElMessage.success('批量造数已完成');
  } catch (error) {
    console.error('批量机器人造数失败:', error);
    ElMessage.error('批量机器人造数失败');
  } finally {
    robotActionLoading.batch = false;
  }
};

onMounted(async () => {
  await loadHandlers();
  await loadJobs();
});

watch(
  () => form.jobCode,
  (jobCode) => {
    if (dialog.mode !== 'create' || !jobCode) {
      return;
    }
    applyHandlerDefaults(handlers.value.find((item) => item.jobCode === jobCode));
  }
);
</script>

<style scoped lang="scss">
.quartz-job-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .summary-row {
    margin-bottom: 0;
  }

  .summary-card,
  .search-card,
  .table-card {
    border-radius: 20px;
  }

  .summary-card {
    .summary-label {
      font-size: 13px;
      color: #6b7280;
    }

    .summary-value {
      margin-top: 10px;
      font-size: 30px;
      font-weight: 700;
      color: #111827;

      &.success {
        color: #059669;
      }

      &.warning {
        color: #d97706;
      }
    }

    .summary-tip {
      margin-top: 8px;
      font-size: 12px;
      color: #94a3b8;
    }
  }

  .toolbar {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
    margin-bottom: 14px;
  }

  .page-title {
    font-size: 20px;
    font-weight: 700;
    color: #111827;
  }

  .page-subtitle {
    margin-top: 6px;
    font-size: 13px;
    line-height: 1.6;
    color: #6b7280;
  }

  .toolbar-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .search-form {
    margin-bottom: -18px;
  }

  .robot-ops-panel {
    margin-bottom: 18px;
    padding: 18px 20px;
    border-radius: 20px;
    background:
      radial-gradient(circle at top left, rgba(59, 130, 246, 0.14), transparent 36%),
      radial-gradient(circle at top right, rgba(16, 185, 129, 0.16), transparent 34%),
      linear-gradient(135deg, #f8fbff 0%, #f5fff8 100%);
    border: 1px solid #dbeafe;
  }

  .robot-ops-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 14px;
    flex-wrap: wrap;
  }

  .robot-ops-title {
    font-size: 16px;
    font-weight: 700;
    color: #111827;
  }

  .robot-ops-subtitle {
    margin-top: 6px;
    font-size: 13px;
    color: #6b7280;
  }

  .robot-ops-body {
    display: flex;
    gap: 16px;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
  }

  .robot-form {
    margin-bottom: -18px;
  }

  .robot-ops-actions {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
  }

  .robot-ops-result {
    margin-top: 14px;
    padding: 12px 14px;
    border-radius: 14px;
    background: rgba(255, 255, 255, 0.7);
    color: #0f172a;
    font-size: 13px;
    line-height: 1.6;
  }

  .job-name {
    font-weight: 600;
    color: #111827;
  }

  .job-meta {
    margin-top: 4px;
    font-size: 12px;
    color: #6b7280;
  }

  .handler-tip {
    margin-top: 12px;
    padding: 14px 16px;
    border-radius: 14px;
    background: linear-gradient(135deg, #f8fbff 0%, #f3fff8 100%);
    border: 1px solid #dbeafe;
  }

  .handler-name {
    font-weight: 600;
    color: #111827;
  }

  .handler-desc {
    margin-top: 6px;
    font-size: 13px;
    line-height: 1.6;
    color: #6b7280;
  }

  .drawer-title {
    font-size: 18px;
    font-weight: 700;
    color: #111827;
  }
}
</style>
