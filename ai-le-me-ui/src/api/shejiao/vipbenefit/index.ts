import { shejiaoService } from '@/utils/request';
import { VipBenefitVO, VipBenefitQuery, VipBenefitForm } from './types';

/**
 * 查询VIP权益管理列表
 */
export function listVipBenefit(query: VipBenefitQuery) {
  return shejiaoService({
    url: '/admin/vipbenefit/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询VIP权益管理详细
 */
export function getVipBenefit(id: string | number) {
  return shejiaoService({
    url: '/admin/vipbenefit/info/' + id,
    method: 'get'
  });
}

/**
 * 新增VIP权益管理
 */
export function addVipBenefit(data: VipBenefitForm) {
  return shejiaoService({
    url: '/admin/vipbenefit/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改VIP权益管理
 */
export function updateVipBenefit(data: VipBenefitForm) {
  return shejiaoService({
    url: '/admin/vipbenefit/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除VIP权益管理
 */
export function delVipBenefit(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/vipbenefit/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
