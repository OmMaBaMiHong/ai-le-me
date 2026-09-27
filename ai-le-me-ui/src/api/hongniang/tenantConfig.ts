import request from '@/utils/request';
import { TenantConfigForm, TenantConfigQuery, TenantConfigVO } from './types';

// 查询租户配置列表
export function listTenantConfig(query: TenantConfigQuery) {
  return request({
    url: '/hongniang/tenantConfig/list',
    method: 'get',
    params: query
  });
}

// 查询租户配置详细
export function getTenantConfig(id: number | string) {
  return request({
    url: `/hongniang/tenantConfig/${id}`,
    method: 'get'
  });
}

// 新增租户配置
export function addTenantConfig(data: TenantConfigForm) {
  return request({
    url: '/hongniang/tenantConfig',
    method: 'post',
    data: data
  });
}

// 修改租户配置
export function updateTenantConfig(data: TenantConfigForm) {
  return request({
    url: '/hongniang/tenantConfig',
    method: 'put',
    data: data
  });
}

// 删除租户配置
export function delTenantConfig(id: number | string) {
  return request({
    url: `/hongniang/tenantConfig/${id}`,
    method: 'delete'
  });
}

// 批量删除租户配置
export function batchDelTenantConfig(ids: number[]) {
  return request({
    url: '/hongniang/tenantConfig/batch',
    method: 'delete',
    data: ids
  });
}

// 导出租户配置
export function exportTenantConfig(query: TenantConfigQuery) {
  return request({
    url: '/hongniang/tenantConfig/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  });
}