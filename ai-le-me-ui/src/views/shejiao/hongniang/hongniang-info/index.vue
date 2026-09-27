<template>
  <div class="hongniang-info">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="红娘姓名" prop="hongniangName">
        <el-input
          v-model="queryParams.hongniangName"
          placeholder="请输入红娘姓名"
          clearable
          style="width: 240px;"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入手机号"
          clearable
          style="width: 240px;"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          style="width: 240px;"
        >
          <el-option label="全部" :value="null" />
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
          v-hasPermi="['hongniang:hongniangInfo:add']"
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
          v-hasPermi="['hongniang:hongniangInfo:edit']"
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
          v-hasPermi="['hongniang:hongniangInfo:remove']"
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
          v-hasPermi="['hongniang:hongniangInfo:export']"
        >
          导出
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="hongniangInfoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="头像" align="center" prop="avatar" width="80">
        <template #default="scope">
          <el-avatar :size="50" :src="scope.row.avatar" v-if="scope.row.avatar" />
          <el-avatar :size="50" v-else>{{ scope.row.hongniangName?.charAt(0) || 'H' }}</el-avatar>
        </template>
      </el-table-column>
      <el-table-column label="红娘姓名" align="center" prop="hongniangName" />
      <el-table-column label="手机号" align="center" prop="phone" />
      <el-table-column label="微信号" align="center" prop="wechat" />
      <el-table-column label="所属机构" align="center" prop="companyName" />
      <el-table-column label="服务地区" align="center" prop="serviceArea" />
      <el-table-column label="等级" align="center" prop="level" width="100">
        <template #default="scope">
          <span v-if="scope.row.level == 1">普通</span>
          <span v-else-if="scope.row.level == 2">高级</span>
          <span v-else-if="scope.row.level == 3">金牌</span>
          <span v-else>{{ scope.row.level }}</span>
        </template>
      </el-table-column>
      <el-table-column label="管理用户数" align="center" prop="totalUsers" width="100" />
      <el-table-column label="组织活动数" align="center" prop="totalActivities" width="100" />
      <el-table-column label="认证状态" align="center" prop="certificationStatus" width="100">
        <template #default="scope">
          <span v-if="scope.row.certificationStatus == 0">未认证</span>
          <span v-else-if="scope.row.certificationStatus == 1">已认证</span>
          <span v-else-if="scope.row.certificationStatus == 2">认证失败</span>
          <span v-else>{{ scope.row.certificationStatus }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="1"
            :inactive-value="0"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['hongniang:hongniangInfo:edit']">
            修改
          </el-button>
          <el-button link type="primary" icon="Upload" @click="handleImportUsers(scope.row)" v-hasPermi="['hongniang:hongniangInfo:import']">
            导入用户
          </el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['hongniang:hongniangInfo:remove']">
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

    <!-- 添加或修改红娘信息对话框 -->
    <el-dialog :title="dialog.title" v-model="dialog.visible" width="600px" append-to-body>
      <el-form ref="hongniangInfoRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="红娘头像" prop="avatar">
          <el-upload
            class="avatar-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleAvatarSuccess">
            <img v-if="form.avatar" :src="form.avatar" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="红娘姓名" prop="hongniangName">
          <el-input v-model="form.hongniangName" placeholder="请输入红娘姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="微信号" prop="wechat">
          <el-input v-model="form.wechat" placeholder="请输入微信号" />
        </el-form-item>
        <el-form-item label="所属机构" prop="companyName">
          <el-select
            v-if="isSuperAdmin"
            v-model="form.companyName"
            filterable
            placeholder="请选择所属机构/公司"
            style="width: 100%">
            <el-option
              v-for="tenant in tenantOptions"
              :key="tenant.tenantId"
              :label="tenant.companyName"
              :value="tenant.companyName">
            </el-option>
          </el-select>
          <el-input v-else v-model="form.companyName" disabled placeholder="当前租户机构" />
        </el-form-item>
        <el-form-item label="服务地区" prop="serviceArea">
          <el-input v-model="form.serviceArea" placeholder="如：北京市朝阳区" />
        </el-form-item>
        <el-form-item label="红娘等级" prop="level">
          <el-radio-group v-model="form.level">
            <el-radio :label="1">普通</el-radio>
            <el-radio :label="2">高级</el-radio>
            <el-radio :label="3">金牌</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="认证状态" prop="certificationStatus">
          <el-radio-group v-model="form.certificationStatus">
            <el-radio :label="0">未认证</el-radio>
            <el-radio :label="1">已认证</el-radio>
            <el-radio :label="2">认证失败</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="个人简介" prop="intro">
          <el-input v-model="form.intro" type="textarea" placeholder="请输入个人简介" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入用户对话框 -->
    <el-dialog v-model="importDialog.visible" title="导入用户" width="500px" append-to-body>
      <el-form :model="importForm">
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :file-list="fileList"
            :limit="1"
            accept=".pdf,.docx"
            drag>
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                仅支持PDF和DOCX格式，文件需包含：姓名、电话、性别、年龄等信息
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="importDialog.visible = false">取 消</el-button>
          <el-button type="primary" @click="submitImport" :loading="importLoading">确定导入</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { listHongniangInfo, getHongniangInfo, delHongniangInfo, addHongniangInfo, updateHongniangInfo, exportHongniangInfo, changeHongniangInfoStatus, importUsers } from '@/api/hongniang/hongniangInfo';
import { getCurrentInstance, ref, reactive, onMounted } from 'vue';
import { ElForm, ElMessage, ElMessageBox } from 'element-plus';
import { UploadFilled, Plus } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/modules/user';
import { listTenant } from '@/api/system/tenant'; // 导入租户API
import { getInfo as getUserInfo } from '@/api/login';
import { TenantVO, TenantQuery } from '@/api/system/tenant/types'; // 导入租户类型
import { getToken } from '@/utils/auth';

const { proxy } = getCurrentInstance() as any;

const hongniangInfoList = ref<any[]>([]);
const loading = ref(true);
type FormInstance = InstanceType<typeof ElForm>;
const queryRef = ref<FormInstance>();
const hongniangInfoRef = ref<FormInstance>();
const ids = ref<number[]>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const showSearch = ref(true);

// 添加判断是否为超级管理员的变量
const isSuperAdmin = ref(false);

// 当前租户信息（从 /system/user/getInfo 获取）
const currentTenantId = ref<string>('');
const currentDeptName = ref<string>('');

// 租户选项列表
const tenantOptions = ref<TenantVO[]>([]);

const dialog = reactive({
  visible: false,
  title: '',
});

// 导入用户对话框
const importDialog = reactive({
  visible: false,
});

const importForm = reactive({
  hongniangId: undefined as number | undefined,
});

const importLoading = ref(false);
const fileList = ref<any[]>([]);
const selectedFile = ref<File | null>(null);
const uploadRef = ref();

// 上传配置
const uploadUrl = ref('');
const uploadHeaders = ref({});

const initFormData = {
  id: undefined,
  hongniangName: '',
  avatar: '',
  phone: '',
  wechat: '',
  companyName: '',
  serviceArea: '',
  level: 1,
  certificationStatus: 0,
  status: 1,
  intro: ''
};

const form = reactive(initFormData);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  hongniangName: undefined,
  phone: undefined,
  status: null,
});

