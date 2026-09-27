<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="关键词" prop="key">
        <el-input
          v-model="queryParams.key"
          placeholder="请输入圈子名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
        >
          <el-option label="启用" :value="0" />
          <el-option label="禁用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="推荐" prop="isRecommend">
        <el-select
          v-model="queryParams.isRecommend"
          placeholder="请选择是否推荐"
          clearable
        >
          <el-option label="未推荐" :value="0" />
          <el-option label="已推荐" :value="1" />
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
      <el-table-column label="圈子ID" align="center" prop="id" width="80" />
      <el-table-column label="圈子名称" align="center" prop="topicName" width="150" />
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
      <el-table-column label="描述" align="center" prop="description" width="200" show-overflow-tooltip />
      <el-table-column label="帖子数" align="center" prop="postCount" width="100" />
      <el-table-column label="关注数" align="center" prop="followCount" width="100" />
      <el-table-column label="推荐状态" align="center" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.isRecommend === 1" type="success">已推荐</el-tag>
          <el-tag v-else type="info">未推荐</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 0" type="success">启用</el-tag>
          <el-tag v-else type="danger">禁用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button
            v-if="scope.row.isRecommend === 0"
            link
            type="success"
            icon="Star"
            @click="handleSetRecommend(scope.row, 1)"
          >推荐</el-button>
          <el-button
            v-else
            link
            type="warning"
            icon="StarFilled"
            @click="handleSetRecommend(scope.row, 0)"
          >取消推荐</el-button>
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
    
    <!-- 新增/修改对话框 -->
    <add-or-update ref="addOrUpdateRef" @success="getList" />
  </div>
</template>

<script setup lang="ts" name="ShejiaoTopic">
import { getCurrentInstance, ref, reactive } from 'vue';
import { listTopic, getTopic, delTopic, addTopic, updateTopic } from '@/api/shejiao/topic';
import { TopicVO, TopicQuery, TopicForm } from '@/api/shejiao/topic/types';
import AddOrUpdate from './add-or-update.vue';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const dataList = ref<TopicVO[]>([]);
const loading = ref(false);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const addOrUpdateRef = ref();

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  key: undefined,
  status: undefined,
  isRecommend: undefined
});

/** 查询列表 */
const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listTopic(queryParams);
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
const handleSelectionChange = (selection: TopicVO[]) => {
  ids.value = selection.map(item => item.id!);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

/** 推荐/取消推荐操作 */
const handleSetRecommend = async (row: TopicVO, isRecommend: number) => {
  loading.value = true;
  try {
    const data: TopicForm = {
      id: row.id,
      topicName: row.topicName,
      coverImage: row.coverImage,
      description: row.description,
      isRecommend: isRecommend,
      sortOrder: row.sortOrder,
      status: row.status
    };
    await updateTopic(data);
    proxy?.$modal.msgSuccess(isRecommend === 1 ? '推荐成功' : '取消推荐成功');
    await getList();
  } finally {
    loading.value = false;
  }
};

/** 新增按钮操作 */
const handleAdd = () => {
  addOrUpdateRef.value?.open();
};

/** 修改按钮操作 */
const handleUpdate = (row?: TopicVO) => {
  const id = row?.id || ids.value[0];
  addOrUpdateRef.value?.open(id);
};

/** 删除按钮操作 */
const handleDelete = (row?: TopicVO) => {
  const idList = row?.id ? [row.id] : ids.value;
  proxy?.$modal.confirm('是否确认删除编号为"' + idList + '"的数据项？').then(async () => {
    loading.value = true;
    try {
      await delTopic(idList);
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
