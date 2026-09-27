<template>
  <div class="hongniang-match-case">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="82px">
      <el-form-item label="红娘姓名" prop="hongniangName">
        <el-input v-model="queryParams.hongniangName" placeholder="请输入红娘姓名" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="男方查询" prop="maleKeyword">
        <el-input v-model="queryParams.maleKeyword" placeholder="编号/手机/名字" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="女方查询" prop="femaleKeyword">
        <el-input v-model="queryParams.femaleKeyword" placeholder="编号/手机/名字" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="当前阶段" prop="currentStage">
        <el-select v-model="queryParams.currentStage" clearable style="width: 180px">
          <el-option v-for="item in stageOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源类型" prop="sourceType">
        <el-select v-model="queryParams.sourceType" clearable style="width: 180px">
          <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['hongniang:matchCase:add']">新建案件</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="caseList">
      <el-table-column label="案件ID" prop="id" width="88" align="center" />
      <el-table-column label="红娘" prop="hongniangName" min-width="120" />
      <el-table-column label="男方" min-width="180">
        <template #default="{ row }">
          <div>{{ row.maleUsername || '-' }}</div>
          <div class="sub-text">UID: {{ row.maleUserId }} / {{ row.maleMobile || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="女方" min-width="180">
        <template #default="{ row }">
          <div>{{ row.femaleUsername || '-' }}</div>
          <div class="sub-text">UID: {{ row.femaleUserId }} / {{ row.femaleMobile || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="阶段" min-width="110">
        <template #default="{ row }">
          <el-tag :type="stageTagType(row.currentStage)">{{ row.currentStageLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" prop="sourceDisplay" min-width="180" />
      <el-table-column label="下次跟进" prop="nextFollowTime" width="170" />
      <el-table-column label="关联群数" prop="groupCount" width="96" align="center" />
      <el-table-column label="操作" width="430" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="View" @click="handleDetail(row)">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleEdit(row)" v-hasPermi="['hongniang:matchCase:edit']">修改</el-button>
          <el-button link type="primary" icon="Promotion" @click="handleCreateRequest(row)">牵线申请</el-button>
          <el-button link type="primary" icon="Top" @click="handleAdvance(row)" v-hasPermi="['hongniang:matchCase:edit']">推进</el-button>
          <el-button link type="primary" icon="ChatLineRound" @click="handleProgress(row)">跟进</el-button>
          <el-button link type="primary" icon="Connection" @click="handleBindGroups(row)">绑定群</el-button>
          <el-button link type="danger" icon="CircleClose" @click="handleClose(row)">关闭</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="dialog.title" v-model="dialog.visible" width="760px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="98px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="选择红娘" prop="hongniangId">
              <el-select v-model="form.hongniangId" style="width: 100%" filterable clearable @change="handleHongniangChange">
                <el-option v-for="item in hongniangList" :key="item.id" :label="`${item.hongniangName} (${item.phone || '-'})`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源类型" prop="sourceType">
              <el-select v-model="form.sourceType" style="width: 100%" @change="handleSourceTypeChange">
                <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="男方用户" prop="maleUserId">
              <el-select
                v-model="form.maleUserId"
                style="width: 100%"
                placeholder="请输入编号/手机/名字"
                filterable
                remote
                reserve-keyword
                clearable
                :remote-method="(keyword: string) => searchPoolUsers('male', keyword)"
                :loading="maleUserLoading"
              >
                <el-option v-for="item in maleOptions" :key="item.uid" :label="formatPoolUser(item)" :value="item.uid" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="女方用户" prop="femaleUserId">
              <el-select
                v-model="form.femaleUserId"
                style="width: 100%"
                placeholder="请输入编号/手机/名字"
                filterable
                remote
                reserve-keyword
                clearable
                :remote-method="(keyword: string) => searchPoolUsers('female', keyword)"
                :loading="femaleUserLoading"
              >
                <el-option v-for="item in femaleOptions" :key="item.uid" :label="formatPoolUser(item)" :value="item.uid" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="来源记录" v-if="form.sourceType !== 1">
              <el-select
                v-model="form.sourceRefId"
                style="width: 100%"
                filterable
                remote
                reserve-keyword
                clearable
                :remote-method="searchSourceCandidates"
                :loading="sourceLoading"
              >
                <el-option v-for="item in sourceCandidates" :key="item.id" :label="item.label" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下次跟进">
              <el-date-picker
                v-model="form.nextFollowTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="推进案件阶段" v-model="advanceDialog.visible" width="560px" append-to-body>
      <el-form ref="advanceRef" :model="advanceForm" :rules="advanceRules" label-width="100px">
        <el-form-item label="目标阶段" prop="targetStage">
          <el-select v-model="advanceForm.targetStage" style="width: 100%">
            <el-option v-for="item in forwardStageOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="本次跟进">
          <el-date-picker v-model="advanceForm.actualFollowTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker v-model="advanceForm.nextFollowTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="advanceForm.content" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="advanceDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitAdvance">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="新增跟进记录" v-model="progressDialog.visible" width="560px" append-to-body>
      <el-form ref="progressRef" :model="progressForm" :rules="progressRules" label-width="100px">
        <el-form-item label="跟进内容" prop="content">
          <el-input v-model="progressForm.content" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="计划时间">
          <el-date-picker v-model="progressForm.plannedFollowTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="实际时间">
          <el-date-picker v-model="progressForm.actualFollowTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="progressDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitProgress">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="绑定微信群" v-model="groupDialog.visible" width="620px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="选择群">
          <el-select v-model="groupDialog.groupIds" multiple filterable clearable style="width: 100%">
            <el-option v-for="item in groupOptions" :key="item.id" :label="item.groupName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitBindGroups">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="关闭案件" v-model="closeDialog.visible" width="560px" append-to-body>
      <el-form ref="closeRef" :model="closeForm" :rules="closeRules" label-width="100px">
        <el-form-item label="关闭原因" prop="closeReason">
          <el-input v-model="closeForm.closeReason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="closeForm.content" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog.visible = false">取消</el-button>
        <el-button type="danger" @click="submitClose">确认关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog title="发起牵线申请" v-model="requestDialog.visible" width="620px" append-to-body>
      <el-form ref="requestRef" :model="requestForm" :rules="requestRules" label-width="108px">
        <el-alert
          :title="requestChannelHint(requestForm.requestChannel)"
          type="info"
          :closable="false"
          class="mb12"
        />
        <el-form-item label="申请渠道" prop="requestChannel">
          <el-radio-group v-model="requestForm.requestChannel" @change="handleRequestChannelChange">
            <el-radio :label="1">App 私信申请</el-radio>
            <el-radio :label="2">红娘代分享微信</el-radio>
            <el-radio :label="3">公众号通知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发起方向">
          <el-radio-group v-model="requestDialog.direction" @change="handleRequestDirectionChange">
            <el-radio label="male_to_female">{{ requestDialog.maleLabel }} -> {{ requestDialog.femaleLabel }}</el-radio>
            <el-radio label="female_to_male">{{ requestDialog.femaleLabel }} -> {{ requestDialog.maleLabel }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="申请文案">
          <el-input
            v-model="requestForm.requestMessage"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            :placeholder="requestChannelPlaceholder(requestForm.requestChannel)"
          />
        </el-form-item>
        <el-form-item label="分享快照" v-if="requestForm.requestChannel === 2">
          <el-input
            v-model="requestForm.wechatShareSnapshot"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="可记录微信号、分享截图说明或线下转介绍备注"
          />
        </el-form-item>
        <el-form-item label="失效时间">
          <el-date-picker
            v-model="requestForm.expireTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="requestDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitRequest">发送申请</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="案件详情" size="760px">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="案件ID">{{ detailData.id }}</el-descriptions-item>
          <el-descriptions-item label="红娘">{{ detailData.hongniangName }}</el-descriptions-item>
          <el-descriptions-item label="男方">{{ detailData.maleUsername }} / {{ detailData.maleMobile || '-' }}</el-descriptions-item>
          <el-descriptions-item label="女方">{{ detailData.femaleUsername }} / {{ detailData.femaleMobile || '-' }}</el-descriptions-item>
          <el-descriptions-item label="阶段">
            <el-tag :type="stageTagType(detailData.currentStage)">{{ detailData.currentStageLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="来源">{{ detailData.sourceDisplay }}</el-descriptions-item>
          <el-descriptions-item label="下次跟进">{{ detailData.nextFollowTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近跟进">{{ detailData.lastFollowTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关闭原因" :span="2">{{ detailData.closeReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-block">
          <div class="section-head">
            <div class="section-title">关联微信群</div>
          </div>
          <div v-if="detailData.groups?.length" class="tag-wrap">
            <el-tag v-for="item in detailData.groups" :key="item.groupId" class="mr8 mb8">
              {{ item.groupName || ('群#' + item.groupId) }}
            </el-tag>
          </div>
          <el-empty v-else description="暂无关联微信群" :image-size="80" />
        </div>

        <div class="section-block">
          <div class="section-head">
            <div class="section-title">牵线申请记录</div>
            <el-button link type="primary" icon="Promotion" @click="handleCreateRequest(detailData)">发起申请</el-button>
          </div>
          <el-table v-if="detailData.requestList?.length" :data="detailData.requestList" size="small" border>
            <el-table-column label="申请ID" prop="id" width="86" />
            <el-table-column label="方向" min-width="180">
              <template #default="{ row }">{{ row.fromUserName || row.fromUserId }} -> {{ row.toUserName || row.toUserId }}</template>
            </el-table-column>
            <el-table-column label="渠道" width="110">
              <template #default="{ row }">
                <el-tag :type="requestChannelTagType(row.requestChannel)">{{ row.requestChannelLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="requestStatusTagType(row.requestStatus)">{{ row.requestStatusLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申请文案" prop="requestMessage" min-width="180" show-overflow-tooltip />
            <el-table-column label="失效时间" prop="expireTime" width="170" />
          </el-table>
          <el-empty v-else description="暂无牵线申请记录" :image-size="80" />
        </div>

        <div class="section-block">
          <div class="section-head">
            <div class="section-title">时间线</div>
          </div>
          <el-timeline v-if="detailData.progressList?.length">
            <el-timeline-item
              v-for="item in detailData.progressList"
              :key="item.id"
              :timestamp="item.createTime || item.actualFollowTime || item.plannedFollowTime"
            >
              <div class="timeline-title">{{ progressTypeLabel(item.progressType) }}</div>
              <div class="timeline-content">{{ item.content || '-' }}</div>
              <div class="sub-text" v-if="item.stageAfter !== undefined && item.stageAfter !== null">
                阶段：{{ stageLabel(item.stageBefore) }} -> {{ stageLabel(item.stageAfter) }}
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无进度记录" :image-size="80" />
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElForm } from 'element-plus';
import { getAllHongniangs } from '@/api/hongniang/hongniangInfo';
import { addHongniangMatchProgress, advanceHongniangMatchCase, bindHongniangMatchCaseGroups, closeHongniangMatchCase, createHongniangMatchCase, createHongniangMatchRequest, getHongniangMatchCase, listHongniangMatchCase, searchHongniangMatchCaseSources, searchHongniangPoolUsers, updateHongniangMatchCase } from '@/api/hongniang/matchCase';
import { listHongniangWechatGroup } from '@/api/hongniang/wechatGroup';

const { proxy } = getCurrentInstance() as any;
const route = useRoute();
const router = useRouter();

type FormInstance = InstanceType<typeof ElForm>;

const queryRef = ref<FormInstance>();
const formRef = ref<FormInstance>();
const advanceRef = ref<FormInstance>();
const progressRef = ref<FormInstance>();
const closeRef = ref<FormInstance>();
const requestRef = ref<FormInstance>();

const loading = ref(false);
const showSearch = ref(true);
const total = ref(0);
const caseList = ref<any[]>([]);
const hongniangList = ref<any[]>([]);
const maleOptions = ref<any[]>([]);
const femaleOptions = ref<any[]>([]);
const sourceCandidates = ref<any[]>([]);
const groupOptions = ref<any[]>([]);
const detailVisible = ref(false);
const detailData = ref<any>(null);
const submitLoading = ref(false);
const maleUserLoading = ref(false);
const femaleUserLoading = ref(false);
const sourceLoading = ref(false);

const stageOptions = [
  { label: '待建档', value: 0 },
  { label: '推荐中', value: 1 },
  { label: '已建联', value: 2 },
  { label: '已见面', value: 3 },
  { label: '交往中', value: 4 },
  { label: '见家长', value: 5 },
  { label: '已结婚', value: 6 },
  { label: '已生子', value: 7 },
  { label: '已关闭', value: 8 }
];

const forwardStageOptions = stageOptions.filter(item => item.value < 8);

const sourceOptions = [
  { label: '手工建档', value: 1 },
  { label: '智能推荐快照', value: 2 },
  { label: '相亲活动', value: 3 },
  { label: '微信群组局', value: 4 }
];

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  hongniangId: undefined as number | undefined,
  hongniangName: undefined as string | undefined,
  maleKeyword: undefined as string | undefined,
  femaleKeyword: undefined as string | undefined,
  currentStage: undefined as number | undefined,
  sourceType: undefined as number | undefined
});

const dialog = reactive({
  visible: false,
  title: '新建牵线案件'
});

const form = reactive<any>({
  id: undefined,
  hongniangId: undefined,
  maleUserId: undefined,
  femaleUserId: undefined,
  sourceType: 1,
  sourceRefId: undefined,
  nextFollowTime: undefined,
  remark: ''
});

const advanceDialog = reactive({ visible: false });
const advanceForm = reactive<any>({
  caseId: undefined,
  targetStage: undefined,
  actualFollowTime: undefined,
  nextFollowTime: undefined,
  content: ''
});

const progressDialog = reactive({ visible: false });
const progressForm = reactive<any>({
  caseId: undefined,
  progressType: 3,
  content: '',
  plannedFollowTime: undefined,
  actualFollowTime: undefined
});

const groupDialog = reactive<any>({
  visible: false,
  caseId: undefined,
  groupIds: []
});

const closeDialog = reactive({ visible: false });
const closeForm = reactive<any>({
  caseId: undefined,
  closeReason: '',
  content: ''
});

const requestDialog = reactive<any>({
  visible: false,
  caseId: undefined,
  direction: 'male_to_female',
  maleUserId: undefined,
  femaleUserId: undefined,
  maleLabel: '男方',
  femaleLabel: '女方'
});

const requestForm = reactive<any>({
  caseId: undefined,
  fromUserId: undefined,
  toUserId: undefined,
  requestChannel: 1,
  requestMessage: '',
  wechatShareSnapshot: '',
  expireTime: undefined
});

const rules = reactive({
  hongniangId: [{ required: true, message: '请选择红娘', trigger: 'change' }],
  maleUserId: [{ required: true, message: '请选择男方用户', trigger: 'change' }],
  femaleUserId: [{ required: true, message: '请选择女方用户', trigger: 'change' }]
});

const advanceRules = reactive({
  targetStage: [{ required: true, message: '请选择目标阶段', trigger: 'change' }]
});

const progressRules = reactive({
  content: [{ required: true, message: '请填写跟进内容', trigger: 'blur' }]
});

const closeRules = reactive({
  closeReason: [{ required: true, message: '请填写关闭原因', trigger: 'blur' }]
});

const requestRules = reactive({
  requestChannel: [{ required: true, message: '请选择申请渠道', trigger: 'change' }],
  fromUserId: [{ required: true, message: '请选择发起方向', trigger: 'change' }],
  toUserId: [{ required: true, message: '请选择发起方向', trigger: 'change' }]
});

const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listHongniangMatchCase(queryParams);
    caseList.value = res.page?.list || [];
    total.value = res.page?.totalCount || 0;
  } finally {
    loading.value = false;
  }
};

const loadHongniangs = async () => {
  const res: any = await getAllHongniangs();
  hongniangList.value = res.page?.list || [];
};

const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryRef.value?.resetFields();
  handleQuery();
};

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    hongniangId: undefined,
    maleUserId: undefined,
    femaleUserId: undefined,
    sourceType: 1,
    sourceRefId: undefined,
    nextFollowTime: undefined,
    remark: ''
  });
  maleOptions.value = [];
  femaleOptions.value = [];
  sourceCandidates.value = [];
};

const handleAdd = async () => {
  resetForm();
  dialog.title = '新建牵线案件';
  dialog.visible = true;
  await loadHongniangs();
  await applyRoutePrefill();
};

const handleEdit = async (row: any) => {
  resetForm();
  await loadHongniangs();
  const res: any = await getHongniangMatchCase(row.id);
  const data = res.data || {};
  Object.assign(form, {
    id: data.id,
    hongniangId: data.hongniangId,
    maleUserId: data.maleUserId,
    femaleUserId: data.femaleUserId,
    sourceType: data.sourceType,
    sourceRefId: data.sourceRefId,
    nextFollowTime: data.nextFollowTime,
    remark: data.remark
  });
  dialog.title = '修改牵线案件';
  dialog.visible = true;
};

const submitForm = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    submitLoading.value = true;
    try {
      if (form.id) {
        await updateHongniangMatchCase(form);
        proxy.$modal.msgSuccess('修改成功');
      } else {
        await createHongniangMatchCase(form);
        proxy.$modal.msgSuccess('创建成功');
      }
      dialog.visible = false;
      await getList();
      clearRoutePrefill();
    } finally {
      submitLoading.value = false;
    }
  });
};

const handleHongniangChange = async () => {
  form.maleUserId = undefined;
  form.femaleUserId = undefined;
  form.sourceRefId = undefined;
  maleOptions.value = [];
  femaleOptions.value = [];
  sourceCandidates.value = [];
  if (!form.hongniangId) return;
  await Promise.all([searchPoolUsers('male', ''), searchPoolUsers('female', '')]);
};

const searchPoolUsers = async (role: 'male' | 'female', keyword: string) => {
  if (!form.hongniangId) return;
  const targetLoading = role === 'male' ? maleUserLoading : femaleUserLoading;
  targetLoading.value = true;
  try {
    const res: any = await searchHongniangPoolUsers({
      hongniangId: form.hongniangId,
      keyword: keyword?.trim() || undefined,
      gender: role === 'male' ? 1 : 2,
      excludeUserId: role === 'male' ? form.femaleUserId : form.maleUserId,
      limit: 30
    });
    if (role === 'male') {
      maleOptions.value = res.list || [];
    } else {
      femaleOptions.value = res.list || [];
    }
  } finally {
    targetLoading.value = false;
  }
};

const handleSourceTypeChange = () => {
  form.sourceRefId = undefined;
  sourceCandidates.value = [];
};

const searchSourceCandidates = async (keyword: string) => {
  if (!form.hongniangId || !form.sourceType || form.sourceType === 1) return;
  sourceLoading.value = true;
  try {
    const res: any = await searchHongniangMatchCaseSources({
      hongniangId: form.hongniangId,
      sourceType: form.sourceType,
      keyword: keyword?.trim() || undefined,
      limit: 30
    });
    sourceCandidates.value = res.list || [];
  } finally {
    sourceLoading.value = false;
  }
};

const handleDetail = async (row: any) => {
  const res: any = await getHongniangMatchCase(row.id);
  detailData.value = res.data || {};
  detailVisible.value = true;
};

const handleAdvance = (row: any) => {
  Object.assign(advanceForm, {
    caseId: row.id,
    targetStage: undefined,
    actualFollowTime: undefined,
    nextFollowTime: row.nextFollowTime || undefined,
    content: ''
  });
  advanceDialog.visible = true;
};

const submitAdvance = () => {
  advanceRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    await advanceHongniangMatchCase(advanceForm);
    proxy.$modal.msgSuccess('推进成功');
    advanceDialog.visible = false;
    await getList();
    if (detailVisible.value && detailData.value?.id === advanceForm.caseId) {
      await handleDetail({ id: advanceForm.caseId });
    }
  });
};

const handleProgress = (row: any) => {
  Object.assign(progressForm, {
    caseId: row.id,
    progressType: 3,
    content: '',
    plannedFollowTime: row.nextFollowTime || undefined,
    actualFollowTime: undefined
  });
  progressDialog.visible = true;
};

const submitProgress = () => {
  progressRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    await addHongniangMatchProgress(progressForm);
    proxy.$modal.msgSuccess('跟进记录已保存');
    progressDialog.visible = false;
    await getList();
    if (detailVisible.value && detailData.value?.id === progressForm.caseId) {
      await handleDetail({ id: progressForm.caseId });
    }
  });
};

const handleBindGroups = async (row: any) => {
  groupDialog.caseId = row.id;
  groupDialog.groupIds = [];
  const res: any = await listHongniangWechatGroup({
    pageNum: 1,
    pageSize: 999,
    hongniangId: row.hongniangId
  });
  groupOptions.value = res.page?.list || [];
  if (detailVisible.value && detailData.value?.id === row.id && detailData.value.groups?.length) {
    groupDialog.groupIds = detailData.value.groups.map((item: any) => item.groupId);
  } else {
    const detailRes: any = await getHongniangMatchCase(row.id);
    groupDialog.groupIds = (detailRes.data?.groups || []).map((item: any) => item.groupId);
  }
  groupDialog.visible = true;
};

const submitBindGroups = async () => {
  await bindHongniangMatchCaseGroups({
    caseId: groupDialog.caseId,
    groupIds: groupDialog.groupIds
  });
  proxy.$modal.msgSuccess('微信群绑定成功');
  groupDialog.visible = false;
  await getList();
  if (detailVisible.value && detailData.value?.id === groupDialog.caseId) {
    await handleDetail({ id: groupDialog.caseId });
  }
};

const handleClose = (row: any) => {
  Object.assign(closeForm, {
    caseId: row.id,
    closeReason: '',
    content: ''
  });
  closeDialog.visible = true;
};

const submitClose = () => {
  closeRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    await closeHongniangMatchCase(closeForm);
    proxy.$modal.msgSuccess('案件已关闭');
    closeDialog.visible = false;
    await getList();
    if (detailVisible.value && detailData.value?.id === closeForm.caseId) {
      await handleDetail({ id: closeForm.caseId });
    }
  });
};

