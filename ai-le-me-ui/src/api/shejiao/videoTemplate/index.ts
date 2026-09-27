import request from '@/utils/request';
import { VideoTemplateVO, VideoTemplateQuery, VideoTemplateForm } from './types';

// 查询AI视频模板列表
export function listVideoTemplate(query: VideoTemplateQuery) {
  return request({
    url: '/admin/videoTemplate/list',
    method: 'get',
    params: query
  });
}

// 查询AI视频模板详细
export function getVideoTemplate(id: string | number) {
  return request({
    url: '/admin/videoTemplate/info/' + id,
    method: 'get'
  });
}

// 新增AI视频模板
export function addVideoTemplate(data: VideoTemplateForm) {
  return request({
    url: '/admin/videoTemplate/save',
    method: 'post',
    data: data
  });
}

// 修改AI视频模板
export function updateVideoTemplate(data: VideoTemplateForm) {
  return request({
    url: '/admin/videoTemplate/update',
    method: 'post',
    data: data
  });
}

// 删除AI视频模板
export function delVideoTemplate(ids: Array<string | number>) {
  return request({
    url: '/admin/videoTemplate/delete',
    method: 'post',
    data: ids
  });
}
