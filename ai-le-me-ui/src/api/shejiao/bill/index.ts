import { shejiaoService } from '@/utils/request';
import { BillVO, BillQuery, BillForm } from './types';

/**
 * 查询账单管理列表
 */
export function listBill(query: BillQuery) {
  const { pageNum, pageSize, ...rest } = query || {};
  return shejiaoService({
    url: '/admin/bill/list',
    method: 'get',
    params: {
      page: pageNum,
      limit: pageSize,
      ...rest
    }
  });
}

/**
 * 查询账单管理详细
 */
export function getBill(id: string | number) {
  return shejiaoService({
    url: '/admin/bill/info/' + id,
    method: 'get'
  });
}

/**
 * 新增账单管理
 */
export function addBill(data: BillForm) {
  return shejiaoService({
    url: '/admin/bill/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改账单管理
 */
export function updateBill(data: BillForm) {
  return shejiaoService({
    url: '/admin/bill/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除账单管理
 */
export function delBill(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/bill/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
