<template>
  <div class="hongniang-user">
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
      <el-form-item label="用户查询" prop="userKeyword">
        <el-input
          v-model="queryParams.userKeyword"
          placeholder="请输入手机号/姓名/UID"
          clearable
          style="width: 240px;"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="管理编号" prop="hongniangUserNo">
        <el-input
          v-model="queryParams.hongniangUserNo"
          placeholder="请输入红娘内编号"
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
          @click="handleAssignUser"
          v-hasPermi="['hongniang:hongniangUser:add']"
        >
          分配用户
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleBatchUnbind"
          v-hasPermi="['hongniang:hongniangUser:remove']"
        >
          批量解绑
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['hongniang:hongniangUser:export']"
        >
          导出
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="hongniangUserList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="红娘ID" align="center" prop="hongniangId" width="80" />
      <el-table-column label="红娘姓名" align="center" prop="hongniangName" />
      <el-table-column label="管理编号" align="center" prop="hongniangUserNo" width="100" />
      <el-table-column label="红娘手机" align="center" prop="hongniangPhone" />
      <el-table-column label="用户头像" align="center" width="90">
        <template #default="scope">
          <el-avatar :size="42" :src="scope.row.avatar">
            {{ scope.row.username?.charAt(0) || 'U' }}
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column label="用户信息" min-width="180">
        <template #default="scope">
          <div>{{ scope.row.username || '-' }}</div>
          <div class="user-subline">UID: {{ scope.row.userId }} / {{ scope.row.userMobile || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="用户性别" align="center" prop="userGender" width="80">
        <template #default="scope">
          <span v-if="scope.row.userGender == 1">男</span>
          <span v-else-if="scope.row.userGender == 0">保密</span>
          <span v-else-if="scope.row.userGender == 2">女</span>
          <span v-else>{{ scope.row.userGender }}</span>
        </template>
      </el-table-column>
      <el-table-column label="年龄" align="center" prop="userAge" width="80" />
      <el-table-column label="关联时间" align="center" prop="relationTime" width="150" />
      <el-table-column label="操作" align="center" width="340" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['hongniang:hongniangUser:edit']">
            修改
          </el-button>
          <el-button link type="primary" icon="User" @click="viewUserInfo(scope.row.userId)">
            查看用户
          </el-button>
          <el-button
            link
            type="primary"
            icon="Connection"
            @click="createMatchCase(scope.row)"
            v-hasPermi="['hongniang:matchCase:add']"
          >
            牵线
          </el-button>
          <el-button link type="primary" icon="UserFilled" @click="viewHongniangInfo(scope.row.hongniangId)">
            查看红娘
          </el-button>
          <el-button link type="primary" icon="Delete" @click="handleUnbind(scope.row)" v-hasPermi="['hongniang:hongniangUser:remove']">
            解绑
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

    <!-- 分配用户弹窗 -->
    <el-dialog title="分配用户给红娘" v-model="assignDialog.visible" width="600px" append-to-body>
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="120px">
        <el-form-item label="选择红娘" prop="hongniangId">
          <el-select
            v-model="assignForm.hongniangId"
            placeholder="请选择红娘"
            style="width: 100%"
            clearable
            @change="handleAssignHongniangChange"
          >
            <el-option
              v-for="hongniang in hongniangList"
              :key="hongniang.id"
              :label="`${hongniang.hongniangName} (${hongniang.phone})`"
              :value="hongniang.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="选择用户" prop="userId">
          <el-select
            v-model="assignForm.userId"
            placeholder="请输入手机号/姓名/UID搜索"
            style="width: 100%"
            filterable
            remote
            reserve-keyword
            clearable
            :loading="assignUserLoading"
            :remote-method="searchAssignUsers"
          >
            <el-option
              v-for="user in userList"
              :key="user.uid"
              :label="formatAssignUserLabel(user)"
              :value="user.uid">
            </el-option>
          </el-select>
          <div class="assign-tip">支持按手机号、姓名、UID 搜索，自动过滤当前红娘已关联用户</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="confirmAssignUser" :loading="assignLoading">确定分配</el-button>
          <el-button @click="cancelAssign">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 查看用户详情弹窗 -->
    <el-dialog title="用户详情" v-model="userDetailVisible" width="800px" append-to-body>
      <div v-if="selectedUser">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">{{ selectedUser.uid }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ selectedUser.username }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ selectedUser.mobile }}</el-descriptions-item>
          <el-descriptions-item label="性别">
            <el-tag v-if="selectedUser.gender == 1" type="success">男</el-tag>
            <el-tag v-else-if="selectedUser.gender == 0" type="info">保密</el-tag>
            <el-tag v-else-if="selectedUser.gender == 2" type="danger">女</el-tag>
            <el-tag v-else type="info">{{ selectedUser.gender }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="年龄">{{ selectedUser.age || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="头像">
            <img v-if="selectedUser.avatar" :src="selectedUser.avatar" style="width: 50px; height: 50px; border-radius: 50%;" />
            <span v-else>无</span>
          </el-descriptions-item>
          <el-descriptions-item label="个性签名">{{ selectedUser.intro || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="积分">{{ selectedUser.integral || 0 }}</el-descriptions-item>
          <el-descriptions-item label="余额">{{ selectedUser.money || 0 }}</el-descriptions-item>
          <el-descriptions-item label="用户类型">
            <el-tag v-if="selectedUser.vip == 0" type="success">普通用户</el-tag>
            <el-tag v-else-if="selectedUser.vip == 1" type="warning">会员用户</el-tag>
            <el-tag v-else type="info">{{ selectedUser.vip }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="账号类型">
            <el-tag v-if="selectedUser.type == 1" type="success">官方账号</el-tag>
            <el-tag v-else-if="selectedUser.type == 0" type="info">普通账号</el-tag>
            <el-tag v-else type="warning">虚拟账号</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag v-if="selectedUser.status == 0" type="success">正常</el-tag>
            <el-tag v-else-if="selectedUser.status == 1" type="danger">禁用</el-tag>
            <el-tag v-else type="info">{{ selectedUser.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ selectedUser.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ selectedUser.updateTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 查看红娘详情弹窗 -->
    <el-dialog title="红娘详情" v-model="hongniangDetailVisible" width="600px" append-to-body>
      <div v-if="selectedHongniang">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="红娘ID">{{ selectedHongniang.id }}</el-descriptions-item>
          <el-descriptions-item label="红娘姓名">{{ selectedHongniang.hongniangName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ selectedHongniang.phone }}</el-descriptions-item>
          <el-descriptions-item label="微信号">{{ selectedHongniang.wechat || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="所属机构">{{ selectedHongniang.companyName || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="等级">
            <el-tag v-if="selectedHongniang.level == 1" type="info">普通</el-tag>
            <el-tag v-else-if="selectedHongniang.level == 2" type="success">高级</el-tag>
            <el-tag v-else-if="selectedHongniang.level == 3" type="warning">金牌</el-tag>
            <el-tag v-else type="info">{{ selectedHongniang.level }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="管理用户数">{{ selectedHongniang.totalUsers || 0 }}</el-descriptions-item>
          <el-descriptions-item label="组织活动数">{{ selectedHongniang.totalActivities || 0 }}</el-descriptions-item>
          <el-descriptions-item label="认证状态">
            <el-tag v-if="selectedHongniang.certificationStatus == 0" type="info">未认证</el-tag>
            <el-tag v-else-if="selectedHongniang.certificationStatus == 1" type="success">已认证</el-tag>
            <el-tag v-else-if="selectedHongniang.certificationStatus == 2" type="danger">认证失败</el-tag>
            <el-tag v-else type="info">{{ selectedHongniang.certificationStatus }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag v-if="selectedHongniang.status == 1" type="success">启用</el-tag>
            <el-tag v-else type="danger">禁用</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ selectedHongniang.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ selectedHongniang.updateTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 添加或修改红娘用户关系对话框 -->
    <el-dialog :title="dialog.title" v-model="dialog.visible" width="500px" append-to-body>
      <el-form ref="hongniangUserRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="红娘ID" prop="hongniangId">
          <el-input v-model="form.hongniangId" placeholder="请输入红娘ID" />
        </el-form-item>
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID" />
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
import { useRouter } from 'vue-router';
import { ElForm } from 'element-plus';
import { listHongniangUser, getHongniangUser, delHongniangUser, addHongniangUser, updateHongniangUser, exportHongniangUser, batchDelHongniangUser, searchAssignableUsers } from '@/api/hongniang/hongniangUser';
import { getAllHongniangs, getHongniangById } from '@/api/hongniang/hongniangInfo';
import { getUserById } from '@/api/shejiao/user';

const { proxy } = getCurrentInstance() as any;
const router = useRouter();

const hongniangUserList = ref<any[]>([]);
const loading = ref(true);
type FormInstance = InstanceType<typeof ElForm>;
const queryRef = ref<FormInstance>();
const hongniangUserRef = ref<FormInstance>();
const assignFormRef = ref<FormInstance>();
const ids = ref<number[]>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const showSearch = ref(true);

const dialog = reactive({
  visible: false,
  title: '',
});

const assignDialog = reactive({
  visible: false,
  title: '分配用户给红娘',
});

const initFormData = {
  id: undefined,
  hongniangId: undefined,
  userId: undefined
};

const form = reactive(initFormData);

const assignForm = reactive({
  hongniangId: undefined,
  userId: undefined
});

const assignLoading = ref(false);
const assignUserLoading = ref(false);
const userDetailVisible = ref(false);
const hongniangDetailVisible = ref(false);
const selectedUser = ref<any>(null);
const selectedHongniang = ref<any>(null);
const hongniangList = ref<any[]>([]);
const userList = ref<any[]>([]);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  hongniangName: undefined,
  userKeyword: undefined,
  hongniangUserNo: undefined,
});

const rules = reactive({
  hongniangId: [
    { required: true, message: "红娘ID不能为空", trigger: "blur" }
  ],
  userId: [
    { required: true, message: "用户ID不能为空", trigger: "blur" }
  ]
});

const assignRules = reactive({
  hongniangId: [
    { required: true, message: "请选择红娘", trigger: "change" }
  ],
  userId: [
    { required: true, message: "请选择用户", trigger: "change" }
  ]
});

/** 查询红娘用户关系列表 */
const getList = () => {
  loading.value = true;
  listHongniangUser(queryParams).then((res: any) => {
    loading.value = false;
    hongniangUserList.value = res.page?.list || [];
    total.value = res.page?.totalCount || 0;
  }).catch((error) => {
    loading.value = false;
    console.error('获取红娘用户关系列表失败:', error);
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
  hongniangUserRef.value?.resetFields();
};

// 取消分配
const cancelAssign = () => {
  assignDialog.visible = false;
  resetAssign();
};

// 重置分配表单
const resetAssign = () => {
  assignForm.hongniangId = undefined;
  assignForm.userId = undefined;
  userList.value = [];
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
  dialog.title = "添加红娘用户关系";
};

/** 修改按钮操作 */
const handleUpdate = (row?: any) => {
  reset();
  const id = row.id || ids.value[0];
  getHongniangUser(id).then((res: any) => {
    Object.assign(form, res.hongniangUser || res.data || res);
    dialog.visible = true;
    dialog.title = "修改红娘用户关系";
  }).catch((error) => {
    console.error('获取红娘用户关系详情失败:', error);
  });
};

/** 提交按钮 */
const submitForm = () => {
  hongniangUserRef.value?.validate((valid: boolean) => {
    if (valid) {
      if (form.id !== undefined) {
        updateHongniangUser(form).then(res => {
          proxy.$modal.msgSuccess("修改成功");
          dialog.visible = false;
          getList();
        });
      } else {
        addHongniangUser(form).then(res => {
          proxy.$modal.msgSuccess("新增成功");
          dialog.visible = false;
          getList();
        });
      }
    }
  });
};

/** 删除按钮操作 */
const handleUnbind = (row?: any) => {
  const id = row?.id || ids.value;
  proxy.$modal.confirm('是否确认解绑红娘用户关系编号为"' + id + '"的数据项？').then(function() {
    return delHongniangUser(id);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("解绑成功");
  }).catch(() => {});
};

/** 批量解绑按钮操作 */
const handleBatchUnbind = () => {
  if (ids.value.length === 0) {
    proxy.$modal.msgError("请至少选择一条记录");
    return;
  }
  
  proxy.$modal.confirm('是否确认解绑选中的' + ids.value.length + '条数据项？').then(function() {
    return batchDelHongniangUser(ids.value);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("批量解绑成功");
  }).catch(() => {});
};

/** 导出按钮操作 */
const handleExport = () => {
  proxy.$modal.confirm('是否确认导出所有红娘用户关系数据项？').then(function() {
    return exportHongniangUser(queryParams);
  }).then((response: any) => {
    proxy.download(response);
  }).catch(() => {});
};

/** 分配用户 */
const handleAssignUser = async () => {
  resetAssign();
  await loadHongniangs();
  await searchAssignUsers('');
  assignDialog.visible = true;
};

// 加载红娘列表
const loadHongniangs = async () => {
  try {
    const hongniangRes = await getAllHongniangs();
    hongniangList.value = hongniangRes.page?.list || hongniangRes.data?.list || [];
  } catch (error) {
    console.error('加载红娘列表失败:', error);
    proxy.$modal.msgError("加载红娘列表失败");
  }
};

const searchAssignUsers = async (keyword: string) => {
  assignUserLoading.value = true;
  try {
    const response: any = await searchAssignableUsers({
      keyword: keyword?.trim() || undefined,
      hongniangId: assignForm.hongniangId,
      limit: 30
    });
    userList.value = response.list || response.data?.list || [];
  } catch (error) {
    console.error('搜索分配用户失败:', error);
    proxy.$modal.msgError("搜索用户失败");
  } finally {
    assignUserLoading.value = false;
  }
};

const handleAssignHongniangChange = () => {
  assignForm.userId = undefined;
  searchAssignUsers('');
};

const formatAssignUserLabel = (user: any) => {
  const username = user?.username || '未命名';
  const uid = user?.uid ?? '-';
  const mobile = user?.mobile || '无手机号';
  return `${username} | UID:${uid} | ${mobile}`;
};

/** 确认分配用户 */
const confirmAssignUser = () => {
  assignFormRef.value?.validate((valid: boolean) => {
    if (valid) {
      assignLoading.value = true;
      addHongniangUser(assignForm).then(res => {
        proxy.$modal.msgSuccess("分配成功");
        assignDialog.visible = false;
        getList();
      }).finally(() => {
        assignLoading.value = false;
      });
    }
  });
};

/** 查看用户详情 */
const viewUserInfo = async (userId: number) => {
  try {
    console.log('[viewUserInfo] 请求用户ID:', userId);
    // 从shejiao项目获取用户详情
    const response: any = await getUserById(userId);
    console.log('[viewUserInfo] API 响应:', response);
    
    // 赋值给 selectedUser，兼容多种返回格式
    selectedUser.value = response.user || response.data || response;
    console.log('[viewUserInfo] selectedUser.value:', selectedUser.value);
    
    // 显示弹窗
    userDetailVisible.value = true;
    console.log('[viewUserInfo] userDetailVisible:', userDetailVisible.value);
  } catch (error) {
    console.error('[viewUserInfo] 获取用户详情失败:', error);
    proxy.$modal.msgError("获取用户详情失败");
  }
};

/** 查看红娘详情 */
const viewHongniangInfo = async (hongniangId: number) => {
  try {
    console.log('[viewHongniangInfo] 请求红娘ID:', hongniangId);
    // 从shejiao项目获取红娘详情
    const response = await getHongniangById(hongniangId);
    
    // 赋值给 selectedHongniang
    selectedHongniang.value = response.hongniang || response.data || response;
    
    // 显示弹窗
    hongniangDetailVisible.value = true;
  } catch (error) {
    console.error('[viewHongniangInfo] 获取红娘详情失败:', error);
    proxy.$modal.msgError("获取红娘详情失败");
  }
};

const createMatchCase = (row: any) => {
  router.push({
    path: '/hongniang/match-case',
    query: {
      autoCreate: '1',
      hongniangId: String(row.hongniangId),
      prefillUserId: String(row.userId),
      prefillGender: row.userGender == null ? '' : String(row.userGender)
    }
  });
};

onMounted(() => {
  getList();
});
</script>

<style scoped>
.hongniang-user {
  padding: 20px;
}

.assign-tip {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary);
}

.user-subline {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
