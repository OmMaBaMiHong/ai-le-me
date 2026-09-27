<template>
  <el-dialog
    :title="!form.id ? '新增AI视频模板' : '修改AI视频模板'"
    v-model="visible"
    width="700px"
    :close-on-click-modal="false"
    draggable
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item label="模板名称" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入模板名称"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="服务商" prop="provider">
        <el-select
          v-model="form.provider"
          placeholder="请选择服务商"
          clearable
          style="width: 100%"
        >
          <el-option label="即梦" value="jm" />
          <el-option label="可灵" value="kl" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="场景分类" prop="category">
        <el-select
          v-model="form.category"
          placeholder="请选择场景分类"
          clearable
          filterable
          allow-create
          style="width: 100%"
        >
          <el-option label="自我介绍" value="self_intro" />
          <el-option label="约会邀请" value="dating" />
          <el-option label="表白/追求" value="confession" />
          <el-option label="日常分享" value="daily" />
        </el-select>
        <div style="color: #999; font-size: 12px; margin-top: 5px">
          提示：可选择预设分类或输入自定义分类
        </div>
      </el-form-item>
      
      <el-form-item label="模板ID" prop="templateId">
        <el-input
          v-model="form.templateId"
          placeholder="请输入服务商提供的模板ID"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="封面图" prop="coverImage">
        <el-input
          v-model="form.coverImage"
          placeholder="请输入封面图URL"
          clearable
        >
          <template #append>
            <image-upload v-model="form.coverImage" :limit="1" />
          </template>
        </el-input>
      </el-form-item>
      
      <el-form-item label="预览视频" prop="previewVideo">
        <el-input
          v-model="form.previewVideo"
          placeholder="请输入预览视频URL（可选）"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="请输入模板描述"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="脚本变量" prop="scriptVariables">
        <el-input
          v-model="form.scriptVariables"
          type="textarea"
          :rows="4"
          placeholder='JSON格式，如：{"name":"姓名","age":"年龄"}'
          clearable
        />
        <div style="color: #999; font-size: 12px; margin-top: 5px">
          提示：请输入JSON格式的变量配置
        </div>
      </el-form-item>
      
      <el-form-item label="时长(秒)" prop="duration">
        <el-input-number
          v-model="form.duration"
          :min="1"
          :max="300"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="排序" prop="sort">
        <el-input-number
          v-model="form.sort"
          :min="0"
          :max="9999"
          controls-position="right"
          style="width: 100%"
        />
        <div style="color: #999; font-size: 12px; margin-top: 5px">
          提示：数值越小越靠前
        </div>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="ShejiaoVideoTemplateAddOrUpdate">
import { ref, reactive, getCurrentInstance } from 'vue';
import { getVideoTemplate, addVideoTemplate, updateVideoTemplate } from '@/api/shejiao/videoTemplate';
import { VideoTemplateForm } from '@/api/shejiao/videoTemplate/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const visible = ref(false);
const submitLoading = ref(false);
const formRef = ref();

const form = reactive<VideoTemplateForm>({
  id: undefined,
  name: '',
  templateId: '',
  provider: 'jm',
  category: 'self_intro',
  coverImage: '',
  previewVideo: '',
  description: '',
  scriptVariables: '',
  duration: 30,
  status: 1,
  sort: 0
});

const rules = {
  name: [
    { required: true, message: '模板名称不能为空', trigger: 'blur' },
    { min: 2, max: 50, message: '模板名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  provider: [
    { required: true, message: '服务商不能为空', trigger: 'change' }
  ],
  category: [
    { required: true, message: '场景分类不能为空', trigger: 'change' }
  ],
  templateId: [
    { required: true, message: '模板ID不能为空', trigger: 'blur' }
  ],
  coverImage: [
    { required: true, message: '封面图不能为空', trigger: 'blur' }
  ],
  duration: [
    { required: true, message: '时长不能为空', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '状态不能为空', trigger: 'change' }
  ],
  sort: [
    { required: true, message: '排序不能为空', trigger: 'blur' }
  ]
};

/** 打开对话框 */
const open = async (id?: number) => {
  visible.value = true;
  resetForm();
  
  if (id) {
    try {
      const res: any = await getVideoTemplate(id);
      if (res.videoTemplate) {
        Object.assign(form, {
          id: res.videoTemplate.id,
          name: res.videoTemplate.name,
          templateId: res.videoTemplate.templateId,
          provider: res.videoTemplate.provider,
          category: res.videoTemplate.category || 'self_intro',
          coverImage: res.videoTemplate.coverImage,
          previewVideo: res.videoTemplate.previewVideo,
          description: res.videoTemplate.description,
          scriptVariables: res.videoTemplate.scriptVariables,
          duration: res.videoTemplate.duration ?? 30,
          status: res.videoTemplate.status ?? 1,
          sort: res.videoTemplate.sort ?? 0
        });
      }
    } catch (error) {
      proxy?.$modal.msgError('获取模板信息失败');
      visible.value = false;
    }
  }
};

/** 重置表单 */
const resetForm = () => {
  form.id = undefined;
  form.name = '';
  form.templateId = '';
  form.provider = 'jm';
  form.category = 'self_intro';
  form.coverImage = '';
  form.previewVideo = '';
  form.description = '';
  form.scriptVariables = '';
  form.duration = 30;
  form.status = 1;
  form.sort = 0;
  formRef.value?.clearValidate();
};

/** 提交表单 */
const handleSubmit = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      // 验证 scriptVariables 是否为有效 JSON
      if (form.scriptVariables) {
        try {
          JSON.parse(form.scriptVariables);
        } catch (e) {
          proxy?.$modal.msgError('脚本变量必须是有效的JSON格式');
          return;
        }
      }
      
      submitLoading.value = true;
      try {
        const data: VideoTemplateForm = { ...form };
        
        if (form.id) {
          await updateVideoTemplate(data);
          proxy?.$modal.msgSuccess('修改成功');
        } else {
          await addVideoTemplate(data);
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
