<template>
  <div class="shejiao-image-upload">
    <el-upload
      class="image-uploader"
      :action="uploadUrl"
      :headers="headers"
      :show-file-list="false"
      :on-success="handleSuccess"
      :before-upload="beforeUpload"
      :class="{ 'is-disabled': disabled }"
    >
      <img v-if="modelValue" :src="modelValue" :class="imageClass" />
      <el-icon v-else :class="iconClass">
        <Plus />
      </el-icon>
    </el-upload>
    <div v-if="tip" class="upload-tip">{{ tip }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { globalHeaders } from '@/utils/request';
import type { UploadProps } from 'element-plus';

interface Props {
  modelValue?: string;
  // 图片尺寸类型：avatar(100x100), cover(400x300), banner(800x200)
  size?: 'avatar' | 'cover' | 'banner' | 'custom';
  // 自定义宽度（size为custom时生效）
  width?: number;
  // 自定义高度（size为custom时生效）
  height?: number;
  // 文件大小限制(MB)
  maxSize?: number;
  // 提示文字
  tip?: string;
  // 是否禁用
  disabled?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  size: 'avatar',
  width: 100,
  height: 100,
  maxSize: 5,
  tip: '',
  disabled: false
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const uploadUrl = computed(() => import.meta.env.VITE_APP_BASE_API + '/resource/oss/upload');
const headers = computed(() => globalHeaders());

// 根据 size 计算图片样式类
const imageClass = computed(() => {
  const sizeMap = {
    avatar: 'image-avatar',
    cover: 'image-cover',
    banner: 'image-banner',
    custom: 'image-custom'
  };
  return sizeMap[props.size] || sizeMap.avatar;
});

// 根据 size 计算图标样式类
const iconClass = computed(() => {
  const sizeMap = {
    avatar: 'icon-avatar',
    cover: 'icon-cover',
    banner: 'icon-banner',
    custom: 'icon-custom'
  };
  return sizeMap[props.size] || sizeMap.avatar;
});

// 上传前校验
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  if (props.disabled) {
    return false;
  }

  const isImage = file.type.startsWith('image/');
  const isLtMaxSize = file.size / 1024 / 1024 < props.maxSize;

  if (!isImage) {
    proxy?.$modal.msgError('只能上传图片文件！');
    return false;
  }
  if (!isLtMaxSize) {
    proxy?.$modal.msgError(`上传图片大小不能超过 ${props.maxSize}MB！`);
    return false;
  }

  proxy?.$modal.loading('正在上传图片，请稍候...');
  return true;
};

// 上传成功回调 - 关键：直接返回完整 URL
const handleSuccess: UploadProps['onSuccess'] = (response: any) => {
  proxy?.$modal.closeLoading();
  
  if (response.code === 200) {
    // ✅ 直接使用后端返回的完整 URL，而不是 ossId
    emit('update:modelValue', response.data.url);
    proxy?.$modal.msgSuccess('上传成功');
  } else {
    proxy?.$modal.msgError(response.msg || '上传失败');
  }
};
</script>

<style scoped lang="scss">
.shejiao-image-upload {
  display: inline-block;

  .image-uploader {
    :deep(.el-upload) {
      border: 1px dashed var(--el-border-color);
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: var(--el-transition-duration-fast);
      display: flex;
      align-items: center;
      justify-content: center;

      &:hover {
        border-color: var(--el-color-primary);
      }
    }

    &.is-disabled {
      :deep(.el-upload) {
        cursor: not-allowed;
        opacity: 0.5;
      }
    }
  }

  // 头像样式 100x100
  .image-avatar,
  .icon-avatar {
    width: 100px;
    height: 100px;
  }

  // 封面样式 400x300
  .image-cover,
  .icon-cover {
    width: 400px;
    height: 300px;
  }

  // 横幅样式 800x200
  .image-banner,
  .icon-banner {
    width: 800px;
    height: 200px;
  }

  // 自定义尺寸
  .image-custom {
    width: v-bind('props.width + "px"');
    height: v-bind('props.height + "px"');
  }

  .icon-custom {
    width: v-bind('props.width + "px"');
    height: v-bind('props.height + "px"');
    font-size: 28px;
    color: #8c939d;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .image-avatar,
  .image-cover,
  .image-banner,
  .image-custom {
    display: block;
    object-fit: cover;
  }

  .icon-avatar,
  .icon-cover,
  .icon-banner {
    font-size: 28px;
    color: #8c939d;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .upload-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #999;
    line-height: 1.5;
  }
}
</style>
