<template>
  <div class="app-container">
    <el-page-header @back="goBack" content="第三方服务配置" />

    <div class="page-intro">
      <div>
        <div class="intro-title">第三方渠道统一入口</div>
        <div class="intro-desc">所有服务按数据库真实 serviceType 全量展示成一组导航 tab，provider、配置项和路由规则都从这里直接进入。</div>
      </div>
      <el-tag type="success" effect="dark" round>统一 CRUD</el-tag>
    </div>

    <div class="service-switchboard">
      <div class="track-label">服务导航</div>
      <div class="pill-list">
        <button
          v-for="item in serviceTabs"
          :key="item"
          type="button"
          :class="['service-pill', { active: activeService === item, route: item === routeTabName }]"
          @click="activeService = item"
        >
          {{ serviceLabel(item) }}
        </button>
      </div>
    </div>

    <div class="service-panel-shell">
      <component
        v-if="activeService !== routeTabName && activeService !== runtimeTraceTabName"
        :key="`service-panel-${activeService}`"
        :is="resolvePanel(activeService)"
        v-bind="resolvePanelProps(activeService)"
      />
      <RuntimeTracePanel
        v-else-if="activeService === runtimeTraceTabName"
        :key="`trace-panel-${activeService}`"
      />
      <RouteRulePanel
        v-else
        :key="`route-panel-${activeService}`"
        class="route-rule-panel-shell"
        :service-types="routeRuleServiceTypes"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import type { Component } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { listServiceTypes } from '@/api/system/thirdparty';
import VideoConfigPanel from './components/VideoConfigPanel.vue';
import OssConfigPanel from './components/OssConfigPanel.vue';
import PaymentConfigPanel from './components/PaymentConfigPanel.vue';
import ServiceRuntimeConfigPanel from './components/ServiceRuntimeConfigPanel.vue';
import RouteRulePanel from './components/RouteRulePanel.vue';
import RuntimeTracePanel from './components/RuntimeTracePanel.vue';

const router = useRouter();
const route = useRoute();
const serviceTypes = ref<string[]>([]);
const routeTabName = 'route-rules';
const runtimeTraceTabName = 'runtime-trace';
const activeService = ref('video');
const tabsReady = ref(false);

const panelMap: Record<string, Component> = {
  video: VideoConfigPanel,
  oss: OssConfigPanel,
  payment: PaymentConfigPanel
};

const preferredOrder = [
  'video',
  'ai',
  'image',
  'payment',
  'oss',
  'sms',
  'realname',
  'education',
  'wechat_mini',
  'wechat_mp',
  'wechat_app',
  'wecom_customer',
  'scrm_vendor',
  'push',
  'map'
];

const orderedServiceTypes = computed(() => {
  const priority = new Map(preferredOrder.map((item, index) => [item, index]));
  return [...serviceTypes.value].sort((left, right) => {
    const leftPriority = priority.get(left);
    const rightPriority = priority.get(right);
    if (leftPriority !== undefined || rightPriority !== undefined) {
      return (leftPriority ?? Number.MAX_SAFE_INTEGER) - (rightPriority ?? Number.MAX_SAFE_INTEGER);
    }
    return left.localeCompare(right);
  });
});

const serviceTabs = computed(() => [...orderedServiceTypes.value, runtimeTraceTabName, routeTabName]);
const routeRuleServiceTypes = computed(() => orderedServiceTypes.value.filter((item) => ['ai', 'image', 'video'].includes(item)));

const serviceLabel = (serviceType: string) => {
  const labelMap: Record<string, string> = {
    ai: 'AI 对话',
    image: '图片生成',
    video: 'AI 视频',
    oss: '云存储',
    sms: '短信服务',
    payment: '支付服务',
    realname: '实名认证',
    education: '学历认证',
    push: '推送服务',
    map: '地图服务',
    wechat_mini: '微信小程序',
    wechat_mp: '微信公众号',
    wechat_app: '微信开放平台',
    wecom_customer: '企业微信客户群',
    scrm_vendor: 'SCRM Bridge',
    [runtimeTraceTabName]: 'Runtime Trace',
    [routeTabName]: '路由规则',
    social_gitea: 'Gitea 登录',
    social_maxkey: 'MaxKey 登录',
    social_topiam: 'TopIAM 登录'
  };
  return labelMap[serviceType] || serviceType;
};

