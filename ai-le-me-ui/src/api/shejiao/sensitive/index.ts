import { shejiaoService } from '@/utils/request';
import { SensitiveVO, SensitiveQuery, SensitiveForm } from './types';

/**
 * 查询敏感词管理列表
 */
export function listSensitive(query: SensitiveQuery) {
  return shejiaoService({
    url: '/admin/sensitive/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询敏感词管理详细
 */
export function getSensitive(id: string | number) {
  return shejiaoService({
    url: '/admin/sensitive/info/' + id,
    method: 'get'
  });
}

/**
 * 新增敏感词管理
 */
export function addSensitive(data: SensitiveForm) {
  return shejiaoService({
    url: '/admin/sensitive/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改敏感词管理
 */
export function updateSensitive(data: SensitiveForm) {
  return shejiaoService({
    url: '/admin/sensitive/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除敏感词管理
 */
export function delSensitive(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/sensitive/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
