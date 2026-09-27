<template>
  <el-dialog
    :title="!form.id ? '新增帖子' : '修改帖子'"
    v-model="visible"
    width="700px"
    :close-on-click-modal="false"
    draggable
    @close="handleClose"
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
      
      <el-form-item label="内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="5"
          placeholder="请输入内容"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="类型" prop="type">
        <el-radio-group v-model="form.type">
          <el-radio :label="0">普通</el-radio>
          <el-radio :label="1">推荐</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :label="0">正常</el-radio>
          <el-radio :label="1">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="是否私密" prop="isPrivate">
        <el-radio-group v-model="form.isPrivate">
          <el-radio :label="0">公开</el-radio>
          <el-radio :label="1">私密</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="是否置顶" prop="postTop">
        <el-radio-group v-model="form.postTop">
          <el-radio :label="0">否</el-radio>
          <el-radio :label="1">是</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="ShejiaoPostAddOrUpdate">
import { ref, reactive, getCurrentInstance } from 'vue';
import { getPost, addPost, updatePost } from '@/api/shejiao/post';
import { PostForm } from '@/api/shejiao/post/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const visible = ref(false);
const submitLoading = ref(false);
const formRef = ref();

const form = reactive<PostForm>({
  id: undefined,
  title: '',
  content: '',
  type: 0,
  status: 0,
  isPrivate: 0,
  postTop: 0,
  cut: 0
});

const rules = {
  title: [
    { required: true, message: '标题不能为空', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '内容不能为空', trigger: 'blur' }
  ]
};

/** 打开对话框 */
const open = async (id?: number) => {
  visible.value = true;
  resetForm();
  
  if (id) {
    try {
      const res: any = await getPost(id);
      if (res.post) {
        Object.assign(form, {
          id: res.post.id,
          title: res.post.title,
          content: res.post.content,
          type: res.post.type ?? 0,
          status: res.post.status ?? 0,
          isPrivate: res.post.isPrivate ?? 0,
          postTop: res.post.postTop ?? 0,
          cut: res.post.cut ?? 0
        });
      }
    } catch (error) {
      proxy?.$modal.msgError('获取帖子信息失败');
      visible.value = false;
    }
  }
};

/** 重置表单 */
const resetForm = () => {
  form.id = undefined;
  form.title = '';
  form.content = '';
  form.type = 0;
  form.status = 0;
  form.isPrivate = 0;
  form.postTop = 0;
  form.cut = 0;
  formRef.value?.clearValidate();
};

/** 提交表单 */
const handleSubmit = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true;
      try {
        const data: PostForm = {
          id: form.id,
          title: form.title,
          content: form.content,
          type: form.type,
          status: form.status,
          isPrivate: form.isPrivate,
          postTop: form.postTop,
          cut: form.cut
        };
        
        if (form.id) {
          await updatePost(data);
          proxy?.$modal.msgSuccess('修改成功');
        } else {
          await addPost(data);
          proxy?.$modal.msgSuccess('新增成功');
        }
        
        visible.value = false;
        emit('success');
      } catch (error) {
        console.error('提交失败:', error);
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

/** 关闭对话框 */
const handleClose = () => {
  visible.value = false;
  resetForm();
};

const emit = defineEmits(['success']);

defineExpose({
  open
});
</script>

<style scoped lang="scss">
.el-upload__tip {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}
</style>
