<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="关键词" prop="key">
        <el-input
          v-model="queryParams.key"
          placeholder="请输入关键词"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
        >删除</el-button>
      </el-col>
    </el-row>

    <el-table
      v-loading="loading"
      :data="dataList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" />
      <!-- TODO: 根据业务需求补充表格列 -->
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
          >修改</el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
          >删除</el-button>
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

<script setup lang="ts" name="ShejiaoLuckdraw">
import { getCurrentInstance, ref, reactive, toRefs } from 'vue';
import { listLuckdraw, getLuckdraw, delLuckdraw, addLuckdraw, updateLuckdraw } from '@/api/shejiao/luckdraw';
import { LuckdrawVO, LuckdrawQuery, LuckdrawForm } from '@/api/shejiao/luckdraw/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const dataList = ref<LuckdrawVO[]>([]);
const loading = ref(false);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);

const queryParams = ref<LuckdrawQuery>({
  pageNum: 1,
  pageSize: 10,
  key: undefined
});

/** 查询列表 */
const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listLuckdraw(queryParams.value);
    dataList.value = res.page?.list || [];
    total.value = res.page?.totalCount || 0;
  } finally {
    loading.value = false;
  }
};

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

/** 重置按钮操作 */
const resetQuery = () => {
  proxy?.resetForm('queryFormRef');
  handleQuery();
};

/** 多选框选中数据 */
const handleSelectionChange = (selection: LuckdrawVO[]) => {
  ids.value = selection.map(item => item.id!);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

/** 新增按钮操作 */
const handleAdd = () => {
  // TODO: 打开新增对话框
  proxy?.$modal.msgWarning('新增功能待实现');
};

/** 修改按钮操作 */
const handleUpdate = (row?: LuckdrawVO) => {
  // TODO: 打开修改对话框
  proxy?.$modal.msgWarning('修改功能待实现');
};

/** 删除按钮操作 */
const handleDelete = (row?: LuckdrawVO) => {
  const idList = row?.id ? [row.id] : ids.value;
  proxy?.$modal.confirm('是否确认删除编号为"' + idList + '"的数据项？').then(async () => {
    loading.value = true;
    try {
      await delLuckdraw(idList);
      await getList();
      proxy?.$modal.msgSuccess('删除成功');
    } finally {
      loading.value = false;
    }
  }).catch(() => {});
};

// 初始化
getList();
</script>
