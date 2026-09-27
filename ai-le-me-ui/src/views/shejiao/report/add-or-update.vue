<template>
  <el-dialog
    :title="!form.id ? '新增举报处理' : '修改举报处理'"
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
      <el-form-item label="处理状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :label="0">待处理</el-radio>
          <el-radio :label="1">已处理</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="处理备注" prop="handleNote">
        <el-input
          v-model="form.handleNote"
          type="textarea"
          :rows="3"
          placeholder="请输入处理备注"
          clearable
        />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="ShejiaoReportAddOrUpdate">
import { ref, reactive, getCurrentInstance } from 'vue';
import { getReport, addReport, updateReport } from '@/api/shejiao/report';
import { ReportForm } from '@/api/shejiao/report/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const visible = ref(false);
const submitLoading = ref(false);
const formRef = ref();

const form = reactive<ReportForm>({
  id: undefined,
  status: 0,
  handleNote: ''
});

const rules = {
  status: [
    { required: true, message: '处理状态不能为空', trigger: 'blur' }
  ]
};

/** 打开对话框 */
const open = async (id?: number) => {
  visible.value = true;
  resetForm();
  
  if (id) {
    try {
      const res: any = await getReport(id);
      if (res.report) {
        Object.assign(form, {
          id: res.report.id,
          status: res.report.status ?? 0,
          handleNote: res.report.handleNote
        });
      }
    } catch (error) {
      proxy?.$modal.msgError('获取举报信息失败');
      visible.value = false;
    }
  }
};

/** 重置表单 */
const resetForm = () => {
  form.id = undefined;
  form.status = 0;
  form.handleNote = '';
  formRef.value?.clearValidate();
};

/** 提交表单 */
const handleSubmit = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true;
      try {
        const data: ReportForm = {
          id: form.id,
          status: form.status,
          handleNote: form.handleNote
        };
        
        if (form.id) {
          await updateReport(data);
          proxy?.$modal.msgSuccess('修改成功');
        } else {
          await addReport(data);
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
