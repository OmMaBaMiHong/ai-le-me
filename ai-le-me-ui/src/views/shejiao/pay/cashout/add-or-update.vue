<template>
  <div>
    <el-dialog
      :title="!dataForm.id ? '新增' : '修改'"
      :close-on-click-modal="false"
      v-model="visible"
      width="35%"
    >
      <el-form
        :model="dataForm"
        :rules="dataRule"
        ref="popForm"
        @keyup.enter="dataFormSubmit()"
        label-width="80px"
      >
        <el-form-item label="提现金额" prop="moneyNumber">
          <el-input
            v-model="dataForm.moneyNumber"
            placeholder="申请提现金额"
            style="width: 150px"
            disabled
          ></el-input>
        </el-form-item>
        <el-form-item label="审核状态" prop="status">
          <el-radio-group v-model="dataForm.status">
            <el-radio :label="0">待审核</el-radio>
            <el-radio :label="1">已线下打款</el-radio>
            <el-radio :label="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="审核反馈"
          prop="feedback"
          v-if="dataForm.status == 2"
        >
          <el-input
            v-model="dataForm.feedback"
            placeholder="请填写原因"
            style="width: 450px"
          ></el-input>
        </el-form-item>
        <div v-if="dataForm.status == 1" style="color: red; margin-left: 30px">
          请确保已完成线下汇款
        </div>
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
import { getCashout, addCashout, updateCashout } from '@/api/shejiao/cashout';
import { ElMessage } from "element-plus";
import { ref, reactive, nextTick, getCurrentInstance } from "vue";
const { proxy } = getCurrentInstance();
const visible = ref(false);
const dataForm = ref({
  id: 0,
  uid: "",
  moneyNumber: "",
  url: "",
  feedback: "",
  type: "",
  status: "",
  createTime: "",
  updateTime: "",
});
const dataRule = reactive({
  uid: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
  moneyNumber: [
    { required: true, message: "申请提现金额不能为空", trigger: "blur" },
  ],
  url: [{ required: true, message: "收款码不能为空", trigger: "blur" }],
  feedback: [{ required: true, message: "审核反馈不能为空", trigger: "blur" }],
  type: [
    {
      required: true,
      message: "打款类型0支付宝1微信不能为空",
      trigger: "blur",
    },
  ],
  status: [{ required: true, message: "状态不能为空", trigger: "blur" }],
  createTime: [
    { required: true, message: "创建时间不能为空", trigger: "blur" },
  ],
  updateTime: [
    { required: true, message: "修改时间不能为空", trigger: "blur" },
  ],
});
async function init(id) {
  dataForm.value.id = id || 0;
  visible.value = true;
  await nextTick();
  proxy.$refs.popForm.resetFields();
  if (dataForm.value.id) {
    const res = await getCashout(dataForm.value.id);
    if (res) {
      Object.assign(dataForm.value, res.data || res);
    }
  }
}

// 表单提交
async function dataFormSubmit() {
  const valid = await proxy.$refs["popForm"].validate();
  if (!valid) return;
  
  try {
    if (dataForm.value.id) {
      await updateCashout(dataForm.value);
    } else {
      await addCashout(dataForm.value);
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