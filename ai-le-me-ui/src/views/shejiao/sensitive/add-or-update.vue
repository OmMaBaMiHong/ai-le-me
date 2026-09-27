<template>
  <el-dialog
    :title="!form.id ? '新增敏感词' : '修改敏感词'"
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
      <el-form-item label="敏感词库" prop="sensitiveWord">
        <el-input
          v-model="form.sensitiveWord"
          type="textarea"
          :rows="5"
          placeholder="请输入敏感词库"
          clearable
        />
        <div class="el-upload__tip">敏感词请用英文逗号","隔开</div>
      </el-form-item>
      
      <el-form-item label="是否开启" prop="state">
        <el-radio-group v-model="form.state">
          <el-radio :label="1">是</el-radio>
          <el-radio :label="0">否</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="处理措施" prop="handleMeasures">
        <el-radio-group v-model="form.handleMeasures">
          <el-radio :label="1">禁止发布</el-radio>
          <el-radio :label="2">需要审核</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="ShejiaoSensitiveAddOrUpdate">
import { ref, reactive, getCurrentInstance } from 'vue';
import { getSensitive, addSensitive, updateSensitive } from '@/api/shejiao/sensitive';
import { SensitiveForm } from '@/api/shejiao/sensitive/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const visible = ref(false);
const submitLoading = ref(false);
const formRef = ref();

const form = reactive<SensitiveForm>({
  id: undefined,
  sensitiveWord: '',
  state: 1,
  handleMeasures: 1
});

const rules = {
  sensitiveWord: [
    { required: true, message: '敏感词库不能为空', trigger: 'blur' }
  ],
  state: [
    { required: true, message: '是否开启不能为空', trigger: 'blur' }
  ],
  handleMeasures: [
    { required: true, message: '处理措施不能为空', trigger: 'blur' }
  ]
};

/** 打开对话框 */
const open = async (id?: number) => {
  visible.value = true;
  resetForm();
  
  if (id) {
    try {
      const res: any = await getSensitive(id);
      if (res.sensitive) {
        Object.assign(form, {
          id: res.sensitive.id,
          sensitiveWord: res.sensitive.sensitiveWord,
          state: res.sensitive.state,
          handleMeasures: res.sensitive.handleMeasures
        });
      }
    } catch (error) {
      proxy?.$modal.msgError('获取敏感词信息失败');
      visible.value = false;
    }
  }
};

/** 重置表单 */
const resetForm = () => {
  form.id = undefined;
  form.sensitiveWord = '';
  form.state = 1;
  form.handleMeasures = 1;
  formRef.value?.clearValidate();
};

/** 提交表单 */
const handleSubmit = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true;
      try {
        const data: SensitiveForm = {
          id: form.id,
          sensitiveWord: form.sensitiveWord,
          state: form.state,
          handleMeasures: form.handleMeasures
        };
        
        if (form.id) {
          await updateSensitive(data);
          proxy?.$modal.msgSuccess('修改成功');
        } else {
          await addSensitive(data);
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
  color: #f14a4a;
  line-height: 1.5;
}
</style>
