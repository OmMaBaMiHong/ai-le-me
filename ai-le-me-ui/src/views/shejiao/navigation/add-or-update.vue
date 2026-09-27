<template>
  <el-dialog
    v-model="dialogVisible"
    :title="form.id ? '修改导航' : '新增导航'"
    width="600px"
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-form-item label="标题" prop="title">
        <el-input
          v-model="form.title"
          placeholder="请输入标题"
          clearable
        />
      </el-form-item>

      <el-form-item label="图片" prop="img">
        <el-upload
          class="avatar-uploader"
          :action="uploadUrl"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleUploadSuccess"
          :before-upload="beforeUpload"
        >
          <img v-if="form.img" :src="form.img" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
        <div class="form-info">建议尺寸：200*200像素，jpg、png图片类型</div>
      </el-form-item>

      <el-form-item label="跳转路径" prop="url">
        <el-input
          v-model="form.url"
          placeholder="请输入跳转路径"
          clearable
        />
      </el-form-item>

      <el-form-item label="跳转类型" prop="type">
        <el-radio-group v-model="form.type">
          <el-radio :label="0">页面</el-radio>
          <el-radio :label="1">外链</el-radio>
        </el-radio-group>
        <div class="form-info">外链就是外部网站的链接，页面指项目内部页面的跳转</div>
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :label="0">正常</el-radio>
          <el-radio :label="1">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, getCurrentInstance } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { getNavigation, addNavigation, updateNavigation } from '@/api/shejiao/navigation';
import { NavigationForm } from '@/api/shejiao/navigation/types';
import { getToken } from '@/utils/auth';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const emit = defineEmits(['success']);

const dialogVisible = ref(false);
const submitLoading = ref(false);
const formRef = ref();

// 上传地址和请求头
const uploadUrl = ref(import.meta.env.VITE_APP_BASE_API + '/resource/oss/upload');
const uploadHeaders = ref({
  Authorization: 'Bearer ' + getToken()
});

const form = ref<NavigationForm>({
  id: undefined,
  title: '',
  img: '',
  url: '',
  type: 0,
  status: 0
});

const rules = {
  title: [
    { required: true, message: '标题不能为空', trigger: 'blur' }
  ],
  img: [
    { required: true, message: '图片不能为空', trigger: 'change' }
  ],
  url: [
    { required: true, message: '跳转路径不能为空', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '跳转类型不能为空', trigger: 'change' }
  ],
  status: [
    { required: true, message: '状态不能为空', trigger: 'change' }
  ]
};

/** 打开对话框 */
const open = async (id?: number) => {
  resetForm();
  dialogVisible.value = true;
  
  if (id) {
    try {
      const res: any = await getNavigation(id);
      form.value = {
        id: res.navigation.id,
        title: res.navigation.title,
        img: res.navigation.img,
        url: res.navigation.url,
        type: res.navigation.type,
        status: res.navigation.status
      };
    } catch (error) {
      console.error('获取导航信息失败:', error);
    }
  }
};

/** 重置表单 */
const resetForm = () => {
  form.value = {
    id: undefined,
    title: '',
    img: '',
    url: '',
    type: 0,
    status: 0
  };
  nextTick(() => {
    formRef.value?.clearValidate();
  });
};

/** 上传成功回调 */
const handleUploadSuccess = (response: any) => {
  if (response.code === 200) {
    form.value.img = response.data.url;
    formRef.value?.validateField('img');
  } else {
    proxy?.$modal.msgError(response.msg || '上传失败');
  }
};

/** 上传前校验 */
const beforeUpload = (file: any) => {
  const isImage = file.type.startsWith('image/');
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isImage) {
    proxy?.$modal.msgError('只能上传图片文件!');
    return false;
  }
  if (!isLt2M) {
    proxy?.$modal.msgError('图片大小不能超过 2MB!');
    return false;
  }
  return true;
};

/** 提交表单 */
const submitForm = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true;
      try {
        if (form.value.id) {
          await updateNavigation(form.value);
          proxy?.$modal.msgSuccess('修改成功');
        } else {
          await addNavigation(form.value);
          proxy?.$modal.msgSuccess('新增成功');
        }
        dialogVisible.value = false;
        emit('success');
      } catch (error) {
        console.error('提交失败:', error);
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

defineExpose({
  open
});
</script>

<style scoped lang="scss">
.form-info {
  margin-top: 4px;
  font-size: 12px;
  color: #999999;
  line-height: 1.2;
}

.avatar-uploader {
  :deep(.el-upload) {
    border: 1px dashed var(--el-border-color);
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: var(--el-transition-duration-fast);
    
    &:hover {
      border-color: var(--el-color-primary);
    }
  }
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
}
</style>
