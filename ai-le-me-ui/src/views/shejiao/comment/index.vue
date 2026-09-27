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
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
        >批量删除</el-button>
      </el-col>
    </el-row>

    <el-table
      v-loading="loading"
      :data="dataList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="子评论" align="center" prop="pid" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.pid == 0" type="success">否</el-tag>
          <el-tag v-else type="warning">是</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评论作者ID" align="center" prop="uid" width="120" />
      <el-table-column label="被回复用户ID" align="center" prop="toUid" width="120" />
      <el-table-column label="评论帖子ID" align="center" prop="postId" width="120" />
      <el-table-column label="评论内容" align="center" prop="content" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.content.length > 15 ? scope.row.content.slice(0, 15) + '...' : scope.row.content }}
          <el-button 
            v-if="scope.row.content.length > 15"
            link
            type="primary"
            size="small"
            @click="showContent(scope.row.content)"
          >更多</el-button>
        </template>
      </el-table-column>
      <el-table-column label="评论状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status == 1" type="success">展示</el-tag>
          <el-tag v-else type="danger">不展示</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评论时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
          >处理</el-button>
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
    
    <!-- 处理评论对话框 -->
    <add-or-update ref="addOrUpdateRef" @success="getList" />
    
    <!-- 评论内容弹窗 -->
    <el-dialog v-model="contentDialogVisible" title="评论内容" width="500px">
      <div style="white-space: pre-wrap; word-break: break-all;">
        {{ currentContent }}
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="ShejiaoComment">
import { getCurrentInstance, ref, reactive, toRefs } from 'vue';
import { listComment, getComment, delComment, addComment, updateComment } from '@/api/shejiao/comment';
import { CommentVO, CommentQuery, CommentForm } from '@/api/shejiao/comment/types';
import AddOrUpdate from './add-or-update.vue';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const dataList = ref<CommentVO[]>([]);
const loading = ref(false);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const addOrUpdateRef = ref();
const contentDialogVisible = ref(false);
const currentContent = ref('');

const queryParams = ref<CommentQuery>({
  pageNum: 1,
  pageSize: 10,
  key: undefined
});

/** 查询列表 */
const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listComment(queryParams.value);
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
const handleSelectionChange = (selection: CommentVO[]) => {
  ids.value = selection.map(item => item.id!);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

/** 显示评论内容 */
const showContent = (content: string) => {
  currentContent.value = content;
  contentDialogVisible.value = true;
};

/** 修改按钮操作 */
const handleUpdate = (row?: CommentVO) => {
  const id = row?.id || ids.value[0];
  addOrUpdateRef.value?.open(id);
};

/** 删除按钮操作 */
const handleDelete = (row?: CommentVO) => {
  const idList = row?.id ? [row.id] : ids.value;
  proxy?.$modal.confirm('是否确认删除编号为"' + idList + '"的评论数据？').then(async () => {
    loading.value = true;
    try {
      await delComment(idList);
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
