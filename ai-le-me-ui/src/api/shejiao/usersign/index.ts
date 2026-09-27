import { shejiaoService } from '@/utils/request';
import { UserSignVO, UserSignQuery, UserSignForm } from './types';

/**
 * 查询用户签到管理列表
 */
export function listUserSign(query: UserSignQuery) {
  return shejiaoService({
    url: '/admin/usersign/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询用户签到管理详细
 */
export function getUserSign(id: string | number) {
  return shejiaoService({
    url: '/admin/usersign/info/' + id,
    method: 'get'
  });
}

/**
 * 新增用户签到管理
 */
export function addUserSign(data: UserSignForm) {
  return shejiaoService({
    url: '/admin/usersign/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改用户签到管理
 */
export function updateUserSign(data: UserSignForm) {
  return shejiaoService({
    url: '/admin/usersign/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除用户签到管理
 */
export function delUserSign(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/usersign/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
