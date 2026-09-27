/**
 * 数据展示对象
 */
export interface CommentVO {
  id?: number;
  uid?: number;
  username?: string;
  avatar?: string;
  content?: string;
  likeCount?: number;
  postId?: number;
  postTitle?: string;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface CommentQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
}

/**
 * 表单数据对象
 */
export interface CommentForm {
  id?: number;
  content?: string;
}
