/**
 * 数据展示对象
 */
export interface LinkVO {
  id?: number;
  title?: string;
  url?: string;
  icon?: string;
  sortOrder?: number;
  position?: number;
  status?: number;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface LinkQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  position?: number;
  status?: number;
}

/**
 * 表单数据对象
 */
export interface LinkForm {
  id?: number;
  title?: string;
  url?: string;
  icon?: string;
  sortOrder?: number;
  position?: number;
  status?: number;
}
