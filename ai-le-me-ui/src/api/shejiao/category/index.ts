import { shejiaoService } from '@/utils/request';
import { CategoryVO, CategoryQuery, CategoryForm } from './types';

/**
 * 查询分类管理列表
 */
export function listCategory(query: CategoryQuery) {
  return shejiaoService({
    url: '/admin/category/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询分类管理详细
 */
export function getCategory(id: string | number) {
  return shejiaoService({
    url: '/admin/category/info/' + id,
    method: 'get'
  });
}

/**
 * 新增分类管理
 */
export function addCategory(data: CategoryForm) {
  return shejiaoService({
    url: '/admin/category/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改分类管理
 */
export function updateCategory(data: CategoryForm) {
  return shejiaoService({
    url: '/admin/category/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除分类管理
 */
export function delCategory(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/category/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
