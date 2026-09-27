<template>
  <el-dialog
    :title="!dataForm.id ? '新增相亲局' : '修改相亲局'"
    :close-on-click-modal="false"
    v-model="visible"
    width="700px">
    <el-form
      :model="dataForm"
      :rules="dataRule"
      ref="popForm"
      label-width="120px">
      <el-form-item label="活动标题" prop="title">
        <el-input v-model="dataForm.title" placeholder="请输入活动标题"></el-input>
      </el-form-item>
      <el-form-item label="封面图片" prop="coverImg">
        <el-upload
          class="avatar-uploader"
          :action="uploadUrl"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleCoverSuccess">
          <img v-if="dataForm.coverImg" :src="dataForm.coverImg" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker
          v-model="dataForm.startTime"
          type="datetime"
          placeholder="选择开始时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%;">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker
          v-model="dataForm.endTime"
          type="datetime"
          placeholder="选择结束时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%;">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="活动地址" prop="address">
        <el-input v-model="dataForm.address" placeholder="请输入活动地址"></el-input>
      </el-form-item>
      <el-form-item label="男生费用" prop="maleFee">
        <el-input-number v-model="dataForm.maleFee" :min="0" :max="9999" :precision="2" placeholder="男生费用"></el-input-number>
        <span style="margin-left: 10px;">元</span>
      </el-form-item>
      <el-form-item label="女生费用" prop="femaleFee">
        <el-input-number v-model="dataForm.femaleFee" :min="0" :max="9999" :precision="2" placeholder="女生费用"></el-input-number>
        <span style="margin-left: 10px;">元</span>
      </el-form-item>
      <el-form-item label="最大人数" prop="maxParticipants">
        <el-input-number v-model="dataForm.maxParticipants" :min="2" :max="999" placeholder="最大参与人数"></el-input-number>
      </el-form-item>
      <el-form-item label="红娘ID" prop="hongniangId">
        <el-input v-model.number="dataForm.hongniangId" placeholder="请输入红娘ID"></el-input>
      </el-form-item>
      <el-form-item label="活动状态" prop="status">
        <el-radio-group v-model="dataForm.status">
          <el-radio :value="0">草稿</el-radio>
          <el-radio :value="1">报名中</el-radio>
          <el-radio :value="2">报名结束</el-radio>
          <el-radio :value="3">进行中</el-radio>
          <el-radio :value="4">已结束</el-radio>
          <el-radio :value="5">已取消</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="活动描述" prop="description">
        <el-input type="textarea" v-model="dataForm.description" placeholder="请输入活动描述" :rows="4"></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { getXiangqinActivity, addXiangqinActivity, updateXiangqinActivity } from '@/api/shejiao/xiangqin';
import { ElMessage } from "element-plus";
import { Plus } from '@element-plus/icons-vue';
import { ref, reactive, nextTick, getCurrentInstance } from "vue";
import { getToken } from '@/utils/auth';
const { proxy } = getCurrentInstance();

const uploadUrl = ref('');
const uploadHeaders = ref({});

const visible = ref(false);
const dataForm = ref({
  id: 0,
  title: '',
  coverImg: '',
  startTime: '',
  endTime: '',
  address: '',
  maleFee: 0,
  femaleFee: 0,
  maxParticipants: 20,
  hongniangId: null,
  status: 0,
  description: ''
});

const dataRule = reactive({
  title: [
    { required: true, message: '活动标题不能为空', trigger: 'blur' }
  ],
  startTime: [
    { required: true, message: '开始时间不能为空', trigger: 'change' }
  ],
  endTime: [
    { required: true, message: '结束时间不能为空', trigger: 'change' }
  ],
  address: [
    { required: true, message: '活动地址不能为空', trigger: 'blur' }
  ],
  maxParticipants: [
    { required: true, message: '最大人数不能为空', trigger: 'blur' }
  ],
  hongniangId: [
    { required: true, message: '红娘ID不能为空', trigger: 'blur' }
  ]
});

async function init(id) {
  dataForm.value.id = id || 0;
  visible.value = true;
  
  // 配置上传接口
  uploadUrl.value = import.meta.env.VITE_APP_BASE_API + '/resource/oss/upload';
  uploadHeaders.value = {
    Authorization: 'Bearer ' + getToken(),
    clientid: import.meta.env.VITE_APP_CLIENT_ID
  };
  
  await nextTick();
  if (proxy.$refs.popForm) {
    proxy.$refs.popForm.resetFields();
  }
  if (dataForm.value.id) {
    const res = await getXiangqinActivity(dataForm.value.id);
    if (res && res.activity) {
      Object.assign(dataForm.value, res.activity);
    }
  }
}

// 表单提交
async function dataFormSubmit() {
  const valid = await proxy.$refs.popForm.validate();
  if (!valid) return;
  
  try {
    if (dataForm.value.id) {
      await updateXiangqinActivity(dataForm.value);
    } else {
      await addXiangqinActivity(dataForm.value);
    }
    ElMessage({
      message: '操作成功',
      type: 'success',
      duration: 1500,
    });
    visible.value = false;
    proxy.$emit('refreshDataList');
  } catch (error) {
    console.error('提交失败:', error);
  }
}

// 封面上传成功回调
function handleCoverSuccess(response) {
  dataForm.value.coverImg = response.data.url;
  proxy.$forceUpdate();
}

defineExpose({
  init,
});
</script>

<style scoped>
.avatar-uploader .avatar {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

.avatar-uploader .avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  line-height: 178px;
  text-align: center;
}

.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
}
</style>
