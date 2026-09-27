/**
 * 数据展示对象
 */
export interface UsersignVO {
  uid?: number;
  username?: string;
  continuousDays?: number;
  totalDays?: number;
  lastSignTime?: string;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface UsersignQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
}

/**
 * 表单数据对象
 */
export interface UsersignForm {
  uid?: number;
}
