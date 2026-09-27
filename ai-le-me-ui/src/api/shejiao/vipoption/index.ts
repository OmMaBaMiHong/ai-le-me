import { shejiaoService } from '@/utils/request';
import { VipOptionVO, VipOptionQuery, VipOptionForm } from './types';

/**
 * 查询VIP选项管理列表
 */
export function listVipOption(query: VipOptionQuery) {
  return shejiaoService({
    url: '/admin/vipoption/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询VIP选项管理详细
 */
export function getVipOption(id: string | number) {
  return shejiaoService({
    url: '/admin/vipoption/info/' + id,
    method: 'get'
  });
}

/**
 * 新增VIP选项管理
 */
export function addVipOption(data: VipOptionForm) {
  return shejiaoService({
    url: '/admin/vipoption/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改VIP选项管理
 */
export function updateVipOption(data: VipOptionForm) {
  return shejiaoService({
    url: '/admin/vipoption/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除VIP选项管理
 */
export function delVipOption(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/vipoption/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
