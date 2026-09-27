import { shejiaoService } from '@/utils/request';
import { UserRechargeVO, UserRechargeQuery, UserRechargeForm, UserRechargeRefundForm } from './types';

/**
 * 查询用户充值管理列表
 */
export function listUserRecharge(query: UserRechargeQuery) {
  const { pageNum, pageSize, ...rest } = query || {};
  return shejiaoService({
    url: '/admin/userrecharge/list',
    method: 'get',
    params: {
      page: pageNum,
      limit: pageSize,
      ...rest
    }
  });
}

/**
 * 查询用户充值管理详细
 */
export function getUserRecharge(id: string | number) {
  return shejiaoService({
    url: '/admin/userrecharge/info/' + id,
    method: 'get'
  });
}

/**
 * 新增用户充值管理
 */
export function addUserRecharge(data: UserRechargeForm) {
  return shejiaoService({
    url: '/admin/userrecharge/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改用户充值管理
 */
export function updateUserRecharge(data: UserRechargeForm) {
  return shejiaoService({
    url: '/admin/userrecharge/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除用户充值管理
 */
export function delUserRecharge(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/userrecharge/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}

export function refundUserRecharge(data: UserRechargeRefundForm) {
  return shejiaoService({
    url: '/admin/userrecharge/refund',
    method: 'post',
    data
  });
}

export function listUserRechargeRefund(orderId?: string) {
  return shejiaoService({
    url: '/admin/userrecharge/refundList',
    method: 'get',
    params: { orderId }
  });
}
