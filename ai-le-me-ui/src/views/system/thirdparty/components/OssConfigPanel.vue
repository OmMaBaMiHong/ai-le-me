<template>
  <div class="oss-config-panel">
    <div class="panel-hero">
      <div>
        <div class="hero-title">云存储配置</div>
        <div class="hero-desc">上传、转存与资源回源统一走这里管理，当前 OSS provider 与配置项都已迁入第三方配置体系。</div>
      </div>
      <div class="hero-actions">
        <el-button plain @click="openOssManager()">文件管理</el-button>
        <el-button type="primary" plain @click="openOssManager('file')">上传文件</el-button>
        <el-button type="success" plain @click="openOssManager('image')">上传图片</el-button>
      </div>
    </div>

    <ServiceRuntimeConfigPanel
      service-type="oss"
      service-label="云存储"
      title="云存储配置"
      description="上传、转存、回源与访问域名统一从这里维护，文件管理入口保留在右上角。"
    />
  </div>
</template>

<script setup lang="ts">
import ServiceRuntimeConfigPanel from './ServiceRuntimeConfigPanel.vue';

const router = useRouter();

const openOssManager = (action?: 'file' | 'image') => {
  router.push({
    path: '/system/oss',
    query: action ? { action } : {}
  });
};
</script>

<style scoped lang="scss">
.oss-config-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border: 1px solid #ebeef5;
  border-radius: 18px;
  background: linear-gradient(135deg, #f4fff8 0%, #f6fbff 52%, #fffaf4 100%);
  gap: 16px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.hero-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.hero-desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: #6b7280;
}

@media (max-width: 960px) {
  .panel-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}
</style>