const serviceIntroMap: Record<string, string> = {
  ai: '推理模型、提示词生成模型与多模态推理统一在这里维护。',
  image: '图片生成模型、尺寸参数与默认执行渠道都从这里维护。',
  sms: '验证码、通知短信等发送渠道统一从这里切换并维护密钥。',
  realname: '实名、资质、校验类接口统一从这里维护渠道参数。',
  education: '学历认证的 provider、密钥与运行参数统一在这里维护。',
  wechat_mini: '小程序 AppId、Secret、商户与消息能力统一从这里维护。',
  wechat_mp: '公众号 AppId、Secret、模板消息与网页授权统一在这里维护。',
  wechat_app: '微信开放平台 AppId、Secret 与回调参数统一在这里维护。',
  wecom_customer: '企业微信客户群同步、群发任务与客户联系凭证统一从这里维护。',
  scrm_vendor: 'SCRM Bridge 的基础地址、鉴权参数与厂商标识统一在这里维护。',
  push: '推送服务的 AppKey、MasterSecret 与平台配置统一在这里维护。',
  map: '地图服务的 Key、坐标服务与地理编码参数统一在这里维护。'
};

const resolvePanel = (serviceType: string) => panelMap[serviceType] || ServiceRuntimeConfigPanel;

const resolvePanelProps = (serviceType: string) => {
  if (panelMap[serviceType]) {
    return {};
  }
  return {
    serviceType,
    serviceLabel: serviceLabel(serviceType),
    title: `${serviceLabel(serviceType)}配置`,
    description: serviceIntroMap[serviceType] || `${serviceLabel(serviceType)} 的 provider、密钥与运行参数统一在这里维护。`
  };
};

const goBack = () => {
  router.back();
};

const loadTabMeta = async () => {
  const response: any = await listServiceTypes();
  serviceTypes.value = response.data || [];
  const defaultTab = orderedServiceTypes.value[0] || routeTabName;
  const routeTab = typeof route.query.tab === 'string' ? route.query.tab : defaultTab;
  if (serviceTabs.value.includes(routeTab)) {
    activeService.value = routeTab;
  } else if (!serviceTabs.value.includes(activeService.value)) {
    activeService.value = defaultTab;
  }
  tabsReady.value = true;
};

watch(
  activeService,
  (value) => {
    if (!tabsReady.value) {
      return;
    }
    const nextQuery = { ...route.query, tab: value };
    router.replace({ query: nextQuery });
  },
  { immediate: true }
);

watch(
  () => route.query.tab,
  (value) => {
    if (typeof value === 'string' && serviceTabs.value.includes(value) && value !== activeService.value) {
      activeService.value = value;
    }
  }
);

onMounted(loadTabMeta);
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;

  .page-intro {
    margin-top: 18px;
    padding: 20px 22px;
    border-radius: 22px;
    border: 1px solid rgba(255, 255, 255, 0.9);
    background:
      radial-gradient(circle at top right, rgba(100, 181, 246, 0.18), transparent 34%),
      linear-gradient(135deg, #fff9f1 0%, #f7fbff 48%, #f5fff7 100%);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;

    .intro-title {
      font-size: 18px;
      font-weight: 600;
      color: #111827;
    }

    .intro-desc {
      margin-top: 8px;
      font-size: 13px;
      line-height: 1.7;
      color: #5b6475;
    }
  }

  .service-switchboard {
    margin-top: 20px;
    padding: 18px;
    border-radius: 24px;
    border: 1px solid #e6eef7;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
    box-shadow: 0 16px 40px rgba(15, 23, 42, 0.05);
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .track-label {
    font-size: 12px;
    font-weight: 600;
    color: #64748b;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }

  .pill-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  .service-pill {
    padding: 10px 18px;
    border: 0;
    border-radius: 999px;
    background: linear-gradient(135deg, #eef4ff, #f7fbff);
    color: #475569;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.18s ease;
  }

  .service-pill:hover {
    transform: translateY(-1px);
    color: #0f172a;
    box-shadow: 0 10px 24px rgba(59, 130, 246, 0.12);
  }

  .service-pill.active {
    color: #fff;
    background: linear-gradient(135deg, #111827, #1f8f6b);
    box-shadow: 0 12px 30px rgba(17, 24, 39, 0.18);
  }

  .service-pill.route {
    background: linear-gradient(135deg, #fff7ed, #fff1f2);
    color: #9a3412;
  }

  .service-panel-shell {
    margin-top: 18px;
  }
}
</style>