const defaultRequestExpireTime = () => {
  const expireAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${expireAt.getFullYear()}-${pad(expireAt.getMonth() + 1)}-${pad(expireAt.getDate())} ${pad(expireAt.getHours())}:${pad(expireAt.getMinutes())}:${pad(expireAt.getSeconds())}`;
};

const syncRequestDirection = () => {
  if (requestDialog.direction === 'female_to_male') {
    requestForm.fromUserId = requestDialog.femaleUserId;
    requestForm.toUserId = requestDialog.maleUserId;
  } else {
    requestForm.fromUserId = requestDialog.maleUserId;
    requestForm.toUserId = requestDialog.femaleUserId;
  }
};

const handleRequestDirectionChange = () => {
  syncRequestDirection();
};

const handleRequestChannelChange = (channel: number) => {
  if (channel !== 2) {
    requestForm.wechatShareSnapshot = '';
  }
};

const handleCreateRequest = (row: any) => {
  requestDialog.caseId = row.id;
  requestDialog.maleUserId = row.maleUserId;
  requestDialog.femaleUserId = row.femaleUserId;
  requestDialog.maleLabel = row.maleUsername || `男方#${row.maleUserId}`;
  requestDialog.femaleLabel = row.femaleUsername || `女方#${row.femaleUserId}`;
  requestDialog.direction = 'male_to_female';
  Object.assign(requestForm, {
    caseId: row.id,
    fromUserId: row.maleUserId,
    toUserId: row.femaleUserId,
    requestChannel: 1,
    requestMessage: '',
    wechatShareSnapshot: '',
    expireTime: defaultRequestExpireTime()
  });
  requestDialog.visible = true;
};

