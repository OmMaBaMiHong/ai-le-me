<template>
    <div class="mod-config">
        <el-form :inline="true" :model="dataForm" @keyup.enter="getDataList()">
            <el-form-item>
                <el-input
                    v-model="dataForm.key"
                    placeholder="用户ID搜索"
                    clearable
                ></el-input>
            </el-form-item>
            <el-form-item>
                <!-- 选择框 -->
                <el-select
                    v-model="dataForm.type"
                    clearable
                    placeholder="请选择类型"
                >
                    <el-option
                        v-for="item in options"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                    >
                    </el-option>
                </el-select>

                <el-button @click="getDataList()" type="primary"
                    >查询</el-button
                >
                <el-button @click="refresh()">重置</el-button>
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
                prop="uid"
                header-align="center"
                align="center"
                label="用户ID"
            >
            </el-table-column>
            <el-table-column
                prop="moneyNumber"
                header-align="center"
                align="center"
                label="申请提现金额"
            >
            </el-table-column>
            <el-table-column
                prop="url"
                header-align="center"
                align="center"
                width="100"
                label="收款码"
            >
                <template v-slot="scope">
                    <img
                        style="width: 40px; height: 40px"
                        :src="scope.row.url"
                        @click="goPic(scope.row.url)"
                    />
                </template>
            </el-table-column>
            <el-table-column
                prop="type"
                header-align="center"
                align="center"
                label="类型"
            >
                <template v-slot="scope">
                    <div>
                        <el-tag v-if="scope.row.type == 0">支付宝</el-tag>
                        <el-tag v-else type="success">微信</el-tag>
                    </div>
                </template>
            </el-table-column>
            <el-table-column
                prop="status"
                header-align="center"
                align="center"
                label="状态"
            >
                <template v-slot="scope">
                    <div>
                        <el-tag v-if="scope.row.status == 0" type="warning"
                            >待审核</el-tag
                        >
                        <el-tag v-else-if="scope.row.status == 1" type="success"
                            >已打款</el-tag
                        >
                        <el-tag v-else type="danger">已驳回</el-tag>
                    </div>
                </template>
            </el-table-column>
            <el-table-column
                prop="feedback"
                header-align="center"
                align="center"
                label="审核反馈"
                show-overflow-tooltip="true"
            >
            </el-table-column>
            <el-table-column
                prop="createTime"
                header-align="center"
                align="center"
                label="提交时间"
            >
            </el-table-column>
            <el-table-column
                prop="updateTime"
                header-align="center"
                align="center"
                label="修改时间"
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
                        size="small"
                        v-if="scope.row.status == 0"
                        @click="addOrUpdateHandle(scope.row.id)"
                        >审核</el-button
                    >
                    <el-button
                        type="danger"
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
        <el-dialog v-model="dialogVisible3" width="30%" title="内容">
            <div>{{ postContent }}</div>
        </el-dialog>
    </div>
</template>

<script setup>
import AddOrUpdate from "./add-or-update.vue";
import { listCashout, delCashout } from '@/api/shejiao/cashout';
import { ref, reactive, onMounted, nextTick, getCurrentInstance } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
const { proxy } = getCurrentInstance();

const dataForm = reactive({
    key: "",
    type: "",
});
const dataList = ref([]);
const pageIndex = ref(1);
const pageSize = ref(10);
const totalPage = ref(0);
const dataListLoading = ref(false);
const dataListSelections = ref([]);
const addOrUpdateVisible = ref(false);
const dialogVisible3 = ref(false);
const postContent = ref("");
const options = reactive([
    {
        value: 0,
        label: "待审核",
    },
    {
        value: 1,
        label: "已打款",
    },
    {
        value: 2,
        label: "已驳回",
    },
]);

// 获取数据列表
function getDataList() {
    dataListLoading.value = true;
    proxy
        .$http({
            url: proxy.$http.adornUrl("/admin/cashout/list"),
            method: "get",
            params: proxy.$http.adornParams({
                page: pageIndex.value,
                limit: pageSize.value,
                key: dataForm.key,
                type: dataForm.type,
            }),
        })
        .then(({ data }) => {
            if (data && data.code === 0) {
                dataList.value = data.page.list;
                totalPage.value = data.page.totalCount;
            } else {
                dataList.value = [];
                totalPage.value = 0;
            }
            dataListLoading.value = false;
        });
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
function deleteHandle(id) {
    var ids = id
        ? [id]
        : dataListSelections.value.map((item) => {
              return item.id;
          });
    ElMessageBox.confirm(
        `确定对[id=${ids.join(",")}]进行[${id ? "删除" : "批量删除"}]操作?`,
        "提示",
        {
            confirmButtonText: "确定",
            cancelButtonText: "取消",
            type: "warning",
            center: true,
        }
    )
        .then(() => {
            proxy
                .$http({
                    url: proxy.$http.adornUrl("/admin/cashout/delete"),
                    method: "post",
                    data: proxy.$http.adornData(ids, false),
                })
                .then(({ data }) => {
                    if (data && data.code === 0) {
                        ElMessage.success("删除成功");
                        getDataList();
                    } else {
                        ElMessage.error(data.msg);
                    }
                });
        })
        .catch(() => {});
}

function goPic(url) {
    window.open(url);
}

function refresh() {
    dataForm.key = "";
    dataForm.type = "";
    pageIndex.value = 1;
    pageSize.value = 10;
    getDataList();
}
function showDialog(row) {
    dialogVisible3.value = true;
    postContent.value = row.feedback;
}

onMounted(() => {
    getDataList();
});
</script>
