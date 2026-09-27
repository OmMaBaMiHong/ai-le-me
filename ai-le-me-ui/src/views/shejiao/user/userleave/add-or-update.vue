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
                label-width="120px"
            >
                <el-form-item label="经验等级名称" prop="name">
                    <el-input
                        v-model="dataForm.name"
                        placeholder="名称"
                        style="width: 200px"
                    ></el-input>
                </el-form-item>
                <el-form-item label="最小要求值" prop="minNum">
                    <el-input
                        v-model="dataForm.minNum"
                        placeholder="需大于上一级的最大值"
                        style="width: 200px"
                        type="number"
                    ></el-input>
                </el-form-item>
                <el-form-item label="最大要求值" prop="maxNum">
                    <el-input
                        v-model="dataForm.maxNum"
                        placeholder="最大要求值"
                        style="width: 200px"
                        type="number"
                    ></el-input>
                </el-form-item>
                <el-form-item label="等级ID" prop="levelId">
                    <el-input
                        v-model="dataForm.levelId"
                        placeholder="请设置1到5之间的整数"
                        style="width: 200px"
                        type="number"
                    ></el-input>
                </el-form-item>
                <p style="color:red;margin-left:15px;font-weight:500;font-size:12px;">
                    提示：按照等级ID升序排序，等级ID的设置从1至5，等级ID越大级别越高。
                </p>

                <p style="color:#ccc;margin-left:15px;font-weight:500;font-size:12px;">
                    最小值请设置为大于上一个等级的最大值
                </p>
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
import { getUserLevel, addUserLevel, updateUserLevel } from '@/api/shejiao/userlevel';
import { ElMessage } from "element-plus";
import { ref, reactive, nextTick, getCurrentInstance } from "vue";
const { proxy } = getCurrentInstance();

const visible = ref(false);
const dataForm = ref({
    id: 0,
    name: "",
    minNum: "",
    maxNum: "",
    levelId: "",
});

const dataRule = reactive({
    name: [{ required: true, message: "名称不能为空", trigger: "blur" }],
    minNum: [
        { required: true, message: "最小要求值不能为空", trigger: "blur" },
    ],
    maxNum: [
        { required: true, message: "最大要求值不能为空", trigger: "blur" },
    ],
    levelId: [{ required: true, message: "等级id不能为空", trigger: "blur" }],
});

async function init(id) {
    dataForm.value.id = id || 0;
    visible.value = true;
    await nextTick();
    proxy.$refs.popForm.resetFields();
    if (dataForm.value.id) {
        const res = await getUserLevel(dataForm.value.id);
        if (res) {
            Object.assign(dataForm.value, res.userLevel || res.data || res);
        }
    }
}

// 表单提交
async function dataFormSubmit() {
    const valid = await proxy.$refs["popForm"].validate();
    if (!valid) return;
    
    try {
        if (dataForm.value.id) {
            await updateUserLevel(dataForm.value);
        } else {
            await addUserLevel(dataForm.value);
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
