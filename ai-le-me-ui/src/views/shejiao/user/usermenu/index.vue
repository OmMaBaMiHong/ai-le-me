<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="关键词" prop="key">
        <el-input
          v-model="queryParams.key"
          placeholder="请输入名称或跳转路径"
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
          v-hasPermi="['admin:usermenu:save']"
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
        >
          新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['admin:usermenu:delete']"
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
        >
          批量删除
        </el-button>
      </el-col>
    </el-row>

    <el-table
      v-loading="loading"
      :data="dataList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="图标" align="center" prop="img" width="100">
        <template #default="scope">
          <img
            v-if="scope.row.img"
            :src="scope.row.img"
            alt="菜单图标"
            class="menu-icon"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="名称" align="center" prop="name" min-width="140" />
      <el-table-column label="跳转路径" align="center" prop="url" min-width="220" show-overflow-tooltip />
      <el-table-column label="排序" align="center" prop="sort" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 0" type="success">显示</el-tag>
          <el-tag v-else type="warning">不显示</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            v-hasPermi="['admin:usermenu:update']"
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['admin:usermenu:delete']"
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
          >
            删除
          </el-button>
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

    <add-or-update ref="addOrUpdateRef" @refreshDataList="getList" />
  </div>
</template>

<script setup>
import { getCurrentInstance, ref } from 'vue';
import AddOrUpdate from './add-or-update.vue';
import { listUserMenu, delUserMenu } from '@/api/shejiao/usermenu';

const queryFormRef = ref();
const addOrUpdateRef = ref();
const dataList = ref([]);
const loading = ref(false);
const ids = ref([]);
const multiple = ref(true);
const total = ref(0);

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  key: ''
});

const getList = async () => {
  loading.value = true;
  try {
    const res = await listUserMenu(queryParams.value);
    dataList.value = res.page?.list || [];
    total.value = res.page?.totalCount || 0;
  } catch (error) {
    console.error('获取用户菜单列表失败:', error);
    dataList.value = [];
    total.value = 0;
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

const handleSelectionChange = (selection) => {
  ids.value = selection.map((item) => item.id);
  multiple.value = selection.length === 0;
};

const handleAdd = () => {
  addOrUpdateRef.value?.init();
};

const handleUpdate = (row) => {
  addOrUpdateRef.value?.init(row.id);
};

const handleDelete = (row) => {
  const idList = row?.id ? [row.id] : ids.value;
  if (!idList.length) {
    return;
  }
  proxy?.$modal.confirm(`是否确认删除用户菜单编号为"${idList.join(',')}"的数据项？`).then(async () => {
    loading.value = true;
    try {
      await delUserMenu(idList);
      await getList();
      proxy?.$modal.msgSuccess('删除成功');
    } finally {
      loading.value = false;
    }
  }).catch(() => {});
};

const { proxy } = getCurrentInstance();

getList();
</script>

<style scoped>
.menu-icon {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 8px;
}
</style>
