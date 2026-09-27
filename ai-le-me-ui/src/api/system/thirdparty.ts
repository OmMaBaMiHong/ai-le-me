import request from '@/utils/request';

export interface ThirdPartyProvider {
  providerId?: number;
  serviceType: string;
  providerCode: string;
  providerName: string;
  providerLogo?: string;
  isCurrent?: number;
  isEnabled?: number;
  displayOrder?: number;
  officialWebsite?: string;
  docUrl?: string;
  remark?: string;
  configJson?: string;
}

export interface ThirdPartyConfigItem {
  configId?: number;
  serviceType: string;
  provider: string;
  configKey: string;
  configValue?: string;
  defaultValue?: string;
  valueType?: string;
  selectOptions?: string;
  isRequired?: number;
  isSensitive?: number;
  isEnabled?: number;
  displayOrder?: number;
  configLabel?: string;
  helpText?: string;
  remark?: string;
}

export interface ThirdPartyRouteRule {
  routeRuleId?: number;
  serviceType: string;
  sceneCode?: string;
  templateCode?: string;
  contentMode?: string;
  functionType?: string;
  providerCode: string;
  profileCode?: string;
  matchJson?: string;
  priority?: number;
  isEnabled?: number;
  remark?: string;
}

export interface VideoRuntimeOverview {
  quartzRuntime?: {
    exists: boolean;
    enabled: boolean;
    jobCode: string;
  };
  enabled: boolean;
  defaultProviderCode: string;
  currentProviderCode: string;
  currentProviderName: string;
  allowMockFallback: boolean;
  pollingMode: string;
  pollingIntervalMs: number;
  pollingInitialDelayMs: number;
  pollingTimeoutMinutes: number;
  springSchedulerActive: boolean;
  recommendedProviderCode: string;
  providerAdvice: Record<string, string>;
}

export interface RuntimeTraceListItem {
  trace_id: string;
  agent_type: string;
  scene_code: string;
  function_type: string;
  owner_user_id?: number;
  target_user_id?: number;
  model_provider: string;
  model_profile: string;
  status: string;
  request_summary: string;
  response_summary: string;
  reasoning_summary: string;
  error_message: string;
  created_at: string;
}

export interface RuntimeTracePageResponse {
  total: number;
  page: number;
  page_size: number;
  items: RuntimeTraceListItem[];
}

export interface RuntimeTraceDetail extends RuntimeTraceListItem {
  retrieval_hits: string[];
  graph_facts: string[];
  timing_breakdown: Record<string, number>;
  request_json: Record<string, any>;
  response_json: Record<string, any>;
}

/**
 * 获取AI配置
 */
export function getAIConfig() {
  return request({
    url: '/system/thirdparty/ai/config',
    method: 'get'
  });
}

/**
 * 获取OSS配置
 */
export function getOSSConfig() {
  return request({
    url: '/system/thirdparty/oss/config',
    method: 'get'
  });
}

/**
 * 获取短信配置
 */
export function getSMSConfig() {
  return request({
    url: '/system/thirdparty/sms/config',
    method: 'get'
  });
}

/**
 * 获取支付配置
 */
export function getPaymentConfig() {
  return request({
    url: '/system/thirdparty/payment/config',
    method: 'get'
  });
}

/**
 * 获取指定服务配置
 */
export function getServiceConfig(serviceType: string) {
  return request({
    url: `/system/thirdparty/configs/${serviceType}`,
    method: 'get'
  });
}

/**
 * 切换提供商
 */
export function switchProvider(serviceType: string, data: { providerCode: string }) {
  return request({
    url: `/system/thirdparty/providers/${serviceType}/switch`,
    method: 'put',
    data
  });
}

/**
 * 更新单个配置
 */
export function updateConfig(configId: number, data: { configValue: string }) {
  return request({
    url: `/system/thirdparty/configs/${configId}`,
    method: 'put',
    data
  });
}

/**
 * 新增配置项
 */
export function addConfig(data: any) {
  return request({
    url: '/system/thirdparty/configs',
    method: 'post',
    data
  });
}

/**
 * 删除配置项
 */
export function deleteConfig(configId: number) {
  return request({
    url: `/system/thirdparty/configs/${configId}`,
    method: 'delete'
  });
}

/**
 * 批量更新配置
 */
export function updateConfigBatch(data: any[]) {
  return request({
    url: '/system/thirdparty/configs/batch',
    method: 'put',
    data
  });
}

/**
 * 测试连接
 */
export function testConnection(serviceType: string, provider: string, configs: Record<string, string>) {
  return request({
    url: `/system/thirdparty/test/${serviceType}/${provider}`,
    method: 'post',
    data: configs
  });
}

/**
 * 获取 AI 视频运行态概览
 */
export function getVideoRuntimeOverview() {
  return request<VideoRuntimeOverview>({
    url: '/system/thirdparty/video/runtime',
    method: 'get'
  });
}

/**
 * 获取所有服务类型
 */
export function listServiceTypes() {
  return request({
    url: '/system/thirdparty/serviceTypes',
    method: 'get'
  });
}

/**
 * 获取指定服务类型下的提供商列表
 */
export function listProviders(serviceType: string) {
  return request({
    url: `/system/thirdparty/providers/${serviceType}`,
    method: 'get'
  });
}

/**
 * 新增服务提供商
 */
export function addProvider(serviceType: string, data: any) {
  return request({
    url: `/system/thirdparty/providers/${serviceType}`,
    method: 'post',
    data
  });
}

/**
 * 更新服务提供商
 */
export function updateProvider(providerId: number, data: Partial<ThirdPartyProvider>) {
  return request({
    url: `/system/thirdparty/providers/${providerId}`,
    method: 'put',
    data
  });
}

/**
 * 删除服务提供商
 */
export function deleteProvider(providerId: number) {
  return request({
    url: `/system/thirdparty/providers/${providerId}`,
    method: 'delete'
  });
}

/**
 * 获取路由规则列表
 */
export function listRouteRules(serviceType?: string) {
  return request({
    url: '/system/thirdparty/route-rules',
    method: 'get',
    params: { serviceType }
  });
}

/**
 * 新增路由规则
 */
export function addRouteRule(data: ThirdPartyRouteRule) {
  return request({
    url: '/system/thirdparty/route-rules',
    method: 'post',
    data
  });
}

/**
 * 更新路由规则
 */
export function updateRouteRule(routeRuleId: number, data: ThirdPartyRouteRule) {
  return request({
    url: `/system/thirdparty/route-rules/${routeRuleId}`,
    method: 'put',
    data
  });
}

/**
 * 删除路由规则
 */
export function deleteRouteRule(routeRuleId: number) {
  return request({
    url: `/system/thirdparty/route-rules/${routeRuleId}`,
    method: 'delete'
  });
}

/**
 * 新增服务提供商并批量创建配置
 */
export function addProviderWithConfigs(serviceType: string, data: any) {
  return request({
    url: `/system/thirdparty/providers/${serviceType}/with-configs`,
    method: 'post',
    data
  });
}

export function listRuntimeTraces(params: {
  agentType?: string;
  ownerUserId?: number;
  targetUserId?: number;
  providerCode?: string;
  traceId?: string;
  keyword?: string;
  page?: number;
  pageSize?: number;
}) {
  return request<RuntimeTracePageResponse>({
    url: '/system/thirdparty/runtime/traces',
    method: 'get',
    params
  });
}

export function getRuntimeTraceDetail(traceId: string) {
  return request<RuntimeTraceDetail>({
    url: `/system/thirdparty/runtime/traces/${traceId}`,
    method: 'get'
  });
}
