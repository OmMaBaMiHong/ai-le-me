import { shejiaoService } from '@/utils/request';
import { LuckdrawRecordVO, LuckdrawRecordQuery, LuckdrawRecordForm } from './types';

/**
 * 查询抽奖记录管理列表
 */
export function listLuckdrawRecord(query: LuckdrawRecordQuery) {
  return shejiaoService({
    url: '/admin/luckdrawrecord/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询抽奖记录管理详细
 */
export function getLuckdrawRecord(id: string | number) {
  return shejiaoService({
    url: '/admin/luckdrawrecord/info/' + id,
    method: 'get'
  });
}

/**
 * 新增抽奖记录管理
 */
export function addLuckdrawRecord(data: LuckdrawRecordForm) {
  return shejiaoService({
    url: '/admin/luckdrawrecord/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改抽奖记录管理
 */
export function updateLuckdrawRecord(data: LuckdrawRecordForm) {
  return shejiaoService({
    url: '/admin/luckdrawrecord/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除抽奖记录管理
 */
export function delLuckdrawRecord(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/luckdrawrecord/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
