import { shejiaoService } from '@/utils/request';
import { UserVO, UserQuery, UserForm } from './types';

/**
 * 查询用户管理列表
 */
export function listUser(query: UserQuery) {
  return shejiaoService({
    url: '/admin/user/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询用户管理详细
 */
export function getUser(id: string | number) {
  return shejiaoService({
    url: '/admin/user/info/' + id,
    method: 'get'
  });
}

/**
 * 根据ID获取用户详情（别名方法）
 */
export function getUserById(userId: string | number) {
  return getUser(userId);
}

/**
 * 获取所有用户列表（用于下拉选择等场景）
 */
export function getAllUsers(query?: Partial<UserQuery>) {
  return shejiaoService({
    url: '/admin/user/list',
    method: 'get',
    params: {
      pageNum: 1,
      pageSize: 9999,
      ...query
    }
  });
}

/**
 * 新增用户管理
 */
export function addUser(data: UserForm) {
  return shejiaoService({
    url: '/admin/user/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改用户管理
 */
export function updateUser(data: UserForm) {
  return shejiaoService({
    url: '/admin/user/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除用户管理
 */
export function delUser(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/user/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
