<template>
  <el-dialog
    title="处理评论"
    v-model="visible"
    width="600px"
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
      <el-form-item label="评论内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          placeholder="评论内容"
          disabled
        />
      </el-form-item>
      
      <el-form-item label="评论时间" prop="createTime">
        <el-input
          v-model="form.createTime"
          placeholder="评论时间"
          disabled
        />
      </el-form-item>
      
      <el-form-item label="评论状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">展示</el-radio>
          <el-radio :label="0">不展示</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="ShejiaoCommentAddOrUpdate">
import { ref, reactive, getCurrentInstance } from 'vue';
import { getComment, updateComment } from '@/api/shejiao/comment';
import { CommentForm } from '@/api/shejiao/comment/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const visible = ref(false);
const submitLoading = ref(false);
const formRef = ref();

const form = reactive<CommentForm>({
  id: undefined,
  pid: undefined,
  type: undefined,
  uid: undefined,
  toUid: undefined,
  postId: undefined,
  content: '',
  status: 1,
  createTime: undefined
});

const rules = {
  status: [
    { required: true, message: '评论状态不能为空', trigger: 'change' }
  ]
};

/** 打开对话框 */
const open = async (id: number) => {
  visible.value = true;
  resetForm();
  
  if (id) {
    try {
      const res: any = await getComment(id);
      if (res.comment) {
        Object.assign(form, {
          id: res.comment.id,
          pid: res.comment.pid,
          type: res.comment.type,
          uid: res.comment.uid,
          toUid: res.comment.toUid,
          postId: res.comment.postId,
          content: res.comment.content,
          status: res.comment.status ?? 1,
          createTime: res.comment.createTime
        });
      }
    } catch (error) {
      proxy?.$modal.msgError('获取评论信息失败');
      visible.value = false;
    }
  }
};

/** 重置表单 */
const resetForm = () => {
  form.id = undefined;
  form.pid = undefined;
  form.type = undefined;
  form.uid = undefined;
  form.toUid = undefined;
  form.postId = undefined;
  form.content = '';
  form.status = 1;
  form.createTime = undefined;
  formRef.value?.clearValidate();
};

/** 提交表单 */
const handleSubmit = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true;
      try {
        const data: CommentForm = {
          id: form.id,
          pid: form.pid,
          type: form.type,
          uid: form.uid,
          toUid: form.toUid,
          postId: form.postId,
          content: form.content,
          status: form.status,
          createTime: form.createTime
        };
        
        await updateComment(data);
        proxy?.$modal.msgSuccess('处理成功');
        
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
</style>
