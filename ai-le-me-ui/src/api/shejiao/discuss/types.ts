/**
 * 数据展示对象
 */
export interface DiscussVO {
  id?: number;
  title?: string;
  content?: string;
  uid?: number;
  username?: string;
  replyCount?: number;
  status?: number;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface DiscussQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  status?: number;
}

/**
 * 表单数据对象
 */
export interface DiscussForm {
  id?: number;
  title?: string;
  content?: string;
  status?: number;
}
