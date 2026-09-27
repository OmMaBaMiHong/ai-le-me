import { shejiaoService } from '@/utils/request';
import { MessageVO, MessageQuery, MessageForm } from './types';

/**
 * 查询消息管理列表
 */
export function listMessage(query: MessageQuery) {
  return shejiaoService({
    url: '/admin/message/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询消息管理详细
 */
export function getMessage(id: string | number) {
  return shejiaoService({
    url: '/admin/message/info/' + id,
    method: 'get'
  });
}

/**
 * 新增消息管理
 */
export function addMessage(data: MessageForm) {
  return shejiaoService({
    url: '/admin/message/save',
    method: 'post',
    data: data
  });
}

/**
 * 修改消息管理
 */
export function updateMessage(data: MessageForm) {
  return shejiaoService({
    url: '/admin/message/update',
    method: 'post',
    data: data
  });
}

/**
 * 删除消息管理
 */
export function delMessage(id: string | number | Array<string | number>) {
  return shejiaoService({
    url: '/admin/message/delete',
    method: 'post',
    data: Array.isArray(id) ? id : [id]
  });
}
