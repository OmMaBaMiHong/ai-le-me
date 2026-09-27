<template>
  <el-dialog
    :title="!form.id ? '新增话题' : '修改话题'"
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
      <el-form-item label="话题标题" prop="title">
        <el-input
          v-model="form.title"
          placeholder="请输入话题标题"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="话题描述" prop="introduce">
        <el-input
          v-model="form.introduce"
          type="textarea"
          :rows="3"
          placeholder="请输入话题描述"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="浏览量" prop="readCount">
        <el-input-number
          v-model="form.readCount"
          :min="0"
          :max="999999999"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="ShejiaoDiscussAddOrUpdate">
import { ref, reactive, getCurrentInstance } from 'vue';
import { getDiscuss, addDiscuss, updateDiscuss } from '@/api/shejiao/discuss';
import { DiscussForm } from '@/api/shejiao/discuss/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const visible = ref(false);
const submitLoading = ref(false);
const formRef = ref();

const form = reactive<DiscussForm>({
  id: undefined,
  uid: undefined,
  topicId: undefined,
  title: '',
  introduce: '',
  readCount: 0,
  topType: 0,
  createTime: undefined
});

const rules = {
  title: [
    { required: true, message: '话题标题不能为空', trigger: 'blur' },
    { min: 2, max: 50, message: '话题标题长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  introduce: [
    { required: true, message: '话题描述不能为空', trigger: 'blur' },
    { min: 5, max: 200, message: '话题描述长度在 5 到 200 个字符', trigger: 'blur' }
  ],
  readCount: [
    { required: true, message: '浏览量不能为空', trigger: 'blur' }
  ]
};

/** 打开对话框 */
const open = async (id?: number) => {
  visible.value = true;
  resetForm();
  
  if (id) {
    try {
      const res: any = await getDiscuss(id);
      if (res.discuss) {
        Object.assign(form, {
          id: res.discuss.id,
          uid: res.discuss.uid,
          topicId: res.discuss.topicId,
          title: res.discuss.title,
          introduce: res.discuss.introduce,
          readCount: res.discuss.readCount ?? 0,
          topType: res.discuss.topType ?? 0,
          createTime: res.discuss.createTime
        });
      }
    } catch (error) {
      proxy?.$modal.msgError('获取话题信息失败');
      visible.value = false;
    }
  }
};

/** 重置表单 */
const resetForm = () => {
  form.id = undefined;
  form.uid = undefined;
  form.topicId = undefined;
  form.title = '';
  form.introduce = '';
  form.readCount = 0;
  form.topType = 0;
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
        const data: DiscussForm = {
          id: form.id,
          uid: form.uid,
          topicId: form.topicId,
          title: form.title,
          introduce: form.introduce,
          readCount: form.readCount,
          topType: form.topType,
          createTime: form.createTime
        };
        
        if (form.id) {
          await updateDiscuss(data);
          proxy?.$modal.msgSuccess('修改成功');
        } else {
          await addDiscuss(data);
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
</style>
