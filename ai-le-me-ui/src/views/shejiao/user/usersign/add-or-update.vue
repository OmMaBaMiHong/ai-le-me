<template>
    <div>
        <el-dialog
            :title="!dataForm.id ? '新增' : '修改'"
            :close-on-click-modal="false"
            v-model="visible"
        >
            <el-form
                :model="dataForm"
                :rules="dataRule"
                ref="popForm"
                @keyup.enter="dataFormSubmit()"
                label-width="80px"
            >
                <el-form-item label="用户id" prop="uid">
                    <el-input
                        v-model="dataForm.uid"
                        placeholder="用户id"
                    ></el-input>
                </el-form-item>
                <el-form-item label="签到说明" prop="title">
                    <el-input
                        v-model="dataForm.title"
                        placeholder="签到说明"
                    ></el-input>
                </el-form-item>
                <el-form-item label="获得积分" prop="number">
                    <el-input
                        v-model="dataForm.number"
                        placeholder="获得积分"
                    ></el-input>
                </el-form-item>
                <el-form-item label="剩余积分" prop="balance">
                    <el-input
                        v-model="dataForm.balance"
                        placeholder="剩余积分"
                    ></el-input>
                </el-form-item>
                <el-form-item label="创建时间" prop="createTime">
                    <el-input
                        v-model="dataForm.createTime"
                        placeholder="创建时间"
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
import { getUserSign, addUserSign, updateUserSign } from '@/api/shejiao/usersign';
import { ElMessage } from "element-plus";
import { ref, reactive, nextTick, getCurrentInstance } from "vue";
const { proxy } = getCurrentInstance();
const visible = ref(false);
const dataForm = ref({
    id: 0,
    uid: "",
    title: "",
    number: "",
    balance: "",
    createTime: "",
});
const dataRule = reactive({
    uid: [{ required: true, message: "用户id不能为空", trigger: "blur" }],
    title: [{ required: true, message: "签到说明不能为空", trigger: "blur" }],
    number: [{ required: true, message: "获得积分不能为空", trigger: "blur" }],
    balance: [{ required: true, message: "剩余积分不能为空", trigger: "blur" }],
    createTime: [
        { required: true, message: "创建时间不能为空", trigger: "blur" },
    ],
});
function init(id) {
    dataForm.value.id = id || 0;
    visible.value = true;
    nextTick(() => {
        proxy.$refs.popForm.resetFields();
        if (dataForm.value.id) {
            proxy
                .$http({
                    url: proxy.$http.adornUrl(
                        `/admin/usersign/info/${dataForm.value.id}`
                    ),
                    method: "get",
                    params: proxy.$http.adornParams(),
                })
                .then(({ data }) => {
                    if (data && data.code === 0) {
                        dataForm.value.uid = data.userSign.uid;
                        dataForm.value.title = data.userSign.title;
                        dataForm.value.number = data.userSign.number;
                        dataForm.value.balance = data.userSign.balance;
                        dataForm.value.createTime = data.userSign.createTime;
                    }
                });
        }
    });
}

// 表单提交
function dataFormSubmit() {
    proxy.$refs["popForm"].validate((valid) => {
        if (valid) {
            proxy
                .$http({
                    url: proxy.$http.adornUrl(
                        `/admin/usersign/${
                            !dataForm.value.id ? "save" : "update"
                        }`
                    ),
                    method: "post",
                    data: proxy.$http.adornData({
                        id: dataForm.value.id || undefined,
                        uid: dataForm.value.uid,
                        title: dataForm.value.title,
                        number: dataForm.value.number,
                        balance: dataForm.value.balance,
                        createTime: dataForm.value.createTime,
                    }),
                })
                .then(({ data }) => {
                    if (data && data.code === 0) {
                        ElMessage({
                            message: "操作成功",
                            type: "success",
                            duration: 1500,
                            onClose: () => {
                                visible.value = false;
                                proxy.$emit("refreshDataList");
                            },
                        });
                    } else {
                        ElMessage.error(data.msg);
                    }
                });
        }
    });
}

defineExpose({
    init,
});
</script>