/**
 * 数据展示对象
 */
export interface UserVO {
  uid?: number;
  username?: string;
  avatar?: string;
  sex?: number;
  age?: number;
  intro?: string;
  phone?: string;
  email?: string;
  status?: number;
  type?: number;
  vipStatus?: number;
  vipExpireTime?: string;
  level?: number;
  coin?: number;
  birthday?: string;
  city?: string;
  lastLoginIp?: string;
  createTime?: string;
  updateTime?: string;
}

/**
 * 查询参数对象
 */
export interface UserQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  status?: number;
  type?: number;
  vipStatus?: number;
}

/**
 * 表单数据对象
 */
export interface UserForm {
  uid?: number;
  username?: string;
  avatar?: string;
  sex?: number;
  age?: number;
  intro?: string;
  phone?: string;
  email?: string;
  status?: number;
  type?: number;
  vipStatus?: number;
  birthday?: string;
  city?: string;
}
