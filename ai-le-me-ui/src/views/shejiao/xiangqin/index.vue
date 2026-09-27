<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm" @keyup.enter="getDataList()">
      <el-form-item>
        <el-input v-model="dataForm.title" placeholder="活动标题" clearable></el-input>
      </el-form-item>
      <el-form-item>
        <el-select v-model="dataForm.status" placeholder="活动状态" clearable>
          <el-option label="全部" :value="null"></el-option>
          <el-option label="草稿" :value="0"></el-option>
          <el-option label="报名中" :value="1"></el-option>
          <el-option label="报名结束" :value="2"></el-option>
          <el-option label="进行中" :value="3"></el-option>
          <el-option label="已结束" :value="4"></el-option>
          <el-option label="已取消" :value="5"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="getDataList()">查询</el-button>
        <el-button type="primary" @click="addOrUpdateHandle()">新增活动</el-button>
        <el-button type="danger" @click="deleteHandle()" :disabled="dataListSelections.length <= 0">批量删除</el-button>
      </el-form-item>
    </el-form>
    <el-table
      :data="dataList"
      border
      v-loading="dataListLoading"
      @selection-change="selectionChangeHandle"
      style="width: 100%;">
      <el-table-column type="selection" header-align="center" align="center" width="50"></el-table-column>
      <el-table-column prop="id" header-align="center" align="center" width="80" label="ID"></el-table-column>
      <el-table-column prop="coverImg" header-align="center" align="center" width="100" label="封面">
        <template #default="scope">
          <el-image
            v-if="scope.row.coverImg"
            :src="scope.row.coverImg"
            :preview-src-list="[scope.row.coverImg]"
            fit="cover"
            style="width: 60px; height: 60px; border-radius: 4px;"
          />
          <span v-else style="color: #999;">无封面</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" header-align="center" align="center" label="活动标题" show-overflow-tooltip></el-table-column>
      <el-table-column prop="startTime" header-align="center" align="center" width="160" label="开始时间"></el-table-column>
      <el-table-column prop="address" header-align="center" align="center" label="活动地址" show-overflow-tooltip></el-table-column>
      <el-table-column prop="maleFee" header-align="center" align="center" width="100" label="男生费用">
        <template #default="scope">
          <span>¥{{ scope.row.maleFee || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="femaleFee" header-align="center" align="center" width="100" label="女生费用">
        <template #default="scope">
          <span>¥{{ scope.row.femaleFee || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column header-align="center" align="center" width="120" label="报名人数">
        <template #default="scope">
          <span>{{ (scope.row.maleCount || 0) + (scope.row.femaleCount || 0) }}/{{ scope.row.maxParticipants }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" header-align="center" align="center" width="100" label="状态">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 0" type="info">草稿</el-tag>
          <el-tag v-else-if="scope.row.status === 1" type="success">报名中</el-tag>
          <el-tag v-else-if="scope.row.status === 2" type="warning">报名结束</el-tag>
          <el-tag v-else-if="scope.row.status === 3" type="primary">进行中</el-tag>
          <el-tag v-else-if="scope.row.status === 4">已结束</el-tag>
          <el-tag v-else type="danger">已取消</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" header-align="center" align="center" width="200" label="操作">
        <template #default="scope">
          <el-button type="primary" text size="small" @click="viewEnrollmentsHandle(scope.row.id)">报名列表</el-button>
          <el-button type="primary" text size="small" @click="addOrUpdateHandle(scope.row.id)">修改</el-button>
          <el-button type="danger" text size="small" @click="deleteHandle(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
      :current-page="pageIndex"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="pageSize"
      :total="totalPage"
      layout="total, sizes, prev, pager, next, jumper">
    </el-pagination>
    
    <!-- 弹窗, 新增 / 修改 -->
    <add-or-update
      v-if="addOrUpdateVisible"
      ref="addOrUpdate"
      @refreshDataList="getDataList">
    </add-or-update>
    
    <!-- 报名列表对话框 -->
    <el-dialog
      title="报名列表"
      v-model="enrollmentVisible"
      width="900px">
      <el-table
        :data="enrollmentList"
        v-loading="enrollmentLoading"
        border
        style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column label="用户" width="200" align="center">
          <template #default="scope">
            <div style="display: flex; align-items: center;">
              <el-avatar :src="scope.row.avatar" :size="40" style="margin-right: 10px;"></el-avatar>
              <span>{{ scope.row.realName || scope.row.username || '用户' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="80" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.gender === 1" type="primary">男</el-tag>
            <el-tag v-else-if="scope.row.gender === 2" type="danger">女</el-tag>
            <el-tag v-else type="info">未知</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80" align="center"></el-table-column>
        <el-table-column prop="phone" label="联系电话" width="120" align="center"></el-table-column>
        <el-table-column prop="createTime" label="报名时间" width="160" align="center"></el-table-column>
        <el-table-column label="审核状态" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="warning">待审核</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="success">已通过</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="danger">已拒绝</el-tag>
            <el-tag v-else type="info">已取消</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 0"
              type="success"
              text
              size="small"
              @click="auditEnrollment(scope.row.id, 1)">
              通过
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              type="danger"
              text
              size="small"
              @click="auditEnrollment(scope.row.id, 2)">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import AddOrUpdate from "./add-or-update.vue";
import { listXiangqinActivity, delXiangqinActivity, listXiangqinEnrollment, auditXiangqinEnrollment } from '@/api/shejiao/xiangqin';
import { ref, reactive, onMounted, nextTick, getCurrentInstance } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
const { proxy } = getCurrentInstance();

const dataForm = reactive({
  title: "",
  status: null
});
const dataList = ref([]);
const pageIndex = ref(1);
const pageSize = ref(10);
const totalPage = ref(0);
const dataListLoading = ref(false);
const dataListSelections = ref([]);
const addOrUpdateVisible = ref(false);
const enrollmentVisible = ref(false);
const enrollmentLoading = ref(false);
const enrollmentList = ref([]);
const currentActivityId = ref(null);

// 获取数据列表
async function getDataList() {
  dataListLoading.value = true;
  try {
    const res = await listXiangqinActivity({
      pageNum: pageIndex.value,
      pageSize: pageSize.value,
      title: dataForm.title,
      status: dataForm.status !== null ? dataForm.status : undefined
    });
    dataList.value = res.page?.list || [];
    totalPage.value = res.page?.totalCount || 0;
  } catch (error) {
    console.error('获取列表失败:', error);
    dataList.value = [];
    totalPage.value = 0;
  } finally {
    dataListLoading.value = false;
  }
}

// 每页数
function sizeChangeHandle(pageSizeVal) {
  pageSize.value = pageSizeVal;
  pageIndex.value = 1;
  getDataList();
}

// 当前页
function currentChangeHandle(val) {
  pageIndex.value = val;
  getDataList();
}

// 多选
function selectionChangeHandle(selections) {
  dataListSelections.value = selections;
}

// 新增 / 修改
function addOrUpdateHandle(id) {
  addOrUpdateVisible.value = true;
  nextTick(() => {
    if (proxy.$refs.addOrUpdate) {
      proxy.$refs.addOrUpdate.init(id);
    }
  });
}

// 查看报名列表
function viewEnrollmentsHandle(activityId) {
  currentActivityId.value = activityId;
  enrollmentVisible.value = true;
  getEnrollmentList();
}

// 获取报名列表
async function getEnrollmentList() {
  enrollmentLoading.value = true;
  try {
    const res = await listXiangqinEnrollment(currentActivityId.value);
    enrollmentList.value = res.page?.list || [];
  } catch (error) {
    console.error('获取报名列表失败:', error);
    enrollmentList.value = [];
  } finally {
    enrollmentLoading.value = false;
  }
}

// 审核报名
async function auditEnrollment(enrollmentId, auditStatus) {
  try {
    await ElMessageBox.confirm(
      `确定${auditStatus == 1 ? '通过' : '拒绝'}该报名吗?`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await auditXiangqinEnrollment(enrollmentId, auditStatus);
    ElMessage.success('操作成功');
    getEnrollmentList();
    getDataList();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审核失败:', error);
    }
  }
}

// 删除
async function deleteHandle(id) {
  const ids = id
    ? [id]
    : dataListSelections.value.map((item) => item.id);
  
  try {
    await ElMessageBox.confirm(
      `确定对[id=${ids.join(",")}]进行[${id ? "删除" : "批量删除"}]操作?`,
      "提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
    
    await delXiangqinActivity(ids);
    ElMessage.success("删除成功");
    getDataList();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
    }
  }
}

onMounted(() => {
  getDataList();
});
</script>
