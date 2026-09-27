<template>
  <el-dialog
    :title="!form.id ? '新增圈子' : '修改圈子'"
    v-model="visible"
    width="600px"
    :close-on-click-modal="false"
    draggable
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-form-item label="圈主" prop="uid">
        <el-select
          v-model="form.uid"
          placeholder="请选择圈主"
          filterable
          remote
          :remote-method="searchUser"
          :loading="userLoading"
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="user in userList"
            :key="user.uid"
            :label="`${user.username} (ID: ${user.uid})`"
            :value="user.uid"
          />
        </el-select>
      </el-form-item>
      
      <el-form-item label="圈子分类" prop="cateId">
        <el-select
          v-model="form.cateId"
          placeholder="请选择圈子分类"
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="category in categoryList"
            :key="category.cateId"
            :label="category.cateName"
            :value="category.cateId"
          />
        </el-select>
      </el-form-item>
      
      <el-form-item label="圈子名称" prop="topicName">
        <el-input
          v-model="form.topicName"
          placeholder="请输入圈子名称"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          placeholder="请输入描述"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="背景图" prop="bgImage">
        <shejiao-image-upload
          v-model="form.bgImage"
          size="cover"
          tip="建议尺寸：400*300像素，jpg、png图片类型"
        />
      </el-form-item>
      
      <el-form-item label="圈子头像" prop="coverImage">
        <shejiao-image-upload
          v-model="form.coverImage"
          size="avatar"
          tip="建议尺寸：100*100像素，jpg、png图片类型"
        />
      </el-form-item>
      
      <el-form-item label="圈子状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :label="0">正常</el-radio>
          <el-radio :label="1">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="推荐类型" prop="topType">
        <el-radio-group v-model="form.topType">
          <el-radio :label="0">不推荐</el-radio>
          <el-radio :label="1">首页推荐</el-radio>
          <el-radio :label="2">圈子页推荐</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="首页推荐内容" prop="indexRecommend">
        <el-radio-group v-model="form.indexRecommend">
          <el-radio :label="0">否</el-radio>
          <el-radio :label="1">是</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="是否推荐" prop="isRecommend">
        <el-radio-group v-model="form.isRecommend">
          <el-radio :label="0">否</el-radio>
          <el-radio :label="1">是</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="排序" prop="sortOrder">
        <el-input-number
          v-model="form.sortOrder"
          :min="0"
          :max="9999"
          controls-position="right"
        />
      </el-form-item>
      
      <el-form-item label="进圈条件" prop="rest">
        <el-radio-group v-model="form.rest">
          <el-radio :label="0">无限制</el-radio>
          <el-radio :label="1">答题并审核</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="问题内容" prop="question" v-if="form.rest === 1">
        <el-input
          v-model="form.question"
          type="textarea"
          :rows="3"
          placeholder="请输入问题内容"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="是否私密" prop="isPrivacy">
        <el-radio-group v-model="form.isPrivacy">
          <el-radio :label="0">公开</el-radio>
          <el-radio :label="1">私密</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="ShejiaoTopicAddOrUpdate">
import { ref, reactive, getCurrentInstance, onMounted } from 'vue';
import { getTopic, addTopic, updateTopic } from '@/api/shejiao/topic';
import { TopicForm } from '@/api/shejiao/topic/types';
import { listUser } from '@/api/shejiao/user';
import { shejiaoService } from '@/utils/request';
import ShejiaoImageUpload from '@/components/ShejiaoImageUpload/index.vue';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const visible = ref(false);
const submitLoading = ref(false);
const formRef = ref();
const userList = ref<any[]>([]);
const userLoading = ref(false);
const categoryList = ref<any[]>([]);

const form = reactive<TopicForm>({
  id: undefined,
  uid: undefined,
  cateId: undefined,
  topicName: '',
  description: '',
  coverImage: '',
  bgImage: '',
  topType: 0,
  status: 1,
  indexRecommend: 1,
  isRecommend: 0,
  sortOrder: 0,
  rest: 0,
  question: '',
  isPrivacy: 0
});

