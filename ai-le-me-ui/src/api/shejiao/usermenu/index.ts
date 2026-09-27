import { shejiaoService } from '@/utils/request';
import { UserMenuVO, UserMenuQuery, UserMenuForm } from './types';

/**
 * 查询用户菜单管理列表
 */
export function listUserMenu(query: UserMenuQuery) {
  const params = {
    ...query,
    page: query.pageNum,
    limit: query.pageSize
  };
  return shejiaoService({
    url: '/admin/usermenu/list',
    method: 'get',
    params
  });
}

/**
 * 查询用户菜单管理详细
 */
export function getUserMenu(id: string | number) {
  return shejiaoService({
    url: '/admin/usermenu/info/' + id,
    method: 'get'
  });
}

/**
 * 新增用户菜单管理
 */
export function addUserMenu(data: UserMenuForm) {
  return shejiaoService({
    url: '/admin/usermenu/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改用户菜单管理
 */
export function updateUserMenu(data: UserMenuForm) {
  return shejiaoService({
    url: '/admin/usermenu/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除用户菜单管理
 */
export function delUserMenu(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/usermenu/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
