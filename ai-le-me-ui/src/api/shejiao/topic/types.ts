/**
 * 圈子管理 VO 对象
 */
export interface TopicVO {
  id?: number;
  uid?:number;
  topicName?: string;
  coverImage?: string;
  bgImage?: string;
  description?: string;
  postCount?: number;
  followCount?: number;
  isRecommend?: number;
  sortOrder?: number;
  status?: number;
  createTime?: string;
}

/**
 * 圈子管理查询对象
 */
export interface TopicQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  status?: number;
  isRecommend?: number;
}

/**
 * 圈子管理表单对象
 */
export interface TopicForm {
  id?: number;
  uid?: number;
  cateId?: number;
  topicName?: string;
  coverImage?: string;
  bgImage?: string;
  description?: string;
  topType?: number;
  status?: number;
  indexRecommend?: number;
  isRecommend?: number;
  sortOrder?: number;
  rest?: number;
  question?: string;
  isPrivacy?: number;
}
