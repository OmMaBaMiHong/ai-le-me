import { shejiaoService } from '@/utils/request';
import { CashOutVO, CashOutQuery, CashOutForm } from './types';

/**
 * 查询提现管理列表
 */
export function listCashOut(query: CashOutQuery) {
  return shejiaoService({
    url: '/admin/cashout/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询提现管理详细
 */
export function getCashOut(id: string | number) {
  return shejiaoService({
    url: '/admin/cashout/info/' + id,
    method: 'get'
  });
}

/**
 * 新增提现管理
 */
export function addCashOut(data: CashOutForm) {
  return shejiaoService({
    url: '/admin/cashout/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改提现管理
 */
export function updateCashOut(data: CashOutForm) {
  return shejiaoService({
    url: '/admin/cashout/update',
    method: 'post',
    data: data
  });
}

// 兼容旧页面中遗留的命名方式，避免后台构建被大小写差异卡住。
export const getCashout = getCashOut;
export const addCashout = addCashOut;
export const updateCashout = updateCashOut;
export const listCashout = listCashOut;

/**
 * 删除提现管理
 */
export function delCashOut(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/cashout/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}

export const delCashout = delCashOut;
