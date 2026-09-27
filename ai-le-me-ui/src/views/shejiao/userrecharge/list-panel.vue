<template>
  <div class="app-container">
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="80px">
      <el-form-item label="关键词" prop="key">
        <el-input
          v-model="queryParams.key"
          placeholder="UID / 昵称 / 订单号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单状态" prop="type">
        <el-select v-model="queryParams.type" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单类型" prop="type2">
        <el-select v-model="queryParams.type2" placeholder="全部类型" clearable style="width: 160px">
          <el-option v-for="item in orderTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">批量删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="用户" min-width="180">
        <template #default="{ row }">
          <div class="user-cell">
            <span class="user-name">{{ row.nickname || '-' }}</span>
            <span class="user-sub">UID {{ row.uid || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="订单号" prop="orderId" min-width="180" show-overflow-tooltip />
      <el-table-column label="商品信息" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="user-cell">
            <span class="user-name">{{ row.title || formatOrderType(row.type) }}</span>
            <span class="user-sub">业务ID {{ row.bizId || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="订单类型" align="center" width="120">
        <template #default="{ row }">
          <el-tag effect="light">{{ formatOrderType(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="充值金额" align="center" width="110">
        <template #default="{ row }">
          <span class="price-main">{{ formatMoney(row.price) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="赠送金额" align="center" width="110">
        <template #default="{ row }">
          <span>{{ formatMoney(row.givePrice) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="到账爱情币" align="center" width="120">
        <template #default="{ row }">
          <span>{{ row.coinAmount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="已退款" align="center" width="110">
        <template #default="{ row }">
          <span>{{ formatMoney(row.refundAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付方式" align="center" width="120">
        <template #default="{ row }">
          <span>{{ formatRechargeType(row.rechargeType) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付渠道" align="center" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.channel" effect="plain">{{ formatChannel(row.channel) }}</el-tag>
          <span v-else class="muted-text">-</span>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" align="center" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ formatStatus(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="三方流水号" prop="transactionId" min-width="180" show-overflow-tooltip />
      <el-table-column label="商户单号" prop="outTradeNo" min-width="180" show-overflow-tooltip />
      <el-table-column label="支付时间" align="center" prop="payTime" width="170" />
      <el-table-column label="创建时间" align="center" prop="addTime" width="170" />
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRefundRecords(row)">退款记录</el-button>
          <el-button
            v-if="row.status === 1 && [1, 2].includes(Number(row.type))"
            link
            type="warning"
            @click="handleRefund(row)"
          >
            全额退款
          </el-button>
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

    <el-dialog v-model="refundDialogVisible" title="退款记录" width="720px">
      <el-table :data="refundRecords">
        <el-table-column label="退款单号" prop="refundNo" min-width="160" show-overflow-tooltip />
        <el-table-column label="退款金额" width="100">
          <template #default="{ row }">
            <span>{{ formatMoney(row.refundAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="扣回爱情币" prop="coinAmount" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.refundStatus === 1 ? 'success' : row.refundStatus === 2 ? 'danger' : 'warning'">
              {{ row.refundStatus === 1 ? '成功' : row.refundStatus === 2 ? '失败' : '处理中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="原因" prop="reason" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作人" prop="operatorName" width="120" />
        <el-table-column label="退款时间" prop="refundTime" width="170" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, ref, type ComponentInternalInstance } from 'vue';
import { delUserRecharge, listUserRecharge, listUserRechargeRefund, refundUserRecharge } from '@/api/shejiao/userrecharge';
import type { UserRechargeQuery, UserRechargeRefundVO, UserRechargeVO } from '@/api/shejiao/userrecharge/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const statusOptions = [
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '已退款', value: 2 },
  { label: '已关闭', value: 3 }
];

const orderTypeOptions = [
  { label: '钱包充值', value: 0 },
  { label: '会员充值', value: 1 },
  { label: '爱情币充值', value: 2 },
  { label: '活动报名', value: 3 }
];

const rechargeTypeMap: Record<string, string> = {
  weixin: '小程序支付',
  wxh5: '公众号支付',
  h5: 'H5 支付',
  app: 'App 支付'
};

const channelMap: Record<string, string> = {
  wechat: '微信',
  alipay: '支付宝'
};

const dataList = ref<UserRechargeVO[]>([]);
const refundRecords = ref<UserRechargeRefundVO[]>([]);
const refundDialogVisible = ref(false);
const loading = ref(false);
const ids = ref<Array<string | number>>([]);
const multiple = ref(true);
const total = ref(0);
const queryFormRef = ref();

const queryParams = ref<UserRechargeQuery>({
  pageNum: 1,
  pageSize: 10,
  key: undefined,
  type: undefined,
  type2: undefined
});

const formatMoney = (value?: number | string | null) => {
  if (value === undefined || value === null || value === '') return '0';
  return `${value}`;
};

const formatOrderType = (value?: number) => {
  return orderTypeOptions.find((item) => item.value === value)?.label || '未知类型';
};

const formatStatus = (value?: number) => {
  return statusOptions.find((item) => item.value === value)?.label || '未知状态';
};

const statusTagType = (value?: number) => {
  if (value === 1) return 'success';
  if (value === 2) return 'warning';
  if (value === 3) return 'info';
  return '';
};

const formatRechargeType = (value?: string | null) => {
  if (!value) return '-';
  return rechargeTypeMap[value] || value;
};

const formatChannel = (value?: string | null) => {
  if (!value) return '-';
  return channelMap[value] || value;
};

const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listUserRecharge(queryParams.value);
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

const handleSelectionChange = (selection: UserRechargeVO[]) => {
  ids.value = selection.map((item) => item.id!);
  multiple.value = !selection.length;
};

const handleDelete = (row?: UserRechargeVO) => {
  const idList = row?.id ? [row.id] : ids.value;
  if (!idList.length) return;
  proxy?.$modal
    .confirm(`是否确认删除充值记录：${idList.join(', ')}？`)
    .then(async () => {
      loading.value = true;
      try {
        await delUserRecharge(idList);
        await getList();
        proxy?.$modal.msgSuccess('删除成功');
      } finally {
        loading.value = false;
      }
    })
    .catch(() => {});
};

const handleRefund = (row: UserRechargeVO) => {
  proxy?.$modal
    .confirm(`确认全额退款订单 ${row.orderId} 吗？`)
    .then(async () => {
      loading.value = true;
      try {
        await refundUserRecharge({
          orderId: row.orderId,
          refundAmount: row.price as number | string,
          reason: '后台全额退款'
        });
        proxy?.$modal.msgSuccess('退款成功');
        await getList();
      } finally {
        loading.value = false;
      }
    })
    .catch(() => {});
};

const openRefundRecords = async (row: UserRechargeVO) => {
  const res: any = await listUserRechargeRefund(row.orderId);
  refundRecords.value = res.list || [];
  refundDialogVisible.value = true;
};

getList();
</script>

<style scoped>
.user-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.user-name,
.price-main {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.user-sub,
.muted-text {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
