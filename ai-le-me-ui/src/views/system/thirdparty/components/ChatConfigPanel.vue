<template>
  <div class="chat-config-panel">
    <el-alert
      title="AI对话服务配置"
      type="info"
      :closable="false"
      show-icon
    >
      <template #default>
        配置AI对话模型服务（通义千问、智谱GLM、GPT等），用于智能对话、画像生成等功能
      </template>
    </el-alert>
    
    <el-empty description="功能开发中，敬请期待" :image-size="200" />
  </div>
</template>

<script setup lang="ts">
// AI对话服务配置面板
</script>

<style scoped>
.chat-config-panel {
  padding: 20px;
}
</style>
