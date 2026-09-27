<template>
    <div class="mod-config">
        <el-form :inline="true" :model="dataForm" @keyup.enter="getDataList()">
            <el-form-item>
                <el-button
                    v-hasPermi="['admin:signconfig:save']"
                    type="primary"
                    @click="addOrUpdateHandle()"
                    >新增</el-button
                >
                <el-button
                    v-hasPermi="['admin:signconfig:delete']"
                    type="danger"
                    @click="deleteHandle()"
                    :disabled="dataListSelections.length <= 0"
                    >批量删除</el-button
                >
            </el-form-item>
        </el-form>
        <el-table
            :data="dataList"
            border
            v-loading="dataListLoading"
            @selection-change="selectionChangeHandle"
            style="width: 100%"
        >
            <el-table-column
                type="selection"
                header-align="center"
                align="center"
                width="50"
            >
            </el-table-column>
            <el-table-column
                prop="id"
                header-align="center"
                align="center"
                label="ID"
            >
            </el-table-column>
            <el-table-column
                prop="day"
                header-align="center"
                align="center"
                label="天数"
            >
            </el-table-column>
            <el-table-column
                prop="signNum"
                header-align="center"
                align="center"
                label="积分数"
            >
            </el-table-column>
            <el-table-column
                prop="sort"
                header-align="center"
                align="center"
                label="排序"
            >
            </el-table-column>
            <el-table-column
                fixed="right"
                header-align="center"
                align="center"
                width="150"
                label="操作"
            >
                <template v-slot="scope">
                    <el-button
                        type="primary"
                        text
                        size="small"
                        @click="addOrUpdateHandle(scope.row.id)"
                        >修改</el-button
                    >
                    <el-button
                        type="danger"
                        text
                        size="small"
                        @click="deleteHandle(scope.row.id)"
                        >删除</el-button
                    >
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            @size-change="sizeChangeHandle"
            @current-change="currentChangeHandle"
            :current-page="pageIndex"
            :page-sizes="[10, 20, 50, 100]"
            :page-size="pageSize"
            :total="totalPage"
            layout="total, sizes, prev, pager, next, jumper"
        >
        </el-pagination>
        <!-- 弹窗, 新增 / 修改 -->
        <add-or-update
            v-if="addOrUpdateVisible"
            ref="addOrUpdate"
            @refreshDataList="getDataList"
        ></add-or-update>
    </div>
</template>

<script setup>
import AddOrUpdate from "./add-or-update.vue";
import { listSignConfig, delSignConfig } from '@/api/shejiao/signconfig';
import { ref, reactive, onMounted, nextTick, getCurrentInstance } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
const { proxy } = getCurrentInstance();

const dataForm = reactive({
    key: "",
});
const dataList = ref([]);
const pageIndex = ref(1);
const pageSize = ref(10);
const totalPage = ref(0);
const dataListLoading = ref(false);
const dataListSelections = ref([]);
const addOrUpdateVisible = ref(false);

// 获取数据列表
async function getDataList() {
    dataListLoading.value = true;
    try {
        const res = await listSignConfig({
            pageNum: pageIndex.value,
            pageSize: pageSize.value,
            key: dataForm.key,
        });
        dataList.value = res.page?.list || [];
        totalPage.value = res.page?.totalCount || 0;
    } catch (error) {
        console.error('获取列表失败:', error);
        dataList.value = [];
        totalPage.value = 0;
    } finally {
        dataListLoading.value = false;
    }
}
// 每页数
function sizeChangeHandle(pageSizeVal) {
    pageSize.value = pageSizeVal;
    pageIndex.value = 1;
    getDataList();
}
// 当前页
function currentChangeHandle(val) {
    pageIndex.value = val;
    getDataList();
}
// 多选
function selectionChangeHandle(selections) {
    dataListSelections.value = selections;
}
// 新增 / 修改
function addOrUpdateHandle(id) {
    addOrUpdateVisible.value = true;
    nextTick(() => {
        if (id) {
            // 编辑操作
            proxy.$refs.addOrUpdate.init(id);
        } else {
            // 新增操作
            proxy.$refs.addOrUpdate.init();
        }
    });
}
//删除
async function deleteHandle(id) {
    const ids = id
        ? [id]
        : dataListSelections.value.map((item) => item.id);
    
    try {
        await ElMessageBox.confirm(
            `确定对[id=${ids.join(",")}]进行[${id ? "删除" : "批量删除"}]操作?`,
            "提示",
            {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning",
            }
        );
        
        await delSignConfig(ids);
        ElMessage.success("删除成功");
        getDataList();
    } catch (error) {
        if (error !== 'cancel') {
            console.error('删除失败:', error);
        }
    }
}

//重置查询
function refresh() {
    dataForm.key = "";
    pageIndex.value = 1;
    pageSize.value = 10;
    getDataList();
}

onMounted(() => {
    getDataList();
});
</script>
