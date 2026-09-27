import { shejiaoService } from '@/utils/request';
import { PostVO, PostQuery, PostForm } from './types';

/**
 * 查询帖子管理列表
 */
export function listPost(query: PostQuery) {
  const { pageNum, pageSize, ...rest } = query || {};
  return shejiaoService({
    url: '/admin/post/list',
    method: 'get',
    params: {
      page: pageNum,
      limit: pageSize,
      ...rest
    }
  });
}

/**
 * 查询帖子管理详细
 */
export function getPost(id: string | number) {
  return shejiaoService({
    url: '/admin/post/info/' + id,
    method: 'get'
  });
}

/**
 * 新增帖子管理
 */
export function addPost(data: PostForm) {
  return shejiaoService({
    url: '/admin/post/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改帖子管理
 */
export function updatePost(data: PostForm) {
  return shejiaoService({
    url: '/admin/post/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除帖子管理
 */
export function delPost(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/post/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
