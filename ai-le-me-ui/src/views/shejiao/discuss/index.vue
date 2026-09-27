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
      <el-table-column label="话题ID" align="center" prop="id" width="80" />
      <el-table-column label="用户ID" align="center" prop="uid" width="80" />
      <el-table-column label="圈子ID" align="center" prop="topicId" width="80" />
      <el-table-column label="标题" align="center" prop="title" show-overflow-tooltip />
      <el-table-column label="描述" align="center" prop="introduce" show-overflow-tooltip />
      <el-table-column label="浏览量" align="center" prop="readCount" width="100" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" width="250" class-name="small-padding fixed-width">
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
          <el-button
            link
            type="warning"
            icon="Remove"
            @click="handleClearPosts(scope.row)"
          >清除帖子</el-button>
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
    
    <!-- 新增/修改对话框 -->
    <add-or-update ref="addOrUpdateRef" @success="getList" />
  </div>
</template>

<script setup lang="ts" name="ShejiaoDiscuss">
import { getCurrentInstance, ref, reactive, toRefs } from 'vue';
import { listDiscuss, getDiscuss, delDiscuss, addDiscuss, updateDiscuss } from '@/api/shejiao/discuss';
import { DiscussVO, DiscussQuery, DiscussForm } from '@/api/shejiao/discuss/types';
import { shejiaoService } from '@/utils/request';
import AddOrUpdate from './add-or-update.vue';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const dataList = ref<DiscussVO[]>([]);
const loading = ref(false);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const addOrUpdateRef = ref();

const queryParams = ref<DiscussQuery>({
  pageNum: 1,
  pageSize: 10,
  key: undefined
});

/** 查询列表 */
const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listDiscuss(queryParams.value);
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
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    key: undefined
  };
  handleQuery();
};

/** 多选框选中数据 */
const handleSelectionChange = (selection: DiscussVO[]) => {
  ids.value = selection.map(item => item.id!);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

/** 新增按钮操作 */
const handleAdd = () => {
  addOrUpdateRef.value?.open();
};

/** 修改按钮操作 */
const handleUpdate = (row?: DiscussVO) => {
  const id = row?.id || ids.value[0];
  addOrUpdateRef.value?.open(id);
};

/** 删除按钮操作 */
const handleDelete = (row?: DiscussVO) => {
  const idList = row?.id ? [row.id] : ids.value;
  proxy?.$modal.confirm('是否确认删除编号为"' + idList + '"的话题数据？').then(async () => {
    loading.value = true;
    try {
      await delDiscuss(idList);
      await getList();
      proxy?.$modal.msgSuccess('删除成功');
    } finally {
      loading.value = false;
    }
  }).catch(() => {});
};

/** 清除话题下所有帖子 */
const handleClearPosts = (row: DiscussVO) => {
  proxy?.$modal.confirm(`是否确认清除话题"${row.title}"下的所有帖子？（话题不会被删除）`).then(async () => {
    loading.value = true;
    try {
      await shejiaoService({
        url: '/admin/discuss/deletePostInDiscuss',
        method: 'post',
        data: [row.id]
      });
      await getList();
      proxy?.$modal.msgSuccess('清除成功');
    } catch (error) {
      console.error('清除帖子失败:', error);
    } finally {
      loading.value = false;
    }
  }).catch(() => {});
};

// 初始化
getList();
</script>
