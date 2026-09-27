<template>
  <div class="trace-panel">
    <el-card shadow="never" class="filter-card">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">Agent Runtime Trace</div>
            <div class="card-subtitle">统一查看智能画像、智能红娘、智能恋爱助手三条推理链路的运行轨迹。</div>
          </div>
          <el-button type="primary" plain @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="filters" class="trace-filter">
        <el-form-item label="Agent">
          <el-select v-model="filters.agentType" clearable placeholder="全部" style="width: 160px">
            <el-option label="智能画像" value="persona" />
            <el-option label="智能红娘" value="matchmaker" />
            <el-option label="恋爱助手" value="companion" />
          </el-select>
        </el-form-item>
        <el-form-item label="Owner UID">
          <el-input v-model="filters.ownerUserId" clearable placeholder="主人用户" style="width: 140px" />
        </el-form-item>
        <el-form-item label="Target UID">
          <el-input v-model="filters.targetUserId" clearable placeholder="目标用户" style="width: 140px" />
        </el-form-item>
        <el-form-item label="Provider">
          <el-input v-model="filters.providerCode" clearable placeholder="openai / mock" style="width: 150px" />
        </el-form-item>
        <el-form-item label="TraceId">
          <el-input v-model="filters.traceId" clearable placeholder="trace_id" style="width: 180px" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="摘要 / 推理 / 错误" style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="时间" min-width="168">
          <template #default="{ row }">
            {{ formatTime(row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column label="Agent" min-width="120">
          <template #default="{ row }">
            <el-tag :type="agentTagType(row.agent_type)" effect="light">{{ agentLabel(row.agent_type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Scene" prop="scene_code" min-width="160" />
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div class="user-meta">
              <span>Owner {{ row.owner_user_id || '-' }}</span>
              <span>Target {{ row.target_user_id || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="模型" min-width="150">
          <template #default="{ row }">
            <div class="model-meta">
              <span>{{ row.model_provider || '-' }}</span>
              <span>{{ row.model_profile || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="摘要" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.response_summary || row.reasoning_summary || row.request_summary || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" effect="light">
              {{ row.status || 'success' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.trace_id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="pagination.total"
          :current-page="pagination.page"
          :page-size="pagination.pageSize"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="Runtime Trace Detail" size="58%">
      <div v-loading="detailLoading" class="detail-shell">
        <template v-if="detail">
          <div class="detail-grid">
            <el-card shadow="never">
              <template #header>
                <div class="detail-title">基础信息</div>
              </template>
              <div class="detail-meta">
                <span>TraceId: {{ detail.trace_id }}</span>
                <span>Agent: {{ agentLabel(detail.agent_type) }}</span>
                <span>Scene: {{ detail.scene_code }}</span>
                <span>Function: {{ detail.function_type }}</span>
                <span>Provider: {{ detail.model_provider || '-' }}</span>
                <span>Profile: {{ detail.model_profile || '-' }}</span>
              </div>
            </el-card>

            <el-card shadow="never">
              <template #header>
                <div class="detail-title">召回与图谱</div>
              </template>
              <div class="detail-block">
                <div class="detail-subtitle">Retrieval Hits</div>
                <el-tag v-for="item in detail.retrieval_hits || []" :key="item" class="trace-tag">{{ item }}</el-tag>
                <span v-if="!detail.retrieval_hits?.length" class="empty-text">暂无</span>
              </div>
              <div class="detail-block">
                <div class="detail-subtitle">Graph Facts</div>
                <el-tag v-for="item in detail.graph_facts || []" :key="item" type="success" class="trace-tag">{{ item }}</el-tag>
                <span v-if="!detail.graph_facts?.length" class="empty-text">暂无</span>
              </div>
            </el-card>

            <el-card shadow="never">
              <template #header>
                <div class="detail-title">Timing</div>
              </template>
              <div class="json-box">{{ formatJson(detail.timing_breakdown || {}) }}</div>
            </el-card>

            <el-card shadow="never">
              <template #header>
                <div class="detail-title">Request</div>
              </template>
              <div class="json-box">{{ formatJson(detail.request_json || {}) }}</div>
            </el-card>

            <el-card shadow="never">
              <template #header>
                <div class="detail-title">Response</div>
              </template>
              <div class="json-box">{{ formatJson(detail.response_json || {}) }}</div>
            </el-card>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getRuntimeTraceDetail,
  listRuntimeTraces,
  type RuntimeTraceDetail,
  type RuntimeTraceListItem
} from '@/api/system/thirdparty';

const loading = ref(false);
const detailLoading = ref(false);
const detailVisible = ref(false);
const rows = ref<RuntimeTraceListItem[]>([]);
const detail = ref<RuntimeTraceDetail>();

const filters = reactive({
  agentType: '',
  ownerUserId: '',
  targetUserId: '',
  providerCode: '',
  traceId: '',
  keyword: ''
});

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
});

const loadData = async () => {
  loading.value = true;
  try {
    const res: any = await listRuntimeTraces({
      ...filters,
      ownerUserId: filters.ownerUserId ? Number(filters.ownerUserId) : undefined,
      targetUserId: filters.targetUserId ? Number(filters.targetUserId) : undefined,
      page: pagination.page,
      pageSize: pagination.pageSize
    });
    const data = res.data || {};
    rows.value = data.items || [];
    pagination.total = Number(data.total || 0);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.page = 1;
  loadData();
};

const handleReset = () => {
  filters.agentType = '';
  filters.ownerUserId = '';
  filters.targetUserId = '';
  filters.providerCode = '';
  filters.traceId = '';
  filters.keyword = '';
  pagination.page = 1;
  loadData();
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  loadData();
};

const openDetail = async (traceId: string) => {
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    const res: any = await getRuntimeTraceDetail(traceId);
    detail.value = res.data;
  } catch (error: any) {
    ElMessage.error(error?.message || error?.msg || '读取 trace 详情失败');
    detailVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
};

const agentLabel = (agentType: string) => {
  const labelMap: Record<string, string> = {
    persona: '智能画像',
    matchmaker: '智能红娘',
    companion: '恋爱助手'
  };
  return labelMap[agentType] || agentType || '-';
};

const agentTagType = (agentType: string) => {
  if (agentType === 'persona') {
    return 'success';
  }
  if (agentType === 'matchmaker') {
    return 'warning';
  }
  if (agentType === 'companion') {
    return 'primary';
  }
  return 'info';
};

const formatTime = (value: string) => {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').replace(/\.\d+$/, '');
};

const formatJson = (value: Record<string, any> | any[]) => JSON.stringify(value, null, 2);

onMounted(loadData);
</script>

<style scoped lang="scss">
.trace-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card,
.table-card {
  border-radius: 20px;
  border: 1px solid #eef2f7;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.card-title {
  font-size: 17px;
  font-weight: 600;
  color: #111827;
}

.card-subtitle {
  margin-top: 6px;
  color: #667085;
  font-size: 13px;
}

.trace-filter {
  margin-bottom: -18px;
}

.user-meta,
.model-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #475467;
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-shell {
  padding-right: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.detail-title {
  font-size: 15px;
  font-weight: 600;
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  color: #475467;
  font-size: 13px;
}

.detail-block + .detail-block {
  margin-top: 14px;
}

.detail-subtitle {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #344054;
}

.trace-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}

.empty-text {
  color: #98a2b3;
  font-size: 13px;
}

.json-box {
  padding: 14px;
  border-radius: 14px;
  background: #0f172a;
  color: #d1fae5;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
