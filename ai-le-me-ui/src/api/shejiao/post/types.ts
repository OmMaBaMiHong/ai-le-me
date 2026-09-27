/**
 * 数据展示对象
 */
export interface PostVO {
  id?: number;
  uid?: number;
  userInfo?: { username?: string };
  avatar?: string;
  topicName?: string;
  title?: string;
  content?: string;
  type?: number;
  media?: string[];
  readCount?: number;
  commentCount?: number;
  collectionCount?: number;
  cut?: number;
  isPrivate?: number;
  postTop?: number;
  status?: number;
  createTime?: string;
  discussTitle?: string;
  address?: string;
}

/**
 * 查询参数对象
 */
export interface PostQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  status?: number;
  type?: number;
}

/**
 * 表单数据对象
 */
export interface PostForm {
  id?: number;
  title?: string;
  content?: string;
  type?: number;
  media?: string[];
  cut?: number;
  isPrivate?: number;
  postTop?: number;
  status?: number;
}
