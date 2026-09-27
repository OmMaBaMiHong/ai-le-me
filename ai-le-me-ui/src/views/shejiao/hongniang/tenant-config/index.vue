<template>
  <div class="tenant-config">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="租户ID" prop="tenantId">
        <el-input
          v-model="queryParams.tenantId"
          placeholder="请输入租户ID"
          clearable
          style="width: 240px;"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="配置键" prop="configKey">
        <el-input
          v-model="queryParams.configKey"
          placeholder="请输入配置键"
          clearable
          style="width: 240px;"
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
          v-hasPermi="['hongniang:tenantConfig:add']"
        >
          新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['hongniang:tenantConfig:edit']"
        >
          修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['hongniang:tenantConfig:remove']"
        >
          删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['hongniang:tenantConfig:export']"
        >
          导出
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="tenantConfigList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="租户ID" align="center" prop="tenantId" width="100" />
      <el-table-column label="配置键" align="center" prop="configKey" />
      <el-table-column label="配置值" align="center" prop="configValue" width="200" show-overflow-tooltip />
      <el-table-column label="配置类型" align="center" prop="configType" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.configType === 'STRING'" type="info">字符串</el-tag>
          <el-tag v-else-if="scope.row.configType === 'NUMBER'" type="success">数字</el-tag>
          <el-tag v-else-if="scope.row.configType === 'BOOLEAN'" type="warning">布尔</el-tag>
          <el-tag v-else-if="scope.row.configType === 'JSON'" type="primary">JSON</el-tag>
          <el-tag v-else type="info">{{ scope.row.configType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['hongniang:tenantConfig:edit']">
            修改
          </el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['hongniang:tenantConfig:remove']">
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

    <!-- 添加或修改租户配置对话框 -->
    <el-dialog :title="dialog.title" v-model="dialog.visible" width="500px" append-to-body>
      <el-form ref="tenantConfigRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="租户ID" prop="tenantId">
          <el-input v-model="form.tenantId" placeholder="请输入租户ID" />
        </el-form-item>
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="form.configKey" placeholder="请输入配置键" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" type="textarea" placeholder="请输入配置值" :rows="3" />
        </el-form-item>
        <el-form-item label="配置类型" prop="configType">
          <el-select v-model="form.configType" placeholder="请选择配置类型">
            <el-option label="字符串" value="STRING" />
            <el-option label="数字" value="NUMBER" />
            <el-option label="布尔" value="BOOLEAN" />
            <el-option label="JSON" value="JSON" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue';
import { ElForm, ElMessage, ElMessageBox } from 'element-plus';
import { listTenantConfig, getTenantConfig, delTenantConfig, addTenantConfig, updateTenantConfig, exportTenantConfig, batchDelTenantConfig } from '@/api/hongniang/tenantConfig';

const { proxy } = getCurrentInstance() as any;

const tenantConfigList = ref<any[]>([]);
const loading = ref(true);
type FormInstance = InstanceType<typeof ElForm>;
const queryRef = ref<FormInstance>();
const tenantConfigRef = ref<FormInstance>();
const ids = ref<number[]>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const showSearch = ref(true);

const dialog = reactive({
  visible: false,
  title: '',
});

const initFormData = {
  id: undefined,
  tenantId: '',
  configKey: '',
  configValue: '',
  configType: 'STRING',
  remark: ''
};

const form = reactive(initFormData);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  tenantId: undefined,
  configKey: undefined,
});

const rules = reactive({
  tenantId: [
    { required: true, message: "租户ID不能为空", trigger: "blur" }
  ],
  configKey: [
    { required: true, message: "配置键不能为空", trigger: "blur" }
  ],
  configValue: [
    { required: true, message: "配置值不能为空", trigger: "blur" }
  ],
  configType: [
    { required: true, message: "配置类型不能为空", trigger: "change" }
  ]
});

/** 查询租户配置列表 */
const getList = () => {
  loading.value = true;
  listTenantConfig(queryParams).then((res) => {
    loading.value = false;
    tenantConfigList.value = res.rows;
    total.value = res.total;
  });
};

// 取消按钮
const cancel = () => {
  dialog.visible = false;
  reset();
};

// 表单重置
const reset = () => {
  Object.assign(form, initFormData);
  tenantConfigRef.value?.resetFields();
};

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};

/** 重置按钮操作 */
const resetQuery = () => {
  queryRef.value?.resetFields();
  handleQuery();
};

// 多选框选中数据
const handleSelectionChange = (selection: any[]) => {
  ids.value = selection.map(item => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

/** 新增按钮操作 */
const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = "添加租户配置";
};

/** 修改按钮操作 */
const handleUpdate = (row?: any) => {
  reset();
  const id = row.id || ids.value[0];
  getTenantConfig(id).then(res => {
    Object.assign(form, res.data);
    dialog.visible = true;
    dialog.title = "修改租户配置";
  });
};

/** 提交按钮 */
const submitForm = () => {
  tenantConfigRef.value?.validate((valid: boolean) => {
    if (valid) {
      if (form.id !== undefined) {
        updateTenantConfig(form).then(res => {
          proxy.$modal.msgSuccess("修改成功");
          dialog.visible = false;
          getList();
        });
      } else {
        addTenantConfig(form).then(res => {
          proxy.$modal.msgSuccess("新增成功");
          dialog.visible = false;
          getList();
        });
      }
    }
  });
};

/** 删除按钮操作 */
const handleDelete = (row?: any) => {
  const id = row?.id || ids.value;
  proxy.$modal.confirm('是否确认删除租户配置编号为"' + id + '"的数据项？').then(function() {
    return delTenantConfig(id);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
};

/** 批量删除按钮操作 */
const handleBatchDelete = () => {
  if (ids.value.length === 0) {
    proxy.$modal.msgError("请至少选择一条记录");
    return;
  }
  
  proxy.$modal.confirm('是否确认删除选中的' + ids.value.length + '条数据项？').then(function() {
    return batchDelTenantConfig(ids.value);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("批量删除成功");
  }).catch(() => {});
};

/** 导出按钮操作 */
const handleExport = () => {
  proxy.$modal.confirm('是否确认导出所有租户配置数据项？').then(function() {
    return exportTenantConfig(queryParams);
  }).then((response: any) => {
    proxy.download(response);
  }).catch(() => {});
};

/** 时间格式化 */
const parseTime = (time: any, pattern?: string) => {
  if (arguments.length === 0 || !time) {
    return null;
  }
  const format = pattern || '{y}-{m}-{d} {h}:{i}:{s}';
  let date;
  if (typeof time === 'object') {
    date = time;
  } else {
    if ((typeof time === 'string') && (/^[0-9]+$/.test(time))) {
      time = parseInt(time);
    } else if (typeof time === 'string') {
      time = time.replace(new RegExp(/-/gm), '/');
    }
    if ((typeof time === 'number') && (time.toString().length === 10)) {
      time = time * 1000;
    }
    date = new Date(time);
  }
  const formatObj: any = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay()
  };
  const time_str = format.replace(/{(y|m|d|h|i|s|a)+}/g, (result, key) => {
    let value = formatObj[key];
    if (key === 'a') return ['一', '二', '三', '四', '五', '六', '日'][value - 1];
    if (result.length > 0 && value < 10) {
      value = '0' + value;
    }
    return value || 0;
  });
  return time_str;
};

onMounted(() => {
  getList();
});
</script>

<style scoped>
.tenant-config {
  padding: 20px;
}
</style>