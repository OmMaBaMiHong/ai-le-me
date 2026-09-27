<template>
    <div>
        <el-dialog
            :title="!dataForm.id ? '新增' : '修改'"
            :close-on-click-modal="false"
            v-model="visible"
            width="25%"
        >
            <el-form
                :model="dataForm"
                :rules="dataRule"
                ref="popForm"
                @keyup.enter="dataFormSubmit()"
                label-width="80px"
            >
                <el-form-item label="充值金额" prop="price">
                    <el-input
                        v-model="dataForm.price"
                        placeholder="充值金额"
                        type="number"
                        style="width: 200px"
                        clearable
                    ></el-input>
                </el-form-item>
                <el-form-item label="赠送金额" prop="givePrice">
                    <el-input
                        v-model="dataForm.givePrice"
                        placeholder="赠送金额"
                        type="number"
                        style="width: 200px"
                        clearable
                    ></el-input>
                </el-form-item>
                <el-form-item label="排序" prop="sort">
                    <el-input
                        v-model="dataForm.sort"
                        placeholder="排序"
                        style="width: 200px"
                        type="number"
                    ></el-input>
                </el-form-item>
                <p class="formInfo">注：排序数越大越靠前哦~</p>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="dataForm.status">
                        <el-radio :value="0">有效</el-radio>
                        <el-radio :value="1">无效</el-radio>
                    </el-radio-group>
                </el-form-item>
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
import { getRecharge, addRecharge, updateRecharge } from '@/api/shejiao/recharge';
import { ElMessage } from "element-plus";
import { ref, reactive, nextTick, getCurrentInstance } from "vue";
const { proxy } = getCurrentInstance();

const visible = ref(false);
const dataForm = ref({
    id: 0,
    price: "",
    givePrice: "",
    sort: "",
    status: 0,
});

const dataRule = reactive({
    price: [{ required: true, message: "充值金额不能为空", trigger: "blur" }],
    givePrice: [
        { required: true, message: "赠送金额不能为空", trigger: "blur" },
    ],
    sort: [{ required: true, message: "排序不能为空", trigger: "blur" }],
    status: [{ required: true, message: "状态不能为空", trigger: "blur" }],
});

async function init(id) {
    dataForm.value.id = id || 0;
    visible.value = true;
    await nextTick();
    proxy.$refs.popForm.resetFields();
    if (dataForm.value.id) {
        const res = await getRecharge(dataForm.value.id);
        if (res) {
            Object.assign(dataForm.value, res.recharge || res.data || res);
        }
    }
}

// 表单提交
async function dataFormSubmit() {
    const valid = await proxy.$refs["popForm"].validate();
    if (!valid) return;
    
    try {
        if (dataForm.value.id) {
            await updateRecharge(dataForm.value);
        } else {
            await addRecharge(dataForm.value);
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
.formInfo {
    color: #999;
    margin-left: 15px;
    font-weight: 500;
    font-size: 12px;
    margin-top: -10px;
    margin-bottom: 10px;
}
</style>
