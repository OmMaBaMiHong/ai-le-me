import { shejiaoService } from '@/utils/request';
import { TopicVO, TopicQuery, TopicForm } from './types';

/**
 * 查询圈子管理列表
 */
export function listTopic(query: TopicQuery) {
  return shejiaoService({
    url: '/admin/topic/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询圈子管理详细
 */
export function getTopic(id: string | number) {
  return shejiaoService({
    url: '/admin/topic/info/' + id,
    method: 'get'
  });
}

/**
 * 新增圈子
 */
export function addTopic(data: TopicForm) {
  return shejiaoService({
    url: '/admin/topic/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改圈子
 */
export function updateTopic(data: TopicForm) {
  return shejiaoService({
    url: '/admin/topic/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除圈子
 */
export function delTopic(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/topic/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