const rules = {
  uid: [
    { required: true, message: '请选择圈主', trigger: 'change' }
  ],
  cateId: [
    { required: true, message: '请选择圈子分类', trigger: 'change' }
  ],
  topicName: [
    { required: true, message: '圈子名称不能为空', trigger: 'blur' },
    { min: 2, max: 20, message: '圈子名称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '圈子描述不能为空', trigger: 'blur' },
    { min: 5, max: 200, message: '圈子描述长度在 5 到 200 个字符', trigger: 'blur' }
  ],
  coverImage: [
    { required: true, message: '圈子头像不能为空', trigger: 'change' }
  ],
  bgImage: [
    { required: true, message: '背景图不能为空', trigger: 'change' }
  ],
  status: [
    { required: true, message: '圈子状态不能为空', trigger: 'change' }
  ]
};

/** 获取分类列表 */
const getCategoryList = async () => {
  try {
    const res: any = await shejiaoService({
      url: '/admin/category/getList',
      method: 'get'
    });
    categoryList.value = res.result || [];
    console.log('分类列表:', categoryList.value);
  } catch (error) {
    console.error('获取分类列表失败:', error);
  }
};

/** 搜索用户 */
const searchUser = async (query: string) => {
  userLoading.value = true;
  try {
    const res: any = await listUser({ 
      pageNum: 1, 
      pageSize: 20,
      key: query || undefined
    });
    // 社交模块后端返回结构: { code: 0, page: { list: [...] } }
    userList.value = res.page?.list || [];
    console.log('用户列表:', userList.value);
  } catch (error) {
    console.error('搜索用户失败:', error);
  } finally {
    userLoading.value = false;
  }
};

/** 打开对话框 */
const open = async (id?: number) => {
  visible.value = true;
  resetForm();
  
  // 加载分类列表
  await getCategoryList();
  
  // 新增时，加载初始用户列表
  if (!id) {
    searchUser('');
  }
  
  if (id) {
    try {
      const res: any = await getTopic(id);
      if (res.topic) {
        Object.assign(form, {
          id: res.topic.id,
          uid: res.topic.uid,
          cateId: res.topic.cateId,
          topicName: res.topic.topicName,
          description: res.topic.description,
          coverImage: res.topic.coverImage,
          bgImage: res.topic.bgImage,
          topType: res.topic.topType ?? 0,
          status: res.topic.status ?? 1,
          indexRecommend: res.topic.indexRecommend ?? 1,
          isRecommend: res.topic.isRecommend ?? 0,
          sortOrder: res.topic.sortOrder ?? 0,
          rest: res.topic.rest ?? 0,
          question: res.topic.question ?? '',
          isPrivacy: res.topic.isPrivacy ?? 0
        });
        
        // 编辑时，加载该用户信息到列表中
        if (res.topic.uid) {
          searchUser('');
        }
      }
    } catch (error) {
      proxy?.$modal.msgError('获取圈子信息失败');
      visible.value = false;
    }
  }
};

/** 重置表单 */
const resetForm = () => {
  form.id = undefined;
  form.uid = undefined;
  form.cateId = undefined;
  form.topicName = '';
  form.description = '';
  form.coverImage = '';
  form.bgImage = '';
  form.topType = 0;
  form.status = 1;
  form.indexRecommend = 1;
  form.isRecommend = 0;
  form.sortOrder = 0;
  form.rest = 0;
  form.question = '';
  form.isPrivacy = 0;
  formRef.value?.clearValidate();
};

/** 提交表单 */
const handleSubmit = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true;
      try {
        const data: TopicForm = {
          id: form.id,
          uid: form.uid,
          cateId: form.cateId,
          topicName: form.topicName,
          description: form.description,
          coverImage: form.coverImage,
          bgImage: form.bgImage,
          topType: form.topType,
          status: form.status,
          indexRecommend: form.indexRecommend,
          isRecommend: form.isRecommend,
          sortOrder: form.sortOrder,
          rest: form.rest,
          question: form.question,
          isPrivacy: form.isPrivacy
        };
        
        if (form.id) {
          await updateTopic(data);
          proxy?.$modal.msgSuccess('修改成功');
        } else {
          await addTopic(data);
          proxy?.$modal.msgSuccess('新增成功');
        }
        
        visible.value = false;
        emit('success');
      } catch (error) {
        console.error('提交失败:', error);
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

/** 关闭对话框 */
const handleClose = () => {
  visible.value = false;
  resetForm();
};

const emit = defineEmits(['success']);

defineExpose({
  open
});
</script>

<style scoped lang="scss">
</style>
