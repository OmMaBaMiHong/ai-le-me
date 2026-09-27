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
          @click="handleBatchDelete"
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

<script setup lang="ts" name="ShejiaoTags">
import { getCurrentInstance, ref } from 'vue';
import type { ComponentInternalInstance } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { listTags, getTags, delTags, addTags, updateTags } from '@/api/shejiao/tags';
import { TagsVO, TagsQuery, TagsForm } from '@/api/shejiao/tags/types';
const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const dataList = ref<TagsVO[]>([]);
const loading = ref(false);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const queryFormRef = ref<FormInstance>();
const formRef = ref<FormInstance>();

const queryParams = ref<TagsQuery>({
  pageNum: 1,
  pageSize: 10,
  key: undefined,
  status: undefined
});

const dialogVisible = ref(false);
const dialogTitle = ref('');
const form = ref<TagsForm>({
  id: undefined,
  tagName: '',
  tagCategory: '',
  tagType: 1,
  sort: 0,
  status: 1
});

const rules = ref<FormRules>({
  tagName: [{ required: true, message: '标签名称不能为空', trigger: 'blur' }],
  tagCategory: [{ required: true, message: '分类不能为空', trigger: 'blur' }],
  tagType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
});

/** 查询列表 */
const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listTags(queryParams.value);
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
  queryFormRef.value?.resetFields();
  handleQuery();
};

/** 多选框选中数据 */
const handleSelectionChange = (selection: TagsVO[]) => {
  ids.value = selection.map(item => item.id!);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

/** 新增按钮操作 */
const handleAdd = () => {
  dialogTitle.value = '新增标签';
  form.value = {
    id: undefined,
    tagName: '',
    tagCategory: '',
    tagType: 1,
    sort: 0,
    status: 1
  };
  dialogVisible.value = true;
};

/** 修改按钮操作 */
const handleUpdate = async (row?: TagsVO) => {
  const id = row?.id || ids.value[0];
  if (!id) {
    proxy?.$modal.msgWarning('请选择要修改的数据');
    return;
  }
  const res: any = await getTags(id as number);
  const tag = res.tag || res.data || {};
  form.value = {
    id: tag.id,
    tagName: tag.tagName,
    tagCategory: tag.tagCategory,
    tagType: tag.tagType,
    sort: tag.sort,
    status: tag.status
  };
  dialogTitle.value = '修改标签';
  dialogVisible.value = true;
};

/** 提交表单 */
const submitForm = () => {
  formRef.value?.validate(async valid => {
    if (!valid) return;
    loading.value = true;
    try {
      if (form.value.id) {
        await updateTags(form.value as TagsForm);
        proxy?.$modal.msgSuccess('修改成功');
      } else {
        await addTags(form.value as TagsForm);
        proxy?.$modal.msgSuccess('新增成功');
      }
      dialogVisible.value = false;
      await getList();
    } finally {
      loading.value = false;
    }
  });
};

/** 删除按钮操作 */
const handleDelete = (row?: TagsVO) => {
  const idList = row?.id ? [row.id] : [];
  if (!idList.length) {
    proxy?.$modal.msgWarning('请选择要删除的数据');
    return;
  }
  doDelete(idList);
};

/** 批量删除 */
const handleBatchDelete = () => {
  const idList = ids.value;
  if (!idList.length) {
    proxy?.$modal.msgWarning('请选择要删除的数据');
    return;
  }
  doDelete(idList);
};

const doDelete = (idList: Array<string | number>) => {
  proxy?.$modal.confirm('是否确认删除编号为"' + idList + '"的数据项？').then(async () => {
    loading.value = true;
    try {
      await delTags(idList);
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
