<template>
    <div>
        <el-dialog
            :title="!dataForm.id ? '新增' : '修改'"
            :close-on-click-modal="false"
            v-model="visible"
            width="30%"
        >
            <el-form
                :model="dataForm"
                :rules="dataRule"
                ref="popForm"
                @keyup.enter="dataFormSubmit()"
                label-width="80px"
            >
                <el-form-item label="天数" prop="day" style="width: 300px">
                    <el-input
                        v-model="dataForm.day"
                        placeholder="天数"
                    ></el-input>
                </el-form-item>
                <el-form-item
                    label="积分数"
                    prop="signNum"
                    style="width: 300px"
                >
                    <el-input
                        v-model="dataForm.signNum"
                        placeholder="积分数"
                        type="number"
                    ></el-input>
                </el-form-item>
                <el-form-item label="排序" prop="sort" style="width: 300px">
                    <el-input
                        v-model="dataForm.sort"
                        placeholder="排序"
                        type="number"
                        clearable
                    ></el-input>
                </el-form-item>
                <p class="formInfo">注：排序数越小越靠前哦~</p>
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
import { getSignConfig, addSignConfig, updateSignConfig } from '@/api/shejiao/signconfig';
import { ElMessage } from "element-plus";
import { ref, reactive, nextTick, getCurrentInstance } from "vue";
const { proxy } = getCurrentInstance();
const visible = ref(false);
const dataForm = ref({
    id: 0,
    day: "",
    signNum: "",
    sort: "",
});
const dataRule = reactive({
    day: [{ required: true, message: "天数不能为空", trigger: "blur" }],
    signNum: [{ required: true, message: "积分数不能为空", trigger: "blur" }],
    sort: [{ required: true, message: "排序不能为空", trigger: "blur" }],
});
async function init(id) {
    dataForm.value.id = id || 0;
    visible.value = true;
    await nextTick();
    proxy.$refs.popForm.resetFields();
    if (dataForm.value.id) {
        const res = await getSignConfig(dataForm.value.id);
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
            await updateSignConfig(dataForm.value);
        } else {
            await addSignConfig(dataForm.value);
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