const submitRequest = () => {
  requestRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    await createHongniangMatchRequest(requestForm);
    proxy.$modal.msgSuccess(requestSubmitSuccessText(requestForm.requestChannel));
    requestDialog.visible = false;
    await getList();
    if (detailVisible.value && detailData.value?.id === requestForm.caseId) {
      await handleDetail({ id: requestForm.caseId });
    }
  });
};

const formatPoolUser = (user: any) => `编号:${user.hongniangUserNo || '-'} | ${user.username || '未命名'} | ${user.mobile || '-'} | UID:${user.uid}`;
const stageTagType = (stage?: number) => {
  if (stage === 6 || stage === 7) return 'success';
  if (stage === 8) return 'info';
  if (stage === 3 || stage === 4 || stage === 5) return 'warning';
  return 'primary';
};
const stageLabel = (stage?: number) => stageOptions.find(item => item.value === stage)?.label || '-';
const progressTypeLabel = (type?: number) => {
  if (type === 1) return '建档';
  if (type === 2) return '阶段推进';
  if (type === 3) return '跟进记录';
  if (type === 4) return '关闭案件';
  return '-';
};
const requestStatusTagType = (status?: number) => {
  if (status === 2) return 'success';
  if (status === 3 || status === 4) return 'danger';
  if (status === 1) return 'warning';
  return 'info';
};
const requestChannelHint = (channel?: number) => {
  if (channel === 2) {
    return '红娘代分享微信：用于线下微信已互通场景，后台只登记快照。';
  }
  if (channel === 3) {
    return '公众号通知：需用户已关注并绑定公众号，点击通知后回系统处理牵线申请。';
  }
  return 'App 私信申请：会在 App 聊天里生成可接受/拒绝的牵线卡片。';
};
const requestChannelPlaceholder = (channel?: number) => {
  if (channel === 2) {
    return '填写红娘代分享时的说明，如已互推微信、线下已转介绍等';
  }
  if (channel === 3) {
    return '填写公众号模板消息里展示的牵线说明';
  }
  return '填写发给对方的牵线话术';
};
const requestChannelTagType = (channel?: number) => {
  if (channel === 2) return 'warning';
  if (channel === 3) return 'success';
  return 'primary';
};
const requestSubmitSuccessText = (channel?: number) => {
  if (channel === 2) return '代分享记录已创建';
  if (channel === 3) return '公众号通知已发送';
  return '牵线申请已发送';
};

