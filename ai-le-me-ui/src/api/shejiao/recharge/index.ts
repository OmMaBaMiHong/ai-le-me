import { shejiaoService } from '@/utils/request';
import { RechargeVO, RechargeQuery, RechargeForm } from './types';

/**
 * 查询充值方案列表
 */
export function listRecharge(query: RechargeQuery) {
  return shejiaoService({
    url: '/admin/recharge/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询充值方案详细
 */
export function getRecharge(id: string | number) {
  return shejiaoService({
    url: '/admin/recharge/info/' + id,
    method: 'get'
  });
}

/**
 * 新增充值方案
 */
export function addRecharge(data: RechargeForm) {
  return shejiaoService({
    url: '/admin/recharge/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改充值方案
 */
export function updateRecharge(data: RechargeForm) {
  return shejiaoService({
    url: '/admin/recharge/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除充值方案
 */
export function delRecharge(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/recharge/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
