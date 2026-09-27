import { shejiaoService } from '@/utils/request';
import { ConfigForm, ConfigQuery, ConfigVO } from './types';

// 查询参数列表
export function listConfig(query: ConfigQuery) {
  return shejiaoService({
    url: '/admin/config/list',
    method: 'get',
    params: query
  });
}

// 查询参数详细
export function getConfig(configId: string | number) {
  return shejiaoService({
    url: '/admin/config/' + configId,
    method: 'get'
  });
}

// 根据参数键名查询参数值
export function getConfigKey(configKey: string) {
  return shejiaoService({
    url: '/admin/config/configKey/' + configKey,
    method: 'get'
  });
}

// 新增参数配置
export function addConfig(data: ConfigForm) {
  return shejiaoService({
    url: '/admin/config',
    method: 'post',
    data: data
  });
}

// 修改参数配置
export function updateConfig(data: ConfigForm) {
  return shejiaoService({
    url: '/admin/config',
    method: 'put',
    data: data
  });
}

// 批量更新参数配置
export function updateConfigBatch(data: any) {
  return shejiaoService({
    url: '/admin/config/updateBatch',
    method: 'post',
    data: data
  });
}

// 修改参数配置
export function updateConfigByKey(key: string, value: any) {
  return shejiaoService({
    url: '/admin/config/updateByKey',
    method: 'put',
    data: {
      configKey: key,
      configValue: value
    }
  });
}

// 删除参数配置
export function delConfig(configId: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/config/' + configId,
    method: 'delete'
  });
}

// 刷新参数缓存
export function refreshCache() {
  return shejiaoService({
    url: '/admin/config/refreshCache',
    method: 'delete'
  });
}