const rules = reactive({
  hongniangName: [
    { required: true, message: "红娘姓名不能为空", trigger: "blur" },
    { min: 2, max: 20, message: "红娘姓名长度必须在2到20个字符之间", trigger: "blur" }
  ],
  phone: [
    { required: true, message: "手机号不能为空", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "请输入正确的手机号码", trigger: "blur" }
  ]
});

// 检查用户权限
const checkUserPermissions = () => {
  // 从用户store获取角色信息
  try {
    const userStore = useUserStore();
    const userRoles = userStore.roles;
    console.log('当前用户角色:', userRoles); // 调试日志
    // 检查是否包含超级管理员角色
    if (userRoles) {
      isSuperAdmin.value = userRoles.includes('superadmin');
      console.log('isSuperAdmin 值:', isSuperAdmin.value); // 调试日志
    } else {
      isSuperAdmin.value = false;
    }
  } catch (e) {
    console.error('检查用户权限时出错:', e);
    isSuperAdmin.value = false;
  }
};

// 加载当前用户的租户ID和所属机构名称
const loadCurrentTenantInfo = async () => {
  try {
    const res = await getUserInfo();
    const user = res.data?.user;
    if (user) {
      currentTenantId.value = user.tenantId;
      currentDeptName.value = user.deptName;
      console.log('当前用户租户及机构:', currentTenantId.value, currentDeptName.value);
    }
  } catch (e) {
    console.error('获取当前用户信息失败:', e);
  }
};

