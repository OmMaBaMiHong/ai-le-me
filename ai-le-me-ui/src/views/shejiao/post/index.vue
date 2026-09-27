<template>
  <div class="app-container">
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="72px">
      <el-form-item label="关键词" prop="key">
        <el-input
          v-model="queryParams.key"
          placeholder="标题 / 内容 / 帖子ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="帖子类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="全部类型" clearable style="width: 140px">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">批量删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="78" />
      <el-table-column label="发布用户" min-width="170">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :src="row.avatar" :size="40">{{ getUserName(row).slice(0, 1) }}</el-avatar>
            <div class="user-meta">
              <div class="user-name">{{ getUserName(row) }}</div>
              <div class="user-sub">UID {{ row.uid }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="帖子内容" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="post-title">{{ row.title || '无标题' }}</div>
          <div class="post-sub">{{ row.content || row.brief || '暂无正文' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="圈子 / 话题" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <div>{{ row.topicName || '-' }}</div>
          <div class="post-sub">{{ row.discussTitle || '未关联话题' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="媒体" width="124" align="center">
        <template #default="{ row }">
          <div v-if="row.mediasList?.length" class="media-cell">
            <el-image
              v-if="row.type === 1"
              :src="row.mediasList[0]"
              :preview-src-list="row.mediasList"
              fit="cover"
              class="media-thumb"
            />
            <div v-else class="media-video">
              <el-icon><VideoPlay /></el-icon>
            </div>
            <div class="media-count">{{ row.mediasList.length }} 个素材</div>
          </div>
          <span v-else class="muted-text">无素材</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" align="center" width="100">
        <template #default="{ row }">
          <el-tag effect="light">{{ formatType(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="互动数据" align="center" width="140">
        <template #default="{ row }">
          <div class="metric-line">浏览 {{ row.readCount || 0 }}</div>
          <div class="metric-line">评论 {{ row.commentCount || 0 }} / 点赞 {{ row.collectionCount || 0 }}</div>
        </template>
      </el-table-column>
      <el-table-column label="帖子属性" align="center" width="120">
        <template #default="{ row }">
          <div class="metric-line">{{ formatCut(row.cut) }}</div>
          <div class="metric-line">{{ row.isPrivate === 1 ? '私密圈可见' : '公开可见' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : row.status === 1 ? 'warning' : 'danger'">
            {{ formatStatus(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="地址" prop="address" min-width="140" show-overflow-tooltip />
      <el-table-column label="发布时间" align="center" prop="createTime" width="170" />
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">修改</el-button>
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

    <add-or-update ref="addOrUpdateRef" @success="getList" />
  </div>
</template>

<script setup lang="ts" name="ShejiaoPost">
import { getCurrentInstance, ref } from 'vue';
import { VideoPlay } from '@element-plus/icons-vue';
import { listPost, delPost } from '@/api/shejiao/post';
import { PostVO, PostQuery } from '@/api/shejiao/post/types';
import AddOrUpdate from './add-or-update.vue';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const typeOptions = [
  { label: '图文', value: 1 },
  { label: '视频', value: 2 },
  { label: '长文', value: 3 },
  { label: '投票', value: 4 }
];

const statusOptions = [
  { label: '正常', value: 0 },
  { label: '待审核', value: 1 },
  { label: '已拒绝', value: 2 }
];

const dataList = ref<PostVO[]>([]);
const loading = ref(false);
const ids = ref<Array<string | number>>([]);
const multiple = ref(true);
const total = ref(0);
const addOrUpdateRef = ref();
const queryFormRef = ref();

const queryParams = ref<PostQuery>({
  pageNum: 1,
  pageSize: 10,
  key: undefined,
  status: undefined,
  type: undefined
});

const getUserName = (row: PostVO) => {
  return row.userInfo?.username || `用户 ${row.uid || '-'}`;
};

const formatType = (value?: number) => {
  return typeOptions.find((item) => item.value === value)?.label || '未知';
};

const formatStatus = (value?: number) => {
  return statusOptions.find((item) => item.value === value)?.label || '未知';
};

const formatCut = (value?: number) => {
  if (value === 1) return '付费帖';
  if (value === 2) return '红包帖';
  return '普通帖';
};

const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listPost(queryParams.value);
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

const handleSelectionChange = (selection: PostVO[]) => {
  ids.value = selection.map((item) => item.id!);
  multiple.value = !selection.length;
};

const handleAdd = () => {
  addOrUpdateRef.value?.open();
};

const handleUpdate = (row?: PostVO) => {
  const id = row?.id || ids.value[0];
  if (!id) return;
  addOrUpdateRef.value?.open(id);
};

const handleDelete = (row?: PostVO) => {
  const idList = row?.id ? [row.id] : ids.value;
  if (!idList.length) return;
  proxy?.$modal
    .confirm(`是否确认删除帖子：${idList.join(', ')}？`)
    .then(async () => {
      loading.value = true;
      try {
        await delPost(idList);
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
.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-meta,
.media-cell {
  min-width: 0;
}

.user-name,
.post-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.user-sub,
.post-sub,
.muted-text,
.media-count,
.metric-line {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.post-sub {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.media-thumb,
.media-video {
  width: 56px;
  height: 56px;
  border-radius: 12px;
}

.media-video {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  color: var(--el-color-primary);
  font-size: 22px;
}
</style>
