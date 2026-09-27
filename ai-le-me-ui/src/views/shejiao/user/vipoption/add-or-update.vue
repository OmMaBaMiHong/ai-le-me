<template>
    <div>
        <el-dialog
            :title="!dataForm.id ? '新增' : '修改'"
            :close-on-click-modal="false"
            width="30%"
            v-model="visible"
        >
            <el-form
                :model="dataForm"
                :rules="dataRule"
                ref="popForm"
                @keyup.enter="dataFormSubmit()"
                label-width="80px"
            >
                <el-form-item label="名称" prop="name" style="width: 300px">
                    <el-input
                        v-model="dataForm.name"
                        placeholder="名称"
                    ></el-input>
                </el-form-item>
                <el-form-item
                    label="有效天数"
                    prop="validDays"
                    style="width: 300px"
                >
                    <el-input
                        v-model="dataForm.validDays"
                        placeholder="有效天数"
                        type="number"
                    ></el-input>
                </el-form-item>
                <el-form-item label="价格" prop="price" style="width: 300px">
                    <el-input
                        v-model="dataForm.price"
                        placeholder="价格"
                        type="number"
                    ></el-input>
                </el-form-item>
                <el-form-item label="描述" prop="remark" style="width: 300px">
                    <el-input
                        v-model="dataForm.remark"
                        placeholder="描述"
                    ></el-input>
                </el-form-item>
                <el-form-item label="排序" prop="sort" style="width: 300px">
                    <el-input
                        v-model="dataForm.sort"
                        placeholder="排序"
                        type="number"
                    ></el-input>
                </el-form-item>
            </el-form>
            <template v-slot:footer>
                <span class="dialog-footer">
                    <el-button @click="visible = false">取消</el-button>
                    <el-button type="primary" @click="dataFormSubmit()"
                        >确定</el-button
                    >
                </span>
            </template>
        </el-dialog>
    </div>
</template>


<script setup>
import { getVipOption, addVipOption, updateVipOption } from '@/api/shejiao/vipoption';
import { ElMessage } from "element-plus";
import { ref, reactive, nextTick, getCurrentInstance } from "vue";
const { proxy } = getCurrentInstance();
const visible = ref(false);
const dataForm = ref({
    id: 0,
    name: "",
    validDays: "",
    price: "",
    remark: "",
    sort: "",
});
const dataRule = reactive({
    name: [{ required: true, message: "名称不能为空", trigger: "blur" }],
    validDays: [
        { required: true, message: "有效天数不能为空", trigger: "blur" },
    ],
    price: [{ required: true, message: "价格不能为空", trigger: "blur" }],
    remark: [{ required: true, message: "描述不能为空", trigger: "blur" }],
    sort: [{ required: true, message: "排序不能为空", trigger: "blur" }],
});
async function init(id) {
    dataForm.value.id = id || 0;
    visible.value = true;
    await nextTick();
    proxy.$refs.popForm.resetFields();
    if (dataForm.value.id) {
        const res = await getVipOption(dataForm.value.id);
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
            await updateVipOption(dataForm.value);
        } else {
            await addVipOption(dataForm.value);
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