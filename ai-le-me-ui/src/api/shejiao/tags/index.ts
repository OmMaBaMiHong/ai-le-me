import { shejiaoService } from '@/utils/request';
import { TagsVO, TagsQuery, TagsForm } from './types';

/**
 * 查询标签管理列表
 */
export function listTags(query: TagsQuery) {
  return shejiaoService({
    url: '/admin/tags/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询标签管理详细
 */
export function getTags(id: string | number) {
  return shejiaoService({
    url: '/admin/tags/info/' + id,
    method: 'get'
  });
}

/**
 * 新增标签管理
 */
export function addTags(data: TagsForm) {
  return shejiaoService({
    url: '/admin/tags/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改标签管理
 */
export function updateTags(data: TagsForm) {
  return shejiaoService({
    url: '/admin/tags/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除标签管理
 */
export function delTags(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/tags/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
