<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="100px">
      <el-form-item label="模板名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入模板名称"
          clearable
          @keyup.enter="handleQuery"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="服务商" prop="provider">
        <el-select
          v-model="queryParams.provider"
          placeholder="请选择服务商"
          clearable
          style="width: 150px"
        >
          <el-option label="即梦" value="jm" />
          <el-option label="可灵" value="kl" />
        </el-select>
      </el-form-item>
      <el-form-item label="场景分类" prop="category">
        <el-select
          v-model="queryParams.category"
          placeholder="请选择场景"
          clearable
          style="width: 150px"
        >
          <el-option label="自我介绍" value="self_intro" />
          <el-option label="约会邀请" value="dating" />
          <el-option label="表白/追求" value="confession" />
          <el-option label="日常分享" value="daily" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          style="width: 120px"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
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
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="模板名称" align="center" prop="name" min-width="150" show-overflow-tooltip />
      <el-table-column label="场景分类" align="center" prop="category" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.category === 'self_intro'" type="info">自我介绍</el-tag>
          <el-tag v-else-if="scope.row.category === 'dating'" type="warning">约会邀请</el-tag>
          <el-tag v-else-if="scope.row.category === 'confession'" type="danger">表白/追求</el-tag>
          <el-tag v-else-if="scope.row.category === 'daily'" type="success">日常分享</el-tag>
          <span v-else>{{ scope.row.category }}</span>
        </template>
      </el-table-column>
      <el-table-column label="服务商" align="center" prop="provider" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.provider === 'jm'" type="success">即梦</el-tag>
          <el-tag v-else-if="scope.row.provider === 'kl'" type="primary">可灵</el-tag>
          <span v-else>{{ scope.row.provider }}</span>
        </template>
      </el-table-column>
      <el-table-column label="模板ID" align="center" prop="templateId" min-width="120" show-overflow-tooltip />
      <el-table-column label="封面图" align="center" prop="coverImage" width="100">
        <template #default="scope">
          <el-image
            v-if="scope.row.coverImage"
            :src="scope.row.coverImage"
            :preview-src-list="[scope.row.coverImage]"
            fit="cover"
            style="width: 60px; height: 60px; border-radius: 4px"
            :hide-on-click-modal="true"
          />
          <span v-else style="color: #999">无</span>
        </template>
      </el-table-column>
      <el-table-column label="时长(秒)" align="center" prop="duration" width="90" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 1" type="success">启用</el-tag>
          <el-tag v-else type="danger">禁用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="70" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
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
    
    <!-- 新增/修改对话框 -->
    <add-or-update ref="addOrUpdateRef" @success="getList" />
  </div>
</template>

<script setup lang="ts" name="ShejiaoVideoTemplate">
import { getCurrentInstance, ref } from 'vue';
import { listVideoTemplate, delVideoTemplate } from '@/api/shejiao/videoTemplate';
import { VideoTemplateVO, VideoTemplateQuery } from '@/api/shejiao/videoTemplate/types';
import AddOrUpdate from './add-or-update.vue';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const dataList = ref<VideoTemplateVO[]>([]);
const loading = ref(false);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const addOrUpdateRef = ref();

const queryParams = ref<VideoTemplateQuery>({
  pageNum: 1,
  pageSize: 10,
  name: undefined,
  status: undefined,
  provider: undefined,
  category: undefined
});

/** 查询列表 */
const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listVideoTemplate(queryParams.value);
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
    name: undefined,
    status: undefined,
    provider: undefined,
    category: undefined
  };
  handleQuery();
};

/** 多选框选中数据 */
const handleSelectionChange = (selection: VideoTemplateVO[]) => {
  ids.value = selection.map(item => item.id!);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

/** 新增按钮操作 */
const handleAdd = () => {
  addOrUpdateRef.value?.open();
};

/** 修改按钮操作 */
const handleUpdate = (row?: VideoTemplateVO) => {
  const id = row?.id || ids.value[0];
  addOrUpdateRef.value?.open(id);
};

/** 删除按钮操作 */
const handleDelete = (row?: VideoTemplateVO) => {
  const idList = row?.id ? [row.id] : ids.value;
  proxy?.$modal.confirm('是否确认删除选中的AI视频模板数据？').then(async () => {
    loading.value = true;
    try {
      await delVideoTemplate(idList);
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
