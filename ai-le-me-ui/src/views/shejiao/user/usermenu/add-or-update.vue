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
                <el-form-item label="名称" prop="name">
                    <el-input
                        v-model="dataForm.name"
                        placeholder="名称"
                        style="width:250px"
                    ></el-input>
                </el-form-item>
                <el-form-item label="跳转路径" prop="url">
                    <el-input
                        v-model="dataForm.url"
                        placeholder="跳转路径"
                        style="width:250px"
                    ></el-input>
                </el-form-item>
                <el-form-item label="图片" prop="img">
                    <el-upload
                        class="avatar-uploader"
                        :action="url"
                        :headers="uploadHeaders"
                        :show-file-list="false"
                        :on-success="handleIconSuccess"
                    >
                        <img
                            v-if="dataForm.img"
                            :src="dataForm.img"
                            class="avatar"
                        />
                        <el-icon v-else class="avatar-uploader-icon"
                            ><Plus
                        /></el-icon>
                    </el-upload>
                </el-form-item>
                <p class="formInfo">建议尺寸：200*200像素，jpg、png图片类型</p>
                <el-form-item label="排序" prop="sort">
                    <el-input
                        v-model="dataForm.sort"
                        placeholder="排序"
                        style="width:250px"
                    ></el-input>
                    <p class="formInfo">按照降序排列，数字越大越靠前</p>
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="dataForm.status">
                        <el-radio :label="0">显示</el-radio>
                        <el-radio :label="1">不显示</el-radio>
                    </el-radio-group>
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
import { getUserMenu, addUserMenu, updateUserMenu } from '@/api/shejiao/usermenu';
import { ElMessage } from "element-plus";
import { ref, reactive, nextTick } from "vue";
import { Plus } from '@element-plus/icons-vue';
import { getToken } from '@/utils/auth';
const emit = defineEmits(["refreshDataList"]);
const visible = ref(false);
const popForm = ref();
const createDefaultForm = () => ({
    id: 0,
    url: "",
    img: "",
    name: "",
    sort: 0,
    status: 0,
});
const dataForm = ref(createDefaultForm());
const url = ref("");
const uploadHeaders = ref({});
const dataRule = reactive({
    url: [{ required: true, message: "跳转路径不能为空", trigger: "blur" }],
    img: [{ required: true, message: "图片地址不能为空", trigger: "blur" }],
    name: [{ required: true, message: "名称不能为空", trigger: "blur" }],
    sort: [{ required: true, message: "排序不能为空", trigger: "blur" }],
    status: [{ required: true, message: "状态不能为空", trigger: "blur" }],
});
async function init(id) {
    url.value = import.meta.env.VITE_APP_BASE_API + '/resource/oss/upload';
    // 设置上传请求头，携带 token
    uploadHeaders.value = {
        Authorization: 'Bearer ' + getToken(),
        clientid: import.meta.env.VITE_APP_CLIENT_ID
    };
    visible.value = true;
    await nextTick();
    popForm.value?.resetFields();
    dataForm.value = {
        ...createDefaultForm(),
        id: id || 0,
    };
    if (dataForm.value.id) {
        const res = await getUserMenu(dataForm.value.id);
        if (res) {
            Object.assign(dataForm.value, res.userMenu || res.data || res);
        }
    }
}

// 表单提交
async function dataFormSubmit() {
    try {
        await popForm.value?.validate();
    } catch (error) {
        return;
    }
    
    try {
        if (dataForm.value.id) {
            await updateUserMenu(dataForm.value);
        } else {
            await addUserMenu(dataForm.value);
        }
        ElMessage({
            message: "操作成功",
            type: "success",
            duration: 1500,
        });
        visible.value = false;
        emit("refreshDataList");
    } catch (error) {
        console.error('提交失败:', error);
    }
}
function handleIconSuccess(response) {
    // RuoYi 系统上传接口返回: { code: 200, data: { url, fileName, ossId } }
    dataForm.value.img = response?.data?.url || '';
}

defineExpose({
    init,
});
</script>

<style scoped>
.avatar-uploader {
    display: inline-block;
}

.avatar-uploader :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: border-color 0.3s;
}

.avatar-uploader :deep(.el-upload:hover) {
    border-color: #409eff;
}

.avatar {
    width: 80px;
    height: 80px;
    display: block;
    object-fit: cover;
}

.avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 80px;
    height: 80px;
    line-height: 80px;
    text-align: center;
    display: block;
}

.formInfo {
    color: #999;
    font-size: 12px;
    line-height: 1.5;
    margin-top: 5px;
}
</style>
