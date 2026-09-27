import { shejiaoService } from '@/utils/request';
import { CommentVO, CommentQuery, CommentForm } from './types';

/**
 * 查询评论管理列表
 */
export function listComment(query: CommentQuery) {
  return shejiaoService({
    url: '/admin/comment/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询评论管理详细
 */
export function getComment(id: string | number) {
  return shejiaoService({
    url: '/admin/comment/info/' + id,
    method: 'get'
  });
}

/**
 * 新增评论管理
 */
export function addComment(data: CommentForm) {
  return shejiaoService({
    url: '/admin/comment/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改评论管理
 */
export function updateComment(data: CommentForm) {
  return shejiaoService({
    url: '/admin/comment/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除评论管理
 */
export function delComment(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/comment/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
