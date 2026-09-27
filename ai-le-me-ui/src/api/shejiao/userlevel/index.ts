import { shejiaoService } from '@/utils/request';
import { UserLevelVO, UserLevelQuery, UserLevelForm } from './types';

/**
 * 查询用户等级管理列表
 */
export function listUserLevel(query: UserLevelQuery) {
  return shejiaoService({
    url: '/admin/userlevel/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询用户等级管理详细
 */
export function getUserLevel(id: string | number) {
  return shejiaoService({
    url: '/admin/userlevel/info/' + id,
    method: 'get'
  });
}

/**
 * 新增用户等级管理
 */
export function addUserLevel(data: UserLevelForm) {
  return shejiaoService({
    url: '/admin/userlevel/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改用户等级管理
 */
export function updateUserLevel(data: UserLevelForm) {
  return shejiaoService({
    url: '/admin/userlevel/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除用户等级管理
 */
export function delUserLevel(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/userlevel/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
