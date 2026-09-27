<template>
  <ServiceRuntimeConfigPanel
    service-type="sms"
    service-label="短信服务"
    title="短信服务配置"
    description="登录验证码、通知短信与业务发送统一从这里维护，保存后直接作用到当前发送渠道。"
  />
</template>

<script setup lang="ts">
import ServiceRuntimeConfigPanel from './ServiceRuntimeConfigPanel.vue';
</script>
