import { shejiaoService } from '@/utils/request';
import { HongniangVO, HongniangQuery, HongniangForm } from './types';

/**
 * 查询红娘管理列表
 */
export function listHongniang(query: HongniangQuery) {
  return shejiaoService({
    url: '/admin/hongniang/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询红娘管理详细
 */
export function getHongniang(id: string | number) {
  return shejiaoService({
    url: '/admin/hongniang/info/' + id,
    method: 'get'
  });
}

/**
 * 新增红娘管理
 */
export function addHongniang(data: HongniangForm) {
  return shejiaoService({
    url: '/admin/hongniang/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改红娘管理
 */
export function updateHongniang(data: HongniangForm) {
  return shejiaoService({
    url: '/admin/hongniang/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除红娘管理
 */
export function delHongniang(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/hongniang/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
