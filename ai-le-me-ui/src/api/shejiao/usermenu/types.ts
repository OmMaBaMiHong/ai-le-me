/**
 * 数据展示对象
 */
export interface UserMenuVO {
  id?: number;
  name?: string;
  img?: string;
  url?: string;
  sort?: number;
  status?: number;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface UserMenuQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  status?: number;
}

/**
 * 表单数据对象
 */
export interface UserMenuForm {
  id?: number;
  name?: string;
  img?: string;
  url?: string;
  sort?: number;
  status?: number;
}
