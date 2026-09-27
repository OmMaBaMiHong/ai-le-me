import { shejiaoService } from '@/utils/request';
import { XiangqinActivityVO, XiangqinActivityQuery, XiangqinActivityForm, XiangqinEnrollmentVO } from './types';

/**
 * 查询相亲活动管理列表
 */
export function listXiangqinActivity(query: XiangqinActivityQuery) {
  return shejiaoService({
    url: '/admin/xiangqin/activity/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询相亲活动管理详细
 */
export function getXiangqinActivity(id: string | number) {
  return shejiaoService({
    url: '/admin/xiangqin/activity/info/' + id,
    method: 'get'
  });
}

/**
 * 新增相亲活动管理
 */
export function addXiangqinActivity(data: XiangqinActivityForm) {
  return shejiaoService({
    url: '/admin/xiangqin/activity/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改相亲活动管理
 */
export function updateXiangqinActivity(data: XiangqinActivityForm) {
  return shejiaoService({
    url: '/admin/xiangqin/activity/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除相亲活动管理
 */
export function delXiangqinActivity(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/xiangqin/activity/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}

/**
 * 获取活动报名列表
 */
export function listXiangqinEnrollment(activityId: string | number) {
  return shejiaoService({
    url: '/admin/xiangqin/enrollment/list',
    method: 'get',
    params: { activityId }
  });
}

/**
 * 审核报名
 */
export function auditXiangqinEnrollment(enrollmentId: string | number, auditStatus: number) {
  return shejiaoService({
    url: '/admin/xiangqin/enrollment/audit',
    method: 'post',
    params: { enrollmentId, status: auditStatus }
  });
}
