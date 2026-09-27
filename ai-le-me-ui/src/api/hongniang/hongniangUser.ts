import { shejiaoService } from '@/utils/request';
import { HongniangUserForm, HongniangUserQuery, HongniangUserVO } from './types';

// 查询红娘用户关系列表
export function listHongniangUser(query: HongniangUserQuery) {
  return shejiaoService({
    url: '/admin/hongniang-user/list',
    method: 'get',
    params: query
  });
}

// 查询红娘用户关系详细
export function getHongniangUser(id: number | string) {
  return shejiaoService({
    url: `/admin/hongniang-user/info/${id}`,
    method: 'get'
  });
}

// 新增红娘用户关系
export function addHongniangUser(data: HongniangUserForm) {
  return shejiaoService({
    url: '/admin/hongniang-user/add',
    method: 'post',
    data: data
  });
}

// 修改红娘用户关系
export function updateHongniangUser(data: HongniangUserForm) {
  return shejiaoService({
    url: '/admin/hongniang-user/update',
    method: 'put',
    data: data
  });
}

// 删除红娘用户关系
export function delHongniangUser(id: number | string) {
  return shejiaoService({
    url: `/admin/hongniang-user/delete`,
    method: 'post',
    data: [Number(id)]
  });
}

// 批量删除红娘用户关系
export function batchDelHongniangUser(ids: number[]) {
  return shejiaoService({
    url: '/admin/hongniang-user/delete',
    method: 'post',
    data: ids.map(id => Number(id))
  });
}

// 导出红娘用户关系
export function exportHongniangUser(query: HongniangUserQuery) {
  return shejiaoService({
    url: '/admin/hongniang-user/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  });
}

// 搜索分配弹窗用户
export function searchAssignableUsers(params?: {
  keyword?: string;
  hongniangId?: number | string;
  limit?: number;
}) {
  return shejiaoService({
    url: '/admin/hongniang-user/assignable-users',
    method: 'get',
    params
  });
}
