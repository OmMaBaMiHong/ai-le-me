import { shejiaoService } from '@/utils/request';
import { LuckdrawVO, LuckdrawQuery, LuckdrawForm } from './types';

/**
 * 查询抽奖活动管理列表
 */
export function listLuckdraw(query: LuckdrawQuery) {
  return shejiaoService({
    url: '/admin/luckdraw/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询抽奖活动管理详细
 */
export function getLuckdraw(id: string | number) {
  return shejiaoService({
    url: '/admin/luckdraw/info/' + id,
    method: 'get'
  });
}

/**
 * 新增抽奖活动管理
 */
export function addLuckdraw(data: LuckdrawForm) {
  return shejiaoService({
    url: '/admin/luckdraw/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改抽奖活动管理
 */
export function updateLuckdraw(data: LuckdrawForm) {
  return shejiaoService({
    url: '/admin/luckdraw/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除抽奖活动管理
 */
export function delLuckdraw(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/luckdraw/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