/** 查询红娘信息列表 */
const getList = () => {
  loading.value = true;
  listHongniangInfo(queryParams).then((res: any) => {
    loading.value = false;
    hongniangInfoList.value = res.page?.list || [];
    total.value = res.page?.totalCount || 0;
  }).catch((error) => {
    loading.value = false;
    console.error('获取红娘列表失败:', error);
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
  hongniangInfoRef.value?.resetFields();
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
const handleAdd = async () => {
  reset();
  checkUserPermissions(); // 检查用户权限
  
  // 配置上传接口
  uploadUrl.value = import.meta.env.VITE_APP_BASE_API + '/resource/oss/upload';
  uploadHeaders.value = {
    Authorization: 'Bearer ' + getToken(),
    clientid: import.meta.env.VITE_APP_CLIENT_ID
  };
  
  console.log('新增操作 - isSuperAdmin 值:', isSuperAdmin.value); // 调试日志
  
  // 如果非超级管理员，设置所属机构为当前登录用户的机构名称
  if (!isSuperAdmin.value) {
    if (currentDeptName.value) {
      form.companyName = currentDeptName.value;
      console.log('设置所属机构为当前用户机构:', currentDeptName.value); // 调试日志
    } else {
      form.companyName = '';
      console.log('当前用户机构名称为空'); // 调试日志
    }
  } else {
    // 超级管理员 - 初始化为空值，让用户从下拉框中选择
    form.companyName = '';
    console.log('超级管理员，初始化公司名称为空'); // 调试日志
  }
  
  dialog.visible = true;
  dialog.title = "添加红娘信息";
};

/** 修改按钮操作 */
const handleUpdate = async (row?: any) => {
  reset();
  checkUserPermissions(); // 检查用户权限
  
  // 配置上传接口
  uploadUrl.value = import.meta.env.VITE_APP_BASE_API + '/resource/oss/upload';
  uploadHeaders.value = {
    Authorization: 'Bearer ' + getToken(),
    clientid: import.meta.env.VITE_APP_CLIENT_ID
  };
  
  console.log('修改操作 - isSuperAdmin 值:', isSuperAdmin.value); // 调试日志
  const hongniangId = row.id || ids.value[0];
  getHongniangInfo(hongniangId).then((res: any) => {
    Object.assign(form, res.hongniang || res.data || res);
    // 如果不是超级管理员，在修改时也要设置为当前登录用户所属机构，但保持只读
    if (!isSuperAdmin.value) {
      if (currentDeptName.value) {
        form.companyName = currentDeptName.value;
        console.log('修改操作 - 设置所属机构为当前用户机构:', currentDeptName.value); // 调试日志
      }
    } else {
      // 超级管理员 - 确保可以编辑
      console.log('超级管理员修改，保留原始公司名称:', form.companyName); // 调试日志
    }
    dialog.visible = true;
    dialog.title = "修改红娘信息";
  });
};

/** 提交按钮 */
const submitForm = () => {
  hongniangInfoRef.value?.validate((valid: boolean) => {
    if (valid) {
      if (form.id !== undefined) {
        updateHongniangInfo(form).then((res: any) => {
          proxy.$modal.msgSuccess("修改成功");
          dialog.visible = false;
          getList();
        }).catch((error) => {
          console.error('修改失败:', error);
        });
      } else {
        addHongniangInfo(form).then((res: any) => {
          proxy.$modal.msgSuccess("新增成功");
          dialog.visible = false;
          getList();
        }).catch((error) => {
          console.error('新增失败:', error);
        });
      }
    }
  });
};

/** 删除按钮操作 */
const handleDelete = (row?: any) => {
  const hongniangIds = row?.id ? [row.id] : ids.value;
  proxy.$modal.confirm('是否确认删除红娘信息编号为"' + hongniangIds + '"的数据项？').then(function() {
    return delHongniangInfo(hongniangIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
};

/** 导出按钮操作 */
const handleExport = () => {
  ElMessage.warning('导出功能暂未实现，请等待后端接口开发');
  // proxy.$modal.confirm('是否确认导出所有红娘信息数据项？').then(function() {
  //   return exportHongniangInfo(queryParams);
  // }).then((response: any) => {
  //   proxy.download(response);
  // }).catch(() => {});
};

/** 修改状态 */
const handleStatusChange = (row: any) => {
  let text = row.status === 1 ? "启用" : "停用";
  const oldStatus = row.status;
  proxy.$modal.confirm('确认要' + text + '该红娘吗?').then(function() {
    // 直接调用修改接口，将 status 作为数据一部分提交
    return updateHongniangInfo({
      id: row.id,
      status: row.status
    } as any);
  }).then(() => {
    proxy.$modal.msgSuccess(text + "成功");
    getList(); // 刷新列表
  }).catch(function() {
    // 发生错误时恢复原来的状态
    row.status = oldStatus;
  });
};

/** 导入用户操作 */
const handleImportUsers = (row: any) => {
  importForm.hongniangId = row.id;
  importDialog.visible = true;
  fileList.value = [];
  selectedFile.value = null;
};

/** 文件选择 */
const handleFileChange = (file: any) => {
  selectedFile.value = file.raw;
};

/** 提交导入 */
const submitImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件');
    return;
  }

  if (!importForm.hongniangId) {
    ElMessage.error('红娘ID不能为空');
    return;
  }

  importLoading.value = true;
  try {
    const res = await importUsers(selectedFile.value, importForm.hongniangId);
    ElMessage.success(res.msg || '导入成功');
    importDialog.visible = false;
    getList();
  } catch (error: any) {
    ElMessage.error(error.message || '导入失败');
  } finally {
    importLoading.value = false;
  }
};

/** 头像上传成功回调 */
const handleAvatarSuccess = (response: any) => {
  form.avatar = response.data.url;
};

onMounted(() => {
  console.log('页面挂载 - 检查用户权限'); // 调试日志
  checkUserPermissions(); // 页面加载时检查用户权限
  loadCurrentTenantInfo(); // 加载当前用户的租户及机构信息
  loadTenantOptions(); // 加载租户选项
  getList();
});

/** 加载租户选项 */
const loadTenantOptions = async () => {
  try {
    const response = await listTenant({ pageNum: 1, pageSize: 1000, tenantId: '', contactUserName: '', contactPhone: '', companyName: '' } as TenantQuery); // 获取所有租户
    tenantOptions.value = response.rows || [];
    console.log('租户选项加载完成:', tenantOptions.value);
  } catch (error) {
    console.error('加载租户选项失败:', error);
    tenantOptions.value = [];
  }
};
</script>

<style scoped>
.hongniang-info {
  padding: 20px;
}

.avatar-uploader .avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-uploader .avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
  border-radius: 50%;
}

.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
}
</style>