import { shejiaoService } from '@/utils/request';
import { LinkVO, LinkQuery, LinkForm } from './types';

/**
 * 查询链接管理列表
 */
export function listLink(query: LinkQuery) {
  return shejiaoService({
    url: '/admin/link/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询链接管理详细
 */
export function getLink(id: string | number) {
  return shejiaoService({
    url: '/admin/link/info/' + id,
    method: 'get'
  });
}

/**
 * 新增链接管理
 */
export function addLink(data: LinkForm) {
  return shejiaoService({
    url: '/admin/link/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改链接管理
 */
export function updateLink(data: LinkForm) {
  return shejiaoService({
    url: '/admin/link/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除链接管理
 */
export function delLink(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/link/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
