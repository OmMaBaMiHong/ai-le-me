/**
 * 数据展示对象
 */
export interface VipoptionVO {
  id?: number;
  packageName?: string;
  price?: number;
  duration?: number;
  description?: string;
  sortOrder?: number;
  status?: number;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface VipoptionQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  status?: number;
}

/**
 * 表单数据对象
 */
export interface VipoptionForm {
  id?: number;
  packageName?: string;
  price?: number;
  duration?: number;
  description?: string;
  sortOrder?: number;
  status?: number;
}
