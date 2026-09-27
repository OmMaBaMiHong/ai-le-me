<template>
  <div class="app-container">
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="80px">
      <el-form-item label="用户/账单" prop="key">
        <el-input
          v-model="queryParams.key"
          placeholder="输入 UID 查询"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="收支方向" prop="type">
        <el-select v-model="queryParams.type" placeholder="全部方向" clearable style="width: 140px">
          <el-option label="收入" :value="1" />
          <el-option label="支出" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="账单分类" prop="type2">
        <el-select v-model="queryParams.type2" placeholder="全部分类" clearable style="width: 160px">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="用户UID" align="center" prop="uid" width="96" />
      <el-table-column label="关联用户" align="center" width="96">
        <template #default="{ row }">
          <span>{{ row.tipUserId || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="账单标题" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="bill-title">{{ row.title || '-' }}</div>
          <div class="bill-sub">{{ row.mark || '无备注' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="分类" align="center" width="110">
        <template #default="{ row }">
          <el-tag effect="light">{{ formatCategory(row.category) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="明细类型" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ formatBillType(row.type) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收支" align="center" width="90">
        <template #default="{ row }">
          <el-tag :type="row.pm === 1 ? 'success' : 'danger'">
            {{ row.pm === 1 ? '收入' : '支出' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" align="center" width="110">
        <template #default="{ row }">
          <span :class="row.pm === 1 ? 'amount-up' : 'amount-down'">
            {{ row.pm === 1 ? '+' : '-' }}{{ row.number || 0 }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="余额" align="center" prop="balance" width="110" />
      <el-table-column label="关联ID" align="center" prop="linkId" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" align="center" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : 'info'">
            {{ row.status === 1 ? '有效' : row.status === 0 ? '待确认' : '无效' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="账单时间" align="center" prop="addTime" width="170" />
      <el-table-column label="操作" align="center" width="100" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
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
  </div>
</template>

<script setup lang="ts" name="ShejiaoBill">
import { getCurrentInstance, ref } from 'vue';
import { listBill, delBill } from '@/api/shejiao/bill';
import { BillVO, BillQuery } from '@/api/shejiao/bill/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const categoryOptions = [
  { label: '现金', value: 'money' },
  { label: '积分', value: 'integral' },
  { label: '会员', value: 'vip' }
];

const billTypeLabelMap: Record<string, string> = {
  gain: '奖励入账',
  recharge: '充值到账',
  exchange: '兑换到账',
  reward_integral_add: '被打赏增加积分',
  reward_integral_sub: '打赏扣减积分',
  vip_post_pay: '付费帖子支付',
  vip_post_income: '付费帖子收入',
  sign_reward: '签到奖励'
};

const dataList = ref<BillVO[]>([]);
const loading = ref(false);
const ids = ref<Array<string | number>>([]);
const multiple = ref(true);
const total = ref(0);
const queryFormRef = ref();
const queryParams = ref<BillQuery>({
  pageNum: 1,
  pageSize: 10,
  key: undefined,
  type: undefined,
  type2: undefined
});

const formatCategory = (value?: string) => {
  if (!value) return '未分类';
  const target = categoryOptions.find((item) => item.value === value);
  return target?.label || value;
};

const formatBillType = (value?: string) => {
  if (!value) return '-';
  return billTypeLabelMap[value] || value;
};

const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listBill(queryParams.value);
    dataList.value = res.page?.list || [];
    total.value = res.page?.totalCount || 0;
  } finally {
    loading.value = false;
  }
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

const handleSelectionChange = (selection: BillVO[]) => {
  ids.value = selection.map((item) => item.id!);
  multiple.value = !selection.length;
};

const handleDelete = (row?: BillVO) => {
  const idList = row?.id ? [row.id] : ids.value;
  if (!idList.length) return;
  proxy?.$modal
    .confirm(`是否确认删除账单记录：${idList.join(', ')}？`)
    .then(async () => {
      loading.value = true;
      try {
        await delBill(idList);
        await getList();
        proxy?.$modal.msgSuccess('删除成功');
      } finally {
        loading.value = false;
      }
    })
    .catch(() => {});
};

getList();
</script>

<style scoped>
.bill-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.bill-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.amount-up {
  color: var(--el-color-success);
  font-weight: 600;
}

.amount-down {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