const applyRoutePrefill = async () => {
  const hongniangId = route.query.hongniangId ? Number(route.query.hongniangId) : undefined;
  const prefillUserId = route.query.prefillUserId ? Number(route.query.prefillUserId) : undefined;
  const prefillGender = route.query.prefillGender ? Number(route.query.prefillGender) : undefined;
  if (!hongniangId) return;
  form.hongniangId = hongniangId;
  await handleHongniangChange();
  if (prefillUserId && prefillGender === 1) {
    form.maleUserId = prefillUserId;
  }
  if (prefillUserId && prefillGender === 2) {
    form.femaleUserId = prefillUserId;
  }
};

const clearRoutePrefill = () => {
  if (route.query.hongniangId || route.query.prefillUserId || route.query.prefillGender) {
    router.replace({ path: route.path, query: {} });
  }
};

onMounted(async () => {
  await Promise.all([loadHongniangs(), getList()]);
  if (route.query.autoCreate === '1') {
    await handleAdd();
  }
});
</script>

<style scoped>
.hongniang-match-case {
  padding: 20px;
}

.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.section-block {
  margin-top: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
}

.mr8 {
  margin-right: 8px;
}

.mb8 {
  margin-bottom: 8px;
}

.mb12 {
  margin-bottom: 12px;
}

.timeline-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.timeline-content {
  margin-bottom: 4px;
}
</style>
