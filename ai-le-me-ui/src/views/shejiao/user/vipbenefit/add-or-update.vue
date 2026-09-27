<template>
  <div>
    <el-dialog
      :title="!dataForm.id ? '新增' : '修改'"
      :close-on-click-modal="false"
      width="45%"
      v-model="visible"
    >
      <el-form
        :model="dataForm"
        :rules="dataRule"
        ref="popForm"
        @keyup.enter="dataFormSubmit()"
        label-width="80px"
      >
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="dataForm.title"
            placeholder="会员权益标题"
            style="width: 300px"
          ></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="describes">
          <el-input
            v-model="dataForm.describes"
            placeholder="会员权益描述"
            style="width: 300px"
          ></el-input>
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-upload
            class="avatar-uploader"
            :action="url"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleIconSuccess"
          >
            <img v-if="dataForm.icon" :src="dataForm.icon" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="dataForm.status">
            <el-radio :label="0">有效</el-radio>
            <el-radio :label="1">无效</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input
            v-model="dataForm.sort"
            placeholder="排序"
            style="width: 100px"
          ></el-input>
        </el-form-item>
        <p class="formInfo">
            数值越大排序越靠前
        </p>
      </el-form>
      <template v-slot:footer>
        <span class="dialog-footer">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>


<script setup>
import { getVipBenefit, addVipBenefit, updateVipBenefit } from '@/api/shejiao/vipbenefit';
import { ElMessage } from "element-plus";
import { ref, reactive, nextTick, getCurrentInstance } from "vue";
import { Plus } from '@element-plus/icons-vue';
import { getToken } from '@/utils/auth';
const { proxy } = getCurrentInstance();
const visible = ref(false);
const dataForm = ref({
  id: 0,
  title: "",
  describes: "",
  icon: "",
  status: 0,
  sort: 1,
});
const dataRule = reactive({
  title: [{ required: true, message: "会员权益标题不能为空", trigger: "blur" }],
  describes: [
    { required: true, message: "会员权益描述不能为空", trigger: "blur" },
  ],
  icon: [{ required: true, message: "图标不能为空", trigger: "blur" }],
  status: [
    { required: true, message: "状态不能为空", trigger: "blur" },
  ],
  sort: [{ required: true, message: "排序不能为空", trigger: "blur" }],
});
const url = ref("");
const uploadHeaders = ref({});


async function init(id) {
  dataForm.value.id = id || 0;
  url.value = import.meta.env.VITE_APP_BASE_API + '/resource/oss/upload';
  // 设置上传请求头，携带 token
  uploadHeaders.value = {
    Authorization: 'Bearer ' + getToken(),
    clientid: import.meta.env.VITE_APP_CLIENT_ID
  };
  visible.value = true;
  await nextTick();
  proxy.$refs.popForm.resetFields();
  if (dataForm.value.id) {
    const res = await getVipBenefit(dataForm.value.id);
    if (res) {
      Object.assign(dataForm.value, res.vipBenefit || res.data || res);
    }
  }
}

function handleIconSuccess(response) {
    // RuoYi 系统上传接口返回: { code: 200, data: { url, fileName, ossId } }
    dataForm.value.icon = response.data.url;
    proxy.$forceUpdate();
}

// 表单提交
async function dataFormSubmit() {
  const valid = await proxy.$refs["popForm"].validate();
  if (!valid) return;
  
  try {
    if (dataForm.value.id) {
      await updateVipBenefit(dataForm.value);
    } else {
      await addVipBenefit(dataForm.value);
    }
    ElMessage({
      message: "操作成功",
      type: "success",
      duration: 1500,
    });
    visible.value = false;
    proxy.$emit("refreshDataList");
  } catch (error) {
    console.error('提交失败:', error);
  }
}

defineExpose({
  init,
});
</script>

<style scoped>
.avatar-uploader {
  display: inline-block;
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

.avatar {
  width: 80px;
  height: 80px;
  display: block;
  object-fit: cover;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 80px;
  height: 80px;
  line-height: 80px;
  text-align: center;
  display: block;
}

.formInfo {
  color: #999;
  font-size: 12px;
  line-height: 1.5;
  margin-top: 5px;
}
</style>