import request from '@/utils/request';
import { HongniangInfoForm, HongniangInfoQuery, HongniangInfoVO } from './types';
import { AxiosPromise } from 'axios';

// 查询红娘信息列表
export function listHongniangInfo(query: HongniangInfoQuery): AxiosPromise<HongniangInfoVO[]> {
  return request({
    url: '/hongniang/hongniang-info/list',
    method: 'get',
    params: query
  });
}

// 查询红娘信息详细
export function getHongniangInfo(id: string | number): AxiosPromise<HongniangInfoVO> {
  return request({
    url: '/hongniang/hongniang-info/' + id,
    method: 'get'
  });
}

// 新增红娘信息
export function addHongniangInfo(data: HongniangInfoForm) {
  return request({
    url: '/hongniang/hongniang-info',
    method: 'post',
    data: data
  });
}

// 修改红娘信息
export function updateHongniangInfo(data: HongniangInfoForm) {
  return request({
    url: '/hongniang/hongniang-info',
    method: 'put',
    data: data
  });
}

// 删除红娘信息
export function delHongniangInfo(id: string | number | Array<string | number>) {
  return request({
    url: '/hongniang/hongniang-info/' + id,
    method: 'delete'
  });
}

// 导出红娘信息
export function exportHongniangInfo(query: HongniangInfoQuery) {
  return request({
    url: '/hongniang/hongniang-info/export',
    method: 'post',
    params: query
  });
}