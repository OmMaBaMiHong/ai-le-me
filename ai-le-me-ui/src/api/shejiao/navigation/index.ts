import { shejiaoService } from '@/utils/request';
import { NavigationVO, NavigationQuery, NavigationForm } from './types';

/**
 * 查询导航管理列表
 */
export function listNavigation(query: NavigationQuery) {
  return shejiaoService({
    url: '/admin/navigation/list',
    method: 'get',
    params: {
      page: query.pageNum,
      limit: query.pageSize,
      key: query.key
    }
  });
}

/**
 * 查询导航管理详细
 */
export function getNavigation(id: string | number) {
  return shejiaoService({
    url: '/admin/navigation/info/' + id,
    method: 'get'
  });
}

/**
 * 新增导航管理
 */
export function addNavigation(data: NavigationForm) {
  return shejiaoService({
    url: '/admin/navigation/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改导航管理
 */
export function updateNavigation(data: NavigationForm) {
  return shejiaoService({
    url: '/admin/navigation/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除导航管理
 */
export function delNavigation(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/navigation/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
