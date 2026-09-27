<template>
  <div class="oss-page">
    <div class="oss-hero">
      <div class="hero-main">
        <div class="hero-badge">OSS</div>
        <div class="hero-title">云文件管理</div>
        <div class="hero-desc">上传入口、文件资产与存储配置统一收口到这一页，上传类动作可以直接跳转唤起。</div>
        <div class="hero-stats">
          <div class="stat-card">
            <div class="stat-label">文件总数</div>
            <div class="stat-value">{{ total }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">图片资源</div>
            <div class="stat-value">{{ imageCount }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">当前服务</div>
            <div class="stat-value">{{ serviceCount }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">图片预览</div>
            <div class="stat-value">{{ previewListResource ? '已开' : '已关' }}</div>
          </div>
        </div>
      </div>

      <div class="hero-actions">
        <el-button v-hasPermi="['system:oss:upload']" type="primary" @click="handleFile">上传文件</el-button>
        <el-button v-hasPermi="['system:oss:upload']" type="success" plain @click="handleImage">上传图片</el-button>
        <el-button
          v-hasPermi="['system:oss:edit']"
          :type="previewListResource ? 'danger' : 'warning'"
          plain
          @click="handlePreviewListResource(!previewListResource)"
        >
          {{ previewListResource ? '关闭预览' : '开启预览' }}
        </el-button>
        <el-button v-hasPermi="['system:thirdparty:list']" plain @click="handleOssConfig">存储配置</el-button>
      </div>
    </div>

    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="search-shell">
        <el-card shadow="never" class="search-card">
          <div class="section-head">
            <div>
              <div class="section-title">筛选条件</div>
              <div class="section-desc">按文件名、类型、上传时间和服务商快速缩小结果集。</div>
            </div>
            <el-button text @click="showSearch = false">收起</el-button>
          </div>

          <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="search-form">
            <el-form-item label="文件名" prop="fileName">
              <el-input v-model="queryParams.fileName" placeholder="文件名" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="原名" prop="originalName">
              <el-input v-model="queryParams.originalName" placeholder="原始文件名" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="文件后缀" prop="fileSuffix">
              <el-input v-model="queryParams.fileSuffix" placeholder=".png / .mp4" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="创建时间" class="date-range-item">
              <el-date-picker
                v-model="dateRangeCreateTime"
                value-format="YYYY-MM-DD HH:mm:ss"
                type="daterange"
                range-separator="-"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
              ></el-date-picker>
            </el-form-item>
            <el-form-item label="服务商" prop="service">
              <el-input v-model="queryParams.service" placeholder="如 qiniu / minio" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <div v-if="serviceOptions.length" class="service-filter-row">
            <button
              type="button"
              :class="['service-chip', { active: !queryParams.service }]"
              @click="handleServiceFilter('')"
            >
              全部服务
            </button>
            <button
              v-for="service in serviceOptions"
              :key="service"
              type="button"
              :class="['service-chip', { active: queryParams.service === service }]"
              @click="handleServiceFilter(service)"
            >
              {{ service }}
            </button>
          </div>
        </el-card>
      </div>
    </transition>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="section-head table-head">
          <div>
            <div class="section-title">文件列表</div>
            <div class="section-desc">当前页 {{ ossList.length }} 条，已选择 {{ ids.length }} 条。</div>
          </div>

          <div class="table-actions">
            <el-button v-if="!showSearch" plain @click="showSearch = true">展开筛选</el-button>
            <el-button plain @click="getList">刷新</el-button>
            <el-button v-hasPermi="['system:oss:remove']" type="danger" plain :disabled="multiple" @click="handleDelete()">批量删除</el-button>
            <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
          </div>
        </div>
      </template>

      <el-table
        v-if="showTable"
        v-loading="loading"
        :data="ossList"
        border
        class="oss-table"
        :header-cell-class-name="handleHeaderClass"
        @selection-change="handleSelectionChange"
        @header-click="handleHeaderCLick"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column v-if="false" label="对象存储主键" align="center" prop="ossId" />
        <el-table-column label="资源" min-width="330">
          <template #default="scope">
            <div class="file-cell">
              <div class="file-preview">
                <ImagePreview
                  v-if="previewListResource && checkFileSuffix(scope.row.fileSuffix)"
                  :width="76"
                  :height="76"
                  :src="scope.row.url"
                  :preview-src-list="[scope.row.url]"
                />
                <div v-else class="file-fallback">
                  {{ formatSuffix(scope.row.fileSuffix) }}
                </div>
              </div>
              <div class="file-meta">
                <div class="file-name">{{ scope.row.fileName }}</div>
                <div class="file-original">{{ scope.row.originalName }}</div>
                <div class="file-tags">
                  <el-tag size="small" round>{{ formatSuffix(scope.row.fileSuffix) }}</el-tag>
                  <el-tag size="small" round type="success">{{ scope.row.service || 'default' }}</el-tag>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="访问地址" min-width="300">
          <template #default="scope">
            <div class="url-cell">{{ shortUrl(scope.row.url) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="上传信息" min-width="180">
          <template #default="scope">
            <div class="uploader-cell">
              <div class="uploader-name">{{ scope.row.createByName || '系统' }}</div>
              <div class="uploader-time">{{ proxy.parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="下载" placement="top">
              <el-button v-hasPermi="['system:oss:download']" link type="primary" icon="Download" @click="handleDownload(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button v-hasPermi="['system:oss:remove']" link type="primary" icon="Delete" @click="handleDelete(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="560px" append-to-body class="oss-upload-dialog">
      <div class="upload-dialog-head">
        <div class="section-title">{{ dialog.title }}</div>
        <div class="section-desc">支持直接走当前启用的 OSS 服务上传，完成后自动回到列表。</div>
      </div>

      <el-form ref="ossFormRef" :model="form" :rules="rules" label-width="80px" class="upload-form">
        <el-form-item label="上传内容">
          <fileUpload v-if="type === 0" v-model="form.file" />
          <imageUpload v-if="type === 1" v-model="form.file" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button :loading="buttonLoading" type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Oss" lang="ts">
import { listOss, delOss } from '@/api/system/oss';
import { getConfigKey, updateConfigByKey } from '@/api/system/config';
import ImagePreview from '@/components/ImagePreview/index.vue';
import { OssForm, OssQuery, OssVO } from '@/api/system/oss/types';

const router = useRouter();
const route = useRoute();
const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const ossList = ref<OssVO[]>([]);
const showTable = ref(true);
const buttonLoading = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const type = ref(0);
const previewListResource = ref(true);
const dateRangeCreateTime = ref<[DateModelType, DateModelType]>(['', '']);

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

// 默认排序
const defaultSort = ref({ prop: 'createTime', order: 'ascending' });

const ossFormRef = ref<ElFormInstance>();
const queryFormRef = ref<ElFormInstance>();

const initFormData = {
  file: undefined
};
const data = reactive<PageData<OssForm, OssQuery>>({
  form: { ...initFormData },
  // 查询参数
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    fileName: '',
    originalName: '',
    fileSuffix: '',
    createTime: '',
    service: '',
    orderByColumn: defaultSort.value.prop,
    isAsc: defaultSort.value.order
  },
  rules: {
    file: [{ required: true, message: '文件不能为空', trigger: 'blur' }]
  }
});

const { queryParams, form, rules } = toRefs(data);
const serviceOptions = computed(() => {
  const values = new Set<string>();
  ossList.value.forEach((item) => {
    if (item.service) {
      values.add(item.service);
    }
  });
  if (queryParams.value.service) {
    values.add(queryParams.value.service);
  }
  return [...values];
});
const imageCount = computed(() => ossList.value.filter((item) => checkFileSuffix(item.fileSuffix)).length);
const serviceCount = computed(() => serviceOptions.value.length);

/** 查询OSS对象存储列表 */
const getList = async () => {
  loading.value = true;
  const res = await getConfigKey('sys.oss.previewListResource');
  previewListResource.value = res?.data === undefined ? true : res.data === 'true';
  const response = await listOss(proxy?.addDateRange(queryParams.value, dateRangeCreateTime.value, 'CreateTime'));
  ossList.value = response.rows;
  total.value = response.total;
  loading.value = false;
  showTable.value = true;
};
function checkFileSuffix(fileSuffix: string | string[]) {
  const arr = ['.png', '.jpg', '.jpeg'];
  const suffixArray = Array.isArray(fileSuffix) ? fileSuffix : [fileSuffix];
  return suffixArray.some((suffix) => arr.includes(suffix.toLowerCase()));
}

const formatSuffix = (fileSuffix: string | string[]) => {
  const suffixArray = Array.isArray(fileSuffix) ? fileSuffix : [fileSuffix];
  return suffixArray.find(Boolean) || 'FILE';
};

const shortUrl = (url?: string) => {
  if (!url) {
    return '--';
  }
  if (url.length <= 88) {
    return url;
  }
  return `${url.slice(0, 46)}...${url.slice(-26)}`;
};
/** 取消按钮 */
function cancel() {
  dialog.visible = false;
  reset();
}
/** 表单重置 */
function reset() {
  form.value = { ...initFormData };
  ossFormRef.value?.resetFields();
}
/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}
/** 重置按钮操作 */
function resetQuery() {
  showTable.value = false;
  dateRangeCreateTime.value = ['', ''];
  queryFormRef.value?.resetFields();
  queryParams.value.orderByColumn = defaultSort.value.prop;
  queryParams.value.isAsc = defaultSort.value.order;
  handleQuery();
}

const handleServiceFilter = (service: string) => {
  queryParams.value.service = service;
  handleQuery();
};
/** 选择条数  */
function handleSelectionChange(selection: OssVO[]) {
  ids.value = selection.map((item) => item.ossId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}
/** 设置列的排序为我们自定义的排序 */
const handleHeaderClass = ({ column }: any): any => {
  column.order = column.multiOrder;
};
/** 点击表头进行排序 */
const handleHeaderCLick = (column: any) => {
  if (column.sortable !== 'custom') {
    return;
  }
  switch (column.multiOrder) {
    case 'descending':
      column.multiOrder = 'ascending';
      break;
    case 'ascending':
      column.multiOrder = '';
      break;
    default:
      column.multiOrder = 'descending';
      break;
  }
  handleOrderChange(column.property, column.multiOrder);
};
const handleOrderChange = (prop: string, order: string) => {
  const orderByArr = queryParams.value.orderByColumn ? queryParams.value.orderByColumn.split(',') : [];
  const isAscArr = queryParams.value.isAsc ? queryParams.value.isAsc.split(',') : [];
  const propIndex = orderByArr.indexOf(prop);
  if (propIndex !== -1) {
    if (order) {
      //排序里已存在 只修改排序
      isAscArr[propIndex] = order;
    } else {
      //如果order为null 则删除排序字段和属性
      isAscArr.splice(propIndex, 1); //删除排序
      orderByArr.splice(propIndex, 1); //删除属性
    }
  } else {
    //排序里不存在则新增排序
    orderByArr.push(prop);
    isAscArr.push(order);
  }
  //合并排序
  queryParams.value.orderByColumn = orderByArr.join(',');
  queryParams.value.isAsc = isAscArr.join(',');
  getList();
};
/** 任务日志列表查询 */
const handleOssConfig = () => {
  router.push('/system/thirdparty?tab=oss');
};
/** 文件按钮操作 */
const handleFile = () => {
  reset();
  type.value = 0;
  dialog.visible = true;
  dialog.title = '上传文件';
};
/** 图片按钮操作 */
const handleImage = () => {
  reset();
  type.value = 1;
  dialog.visible = true;
  dialog.title = '上传图片';
};

const consumeRouteAction = () => {
  const action = typeof route.query.action === 'string' ? route.query.action : '';
  if (!action) {
    return;
  }
  if (action === 'file') {
    handleFile();
  }
  if (action === 'image') {
    handleImage();
  }
  const nextQuery = { ...route.query };
  delete nextQuery.action;
  router.replace({ path: route.path, query: nextQuery });
};
/** 提交按钮 */
const submitForm = () => {
  dialog.visible = false;
  getList();
};
/** 下载按钮操作 */
const handleDownload = (row: OssVO) => {
  proxy?.$download.oss(row.ossId);
};
/** 预览开关按钮  */
const handlePreviewListResource = async (preview: boolean) => {
  const text = preview ? '启用' : '停用';
  try {
    await proxy?.$modal.confirm('确认要"' + text + '""预览列表图片"配置吗?');
    await updateConfigByKey('sys.oss.previewListResource', preview);
    await getList();
    proxy?.$modal.msgSuccess(text + '成功');
  } catch {
    return;
  }
};
/** 删除按钮操作 */
const handleDelete = async (row?: OssVO) => {
  const ossIds = row?.ossId || ids.value;
  await proxy?.$modal.confirm('是否确认删除OSS对象存储编号为"' + ossIds + '"的数据项?');
  loading.value = true;
  await delOss(ossIds).finally(() => (loading.value = false));
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

onMounted(() => {
  getList();
  consumeRouteAction();
});
</script>

<style scoped lang="scss">
.oss-page {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.oss-hero,
.search-card,
.table-card {
  border-radius: 24px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.05);
}

.oss-hero {
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.16), transparent 32%),
    radial-gradient(circle at bottom left, rgba(59, 130, 246, 0.14), transparent 34%),
    linear-gradient(135deg, #f8fff9 0%, #f8fbff 54%, #fffaf1 100%);
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.hero-main {
  flex: 1;
  min-width: 0;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.92);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.hero-title {
  margin-top: 14px;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.hero-desc {
  margin-top: 8px;
  max-width: 680px;
  font-size: 14px;
  line-height: 1.8;
  color: #5b6475;
}

.hero-stats {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.9);
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
}

.stat-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
  color: #111827;
}

.hero-actions {
  width: 200px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.search-shell {
  margin: 0;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.section-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.7;
}

.search-form {
  margin-top: 16px;
}

.date-range-item {
  width: 308px;
}

.service-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 4px;
}

.service-chip {
  padding: 8px 14px;
  border: 0;
  border-radius: 999px;
  background: #f3f6fb;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
}

.service-chip.active {
  background: linear-gradient(135deg, #111827, #1f8f6b);
  color: #fff;
  box-shadow: 0 10px 24px rgba(17, 24, 39, 0.16);
}

.table-head {
  align-items: center;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.oss-table :deep(.el-table__cell) {
  padding: 14px 0;
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 14px;
}

.file-preview {
  width: 76px;
  height: 76px;
  flex-shrink: 0;
  border-radius: 18px;
  overflow: hidden;
  background: #f8fafc;
}

.file-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #0f766e;
  background: linear-gradient(135deg, #ecfeff, #f0fdf4);
}

.file-meta {
  min-width: 0;
}

.file-name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  word-break: break-all;
}

.file-original {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
  word-break: break-all;
}

.file-tags {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.url-cell {
  font-size: 12px;
  line-height: 1.7;
  color: #475569;
  word-break: break-all;
}

.uploader-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.uploader-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.uploader-time {
  font-size: 12px;
  color: #6b7280;
}

.upload-dialog-head {
  margin-bottom: 8px;
}

.upload-form {
  margin-top: 18px;
}

@media (max-width: 1200px) {
  .oss-hero {
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .oss-page {
    padding: 12px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .table-head,
  .section-head {
    flex-direction: column;
    align-items: stretch;
  }

  .table-actions {
    flex-wrap: wrap;
  }

  .date-range-item {
    width: 100%;
  }
}
</style>
