import { shejiaoService } from '@/utils/request';
import { SignConfigVO, SignConfigQuery, SignConfigForm } from './types';

/**
 * 查询签到配置管理列表
 */
export function listSignConfig(query: SignConfigQuery) {
  return shejiaoService({
    url: '/admin/signconfig/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询签到配置管理详细
 */
export function getSignConfig(id: string | number) {
  return shejiaoService({
    url: '/admin/signconfig/info/' + id,
    method: 'get'
  });
}

/**
 * 新增签到配置管理
 */
export function addSignConfig(data: SignConfigForm) {
  return shejiaoService({
    url: '/admin/signconfig/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改签到配置管理
 */
export function updateSignConfig(data: SignConfigForm) {
  return shejiaoService({
    url: '/admin/signconfig/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除签到配置管理
 */
export function delSignConfig(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/signconfig/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
