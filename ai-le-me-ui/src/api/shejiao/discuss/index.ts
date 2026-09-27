import { shejiaoService } from '@/utils/request';
import { DiscussVO, DiscussQuery, DiscussForm } from './types';

/**
 * 查询讨论管理列表
 */
export function listDiscuss(query: DiscussQuery) {
  return shejiaoService({
    url: '/admin/discuss/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询讨论管理详细
 */
export function getDiscuss(id: string | number) {
  return shejiaoService({
    url: '/admin/discuss/info/' + id,
    method: 'get'
  });
}

/**
 * 新增讨论管理
 */
export function addDiscuss(data: DiscussForm) {
  return shejiaoService({
    url: '/admin/discuss/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改讨论管理
 */
export function updateDiscuss(data: DiscussForm) {
  return shejiaoService({
    url: '/admin/discuss/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除讨论管理
 */
export function delDiscuss(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/discuss/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
