import request from '@/utils/request';
import { HongniangUserRelationForm, HongniangUserRelationQuery, HongniangUserRelationVO } from './types';
import { AxiosPromise } from 'axios';

// 查询红娘用户关系列表
export function listHongniangUserRelation(query: HongniangUserRelationQuery): AxiosPromise<HongniangUserRelationVO[]> {
  return request({
    url: '/hongniang/hongniang-user-relation/list',
    method: 'get',
    params: query
  });
}

// 查询红娘用户关系详细
export function getHongniangUserRelation(id: string | number): AxiosPromise<HongniangUserRelationVO> {
  return request({
    url: '/hongniang/hongniang-user-relation/' + id,
    method: 'get'
  });
}

// 新增红娘用户关系
export function addHongniangUserRelation(data: HongniangUserRelationForm) {
  return request({
    url: '/hongniang/hongniang-user-relation',
    method: 'post',
    data: data
  });
}

// 修改红娘用户关系
export function updateHongniangUserRelation(data: HongniangUserRelationForm) {
  return request({
    url: '/hongniang/hongniang-user-relation',
    method: 'put',
    data: data
  });
}

// 删除红娘用户关系
export function delHongniangUserRelation(id: string | number | Array<string | number>) {
  return request({
    url: '/hongniang/hongniang-user-relation/' + id,
    method: 'delete'
  });
}

// 导出红娘用户关系
export function exportHongniangUserRelation(query: HongniangUserRelationQuery) {
  return request({
    url: '/hongniang/hongniang-user-relation/export',
    method: 'post',
    params: query
  });
}