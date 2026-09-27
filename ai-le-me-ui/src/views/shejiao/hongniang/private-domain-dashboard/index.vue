<template>
  <div class="private-domain-dashboard">
    <el-form :model="queryParams" inline label-width="84px">
      <el-form-item label="指定红娘">
        <el-select v-model="queryParams.hongniangId" clearable filterable style="width: 260px" @change="loadData">
          <el-option v-for="item in hongniangList" :key="item.id" :label="`${item.hongniangName} (${item.phone || '-'})`" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Refresh" @click="loadData">刷新</el-button>
      </el-form-item>
    </el-form>

    <div class="metric-grid">
      <div v-for="item in metricCards" :key="item.label" class="metric-card">
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-value">{{ item.value }}</div>
        <div class="metric-tip">{{ item.tip }}</div>
      </div>
    </div>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="section-head">
              <span>待推进案件</span>
              <el-tag type="warning">{{ kanban.urgentCases?.length || 0 }}</el-tag>
            </div>
          </template>
          <el-table :data="kanban.urgentCases || []" size="small" max-height="320">
            <el-table-column prop="id" label="案件" width="80" />
            <el-table-column label="组合" min-width="180">
              <template #default="{ row }">{{ row.maleUsername }} / {{ row.femaleUsername }}</template>
            </el-table-column>
            <el-table-column prop="currentStageLabel" label="阶段" width="100" />
            <el-table-column prop="nextFollowTime" label="下次跟进" width="170" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="section-head">
              <span>待回应牵线申请</span>
              <el-tag type="danger">{{ kanban.pendingRequests?.length || 0 }}</el-tag>
            </div>
          </template>
          <el-table :data="kanban.pendingRequests || []" size="small" max-height="320">
            <el-table-column prop="id" label="申请" width="80" />
            <el-table-column label="方向" min-width="160">
              <template #default="{ row }">{{ row.fromUserName }} -> {{ row.toUserName }}</template>
            </el-table-column>
            <el-table-column prop="requestChannelLabel" label="渠道" width="120" />
            <el-table-column prop="requestStatusLabel" label="状态" width="100" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="section-head">
              <span>最近群资产</span>
              <el-tag type="success">{{ kanban.recentGroups?.length || 0 }}</el-tag>
            </div>
          </template>
          <el-table :data="kanban.recentGroups || []" size="small" max-height="320">
            <el-table-column prop="groupName" label="群名称" min-width="180" />
            <el-table-column prop="syncStatusLabel" label="同步" width="100" />
            <el-table-column prop="boundUserCount" label="用户数" width="88" />
            <el-table-column prop="lastSyncTime" label="最后同步" width="170" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="section-head">
              <span>最近任务执行</span>
              <el-tag>{{ kanban.recentExecutions?.length || 0 }}</el-tag>
            </div>
          </template>
          <el-table :data="kanban.recentExecutions || []" size="small" max-height="320">
            <el-table-column prop="taskId" label="任务" width="80" />
            <el-table-column prop="executionStatusLabel" label="执行状态" width="100" />
            <el-table-column prop="resultSummary" label="结果摘要" min-width="180" show-overflow-tooltip />
            <el-table-column prop="finishTime" label="完成时间" width="170" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { getAllHongniangs } from '@/api/hongniang/hongniangInfo';
import { getHongniangPrivateDomainKanban, getHongniangPrivateDomainOverview } from '@/api/hongniang/privateDomain';

const queryParams = reactive<{ hongniangId?: number | string }>({});
const hongniangList = ref<any[]>([]);
const overview = ref<Record<string, any>>({});
const kanban = ref<Record<string, any>>({});

const metricCards = computed(() => {
  const data = overview.value || {};
  return [
    { label: '用户池', value: data.poolUserCount || 0, tip: '当前归属用户数' },
    { label: '总案件', value: data.caseCount || 0, tip: '全部牵线案件' },
    { label: '开放案件', value: data.openCaseCount || 0, tip: '仍在推进中' },
    { label: '成功案件', value: data.successCaseCount || 0, tip: '结婚 / 生子' },
    { label: '群资产', value: data.wechatGroupCount || 0, tip: '登记群数量' },
    { label: '群任务', value: data.activeTouchTaskCount || 0, tip: '待发或执行中' },
    { label: '待回应牵线', value: data.pendingMatchRequestCount || 0, tip: '需要继续催进度' }
  ];
});

async function loadHongniangList() {
  const res = await getAllHongniangs().catch(() => null);
  hongniangList.value = res?.page?.list || [];
}

async function loadData() {
  const params = queryParams.hongniangId ? { hongniangId: queryParams.hongniangId } : {};
  const [overviewRes, kanbanRes] = await Promise.all([
    getHongniangPrivateDomainOverview(queryParams.hongniangId as any).catch(() => null),
    getHongniangPrivateDomainKanban(params).catch(() => null)
  ]);
  overview.value = overviewRes?.data || {};
  kanban.value = kanbanRes?.data || {};
}

loadHongniangList();
loadData();
</script>

<style scoped lang="scss">
.private-domain-dashboard {
  display: grid;
  gap: 16px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.metric-card {
  padding: 20px;
  border-radius: 22px;
  background: linear-gradient(160deg, #ffffff, #f5f7f2);
  border: 1px solid rgba(23, 51, 47, 0.08);
}

.metric-label {
  color: #647267;
  font-size: 13px;
}

.metric-value {
  margin-top: 8px;
  font-size: 34px;
  font-weight: 700;
  color: #17332f;
}

.metric-tip {
  margin-top: 6px;
  color: #96a297;
  font-size: 12px;
}

.section-row {
  margin-top: 0;
}

.section-card {
  border-radius: 22px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
</style>
