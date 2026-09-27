<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="关键词" prop="key">
        <el-input
          v-model="queryParams.key"
          placeholder="请输入分类名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="置顶状态" prop="isTop">
        <el-select
          v-model="queryParams.isTop"
          placeholder="请选择置顶状态"
          clearable
        >
          <el-option label="未置顶" :value="0" />
          <el-option label="已置顶" :value="1" />
        </el-select>
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
          @click="handleDelete()"
        >删除</el-button>
      </el-col>
    </el-row>

    <el-table
      v-loading="loading"
      :data="dataList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="分类ID" align="center" prop="cateId" width="80" />
      <el-table-column label="分类名称" align="center" prop="cateName" width="120" />
      <el-table-column label="封面图" align="center" width="100">
        <template #default="scope">
          <el-image
            v-if="scope.row.coverImage"
            :src="scope.row.coverImage"
            :preview-src-list="[scope.row.coverImage]"
            fit="cover"
            style="width: 60px; height: 60px; border-radius: 4px;"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="置顶状态" align="center" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.isTop === 1" type="success">已置顶</el-tag>
          <el-tag v-else type="info">未置顶</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            v-if="scope.row.isTop === 0"
            link
            type="success"
            icon="Top"
            @click="handleSetTop(scope.row, 1)"
          >置顶</el-button>
          <el-button
            v-else
            link
            type="warning"
            icon="Bottom"
            @click="handleSetTop(scope.row, 0)"
          >取消置顶</el-button>
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

<script setup lang="ts" name="ShejiaoCategory">
import { getCurrentInstance, ref, reactive } from 'vue';
import { listCategory, getCategory, delCategory, addCategory, updateCategory } from '@/api/shejiao/category';
import { CategoryVO, CategoryQuery, CategoryForm } from '@/api/shejiao/category/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const dataList = ref<CategoryVO[]>([]);
const loading = ref(false);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  key: undefined,
  isTop: undefined
});

/** 查询列表 */
const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listCategory(queryParams);
    dataList.value = res.page?.list || [];
    total.value = res.page?.totalCount || 0;
  } finally {
    loading.value = false;
  }
};

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};

/** 重置按钮操作 */
const resetQuery = () => {
  (proxy as any)?.resetForm('queryFormRef');
  handleQuery();
};

/** 多选框选中数据 */
const handleSelectionChange = (selection: CategoryVO[]) => {
  ids.value = selection.map(item => item.cateId!);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

/** 置顶/取消置顶操作 */
const handleSetTop = async (row: CategoryVO, isTop: number) => {
  loading.value = true;
  try {
    const data: CategoryForm = {
      cateId: row.cateId,
      cateName: row.cateName,
      coverImage: row.coverImage,
      isTop: isTop
    };
    await updateCategory(data);
    proxy?.$modal.msgSuccess(isTop === 1 ? '置顶成功' : '取消置顶成功');
    await getList();
  } finally {
    loading.value = false;
  }
};

/** 新增按钮操作 */
const handleAdd = () => {
  // TODO: 打开新增对话框
  proxy?.$modal.msgWarning('新增功能待实现');
};

/** 修改按钮操作 */
const handleUpdate = (row?: CategoryVO) => {
  // TODO: 打开修改对话框
  proxy?.$modal.msgWarning('修改功能待实现');
};

/** 删除按钮操作 */
const handleDelete = (row?: CategoryVO) => {
  const idList = row?.cateId ? [row.cateId] : ids.value;
  proxy?.$modal.confirm('是否确认删除编号为"' + idList + '"的数据项？').then(async () => {
    loading.value = true;
    try {
      await delCategory(idList);
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
