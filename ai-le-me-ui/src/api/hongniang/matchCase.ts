import { shejiaoService } from '@/utils/request';
import type { HongniangMatchCaseForm, HongniangMatchCaseQuery, HongniangMatchProgressForm } from './types';

export function listHongniangMatchCase(query: HongniangMatchCaseQuery) {
  return shejiaoService({
    url: '/admin/hongniang-match-case/list',
    method: 'get',
    params: query
  });
}

export function getHongniangMatchCase(id: number | string) {
  return shejiaoService({
    url: `/admin/hongniang-match-case/info/${id}`,
    method: 'get'
  });
}

export function createHongniangMatchCase(data: HongniangMatchCaseForm) {
  return shejiaoService({
    url: '/admin/hongniang-match-case/create',
    method: 'post',
    data
  });
}

export function updateHongniangMatchCase(data: HongniangMatchCaseForm) {
  return shejiaoService({
    url: '/admin/hongniang-match-case/update',
    method: 'put',
    data
  });
}

export function advanceHongniangMatchCase(data: {
  caseId: number | string;
  targetStage: number | string;
  content?: string;
  actualFollowTime?: string;
  nextFollowTime?: string;
  attachments?: string;
}) {
  return shejiaoService({
    url: '/admin/hongniang-match-case/advance-stage',
    method: 'post',
    data
  });
}

export function addHongniangMatchProgress(data: HongniangMatchProgressForm) {
  return shejiaoService({
    url: '/admin/hongniang-match-case/add-progress',
    method: 'post',
    data
  });
}

export function closeHongniangMatchCase(data: {
  caseId: number | string;
  closeReason: string;
  content?: string;
}) {
  return shejiaoService({
    url: '/admin/hongniang-match-case/close',
    method: 'post',
    data
  });
}

export function bindHongniangMatchCaseGroups(data: {
  caseId: number | string;
  groupIds: number[];
}) {
  return shejiaoService({
    url: '/admin/hongniang-match-case/bind-groups',
    method: 'post',
    data
  });
}

export function searchHongniangPoolUsers(params: {
  hongniangId: number | string;
  keyword?: string;
  gender?: number;
  limit?: number;
  excludeUserId?: number | string;
}) {
  return shejiaoService({
    url: '/admin/hongniang-match-case/pool-users',
    method: 'get',
    params
  });
}

export function searchHongniangMatchCaseSources(params: {
  hongniangId: number | string;
  sourceType: number;
  keyword?: string;
  limit?: number;
}) {
  return shejiaoService({
    url: '/admin/hongniang-match-case/source-candidates',
    method: 'get',
    params
  });
}

export function createHongniangMatchRequest(data: {
  caseId: number | string;
  fromUserId: number | string;
  toUserId: number | string;
  requestChannel?: number;
  requestMessage?: string;
  wechatShareSnapshot?: string;
  expireTime?: string;
}) {
  return shejiaoService({
    url: '/admin/hongniang-match-case/create-request',
    method: 'post',
    data
  });
}
