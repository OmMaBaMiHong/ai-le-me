import { shejiaoService } from '@/utils/request';
import type { HongniangWechatGroupForm, HongniangWechatGroupQuery, HongniangGroupTouchTaskForm } from './types';

export function listHongniangWechatGroup(query: HongniangWechatGroupQuery) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/list',
    method: 'get',
    params: query
  });
}

export function getHongniangWechatGroup(id: number | string) {
  return shejiaoService({
    url: `/admin/hongniang-wechat-group/info/${id}`,
    method: 'get'
  });
}

export function createHongniangWechatGroup(data: HongniangWechatGroupForm) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/create',
    method: 'post',
    data
  });
}

export function updateHongniangWechatGroup(data: HongniangWechatGroupForm) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/update',
    method: 'put',
    data
  });
}

export function bindHongniangWechatGroupUsers(data: {
  groupId: number | string;
  userIds: number[];
}) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/bind-users',
    method: 'post',
    data
  });
}

export function bindHongniangWechatGroupCases(data: {
  groupId: number | string;
  caseIds: number[];
}) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/bind-cases',
    method: 'post',
    data
  });
}

export function syncHongniangWechatGroup(groupId: number | string) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/sync',
    method: 'post',
    data: { groupId }
  });
}

export function createHongniangGroupTouchTask(data: HongniangGroupTouchTaskForm) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/touch-task/create',
    method: 'post',
    data
  });
}

export function listHongniangGroupTouchTask(params: {
  pageNum?: number;
  pageSize?: number;
  groupId?: number | string;
  hongniangId?: number | string;
  taskStatus?: number;
}) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/touch-task/list',
    method: 'get',
    params
  });
}

export function retryHongniangGroupTouchTask(taskId: number | string) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/touch-task/retry',
    method: 'post',
    data: { taskId }
  });
}

export function listHongniangGroupTouchLog(params: {
  pageNum?: number;
  pageSize?: number;
  taskId?: number | string;
  groupId?: number | string;
}) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/touch-log/list',
    method: 'get',
    params
  });
}

export function listHongniangGroupTaskExecution(params: {
  taskId?: number | string;
  groupId?: number | string;
  limit?: number;
}) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/touch-execution/list',
    method: 'get',
    params
  });
}

export function searchBindableWechatGroupUsers(params: {
  hongniangId: number | string;
  groupId?: number | string;
  keyword?: string;
  limit?: number;
}) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/bindable-users',
    method: 'get',
    params
  });
}

export function searchBindableWechatGroupCases(params: {
  hongniangId: number | string;
  groupId?: number | string;
  keyword?: string;
  limit?: number;
}) {
  return shejiaoService({
    url: '/admin/hongniang-wechat-group/bindable-cases',
    method: 'get',
    params
  });
}
