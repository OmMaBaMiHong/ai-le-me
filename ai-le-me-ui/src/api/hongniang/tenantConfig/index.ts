import request from '@/utils/request';
import { TenantConfigForm, TenantConfigQuery, TenantConfigVO } from './types';
import { AxiosPromise } from 'axios';

// 查询租户配置列表
export function listTenantConfig(query: TenantConfigQuery): AxiosPromise<TenantConfigVO[]> {
  return request({
    url: '/hongniang/tenant-config/list',
    method: 'get',
    params: query
  });
}

// 查询租户配置详细
export function getTenantConfig(id: string | number): AxiosPromise<TenantConfigVO> {
  return request({
    url: '/hongniang/tenant-config/' + id,
    method: 'get'
  });
}

// 新增租户配置
export function addTenantConfig(data: TenantConfigForm) {
  return request({
    url: '/hongniang/tenant-config',
    method: 'post',
    data: data
  });
}

// 修改租户配置
export function updateTenantConfig(data: TenantConfigForm) {
  return request({
    url: '/hongniang/tenant-config',
    method: 'put',
    data: data
  });
}

// 删除租户配置
export function delTenantConfig(id: string | number | Array<string | number>) {
  return request({
    url: '/hongniang/tenant-config/' + id,
    method: 'delete'
  });
}

// 导出租户配置
export function exportTenantConfig(query: TenantConfigQuery) {
  return request({
    url: '/hongniang/tenant-config/export',
    method: 'post',
    params: query
  });
}