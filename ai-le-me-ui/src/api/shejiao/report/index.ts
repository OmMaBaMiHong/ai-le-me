import { shejiaoService } from '@/utils/request';
import { ReportVO, ReportQuery, ReportForm } from './types';

/**
 * 查询举报管理列表
 */
export function listReport(query: ReportQuery) {
  return shejiaoService({
    url: '/admin/report/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询举报管理详细
 */
export function getReport(id: string | number) {
  return shejiaoService({
    url: '/admin/report/info/' + id,
    method: 'get'
  });
}

/**
 * 新增举报管理
 */
export function addReport(data: ReportForm) {
  return shejiaoService({
    url: '/admin/report/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改举报管理
 */
export function updateReport(data: ReportForm) {
  return shejiaoService({
    url: '/admin/report/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除举报管理
 */
export function delReport(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/report/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
