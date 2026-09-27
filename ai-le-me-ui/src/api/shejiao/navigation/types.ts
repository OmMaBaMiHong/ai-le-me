/**
 * 数据展示对象
 */
export interface NavigationVO {
  id?: number;
  title?: string;
  img?: string;
  url?: string;
  type?: number;  // 跳转类型：0页面 1外链
  status?: number;  // 状态：0正常 1禁用
  createTime?: string;
  updateTime?: string;
}

/**
 * 查询参数对象
 */
export interface NavigationQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  status?: number;
}

/**
 * 表单数据对象
 */
export interface NavigationForm {
  id?: number;
  title?: string;
  img?: string;
  url?: string;
  type?: number;  // 跳转类型：0页面 1外链
  status?: number;  // 状态：0正常 1禁用
}
