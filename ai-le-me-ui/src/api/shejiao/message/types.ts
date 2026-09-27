/**
 * 数据展示对象
 */
export interface MessageVO {
  id?: number;
  title?: string;
  content?: string;
  receiverUid?: number;
  type?: number;
  status?: number;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface MessageQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  type?: number;
  status?: number;
}

/**
 * 表单数据对象
 */
export interface MessageForm {
  id?: number;
  title?: string;
  content?: string;
  receiverUid?: number;
  type?: number;
}
