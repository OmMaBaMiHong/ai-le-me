/**
 * 数据展示对象
 */
export interface BillVO {
  id?: number;
  uid?: number;
  tipUserId?: number | null;
  linkId?: string;
  pm?: number;
  title?: string;
  category?: string;
  type?: string;
  number?: number | string;
  balance?: number;
  mark?: string;
  addTime?: string;
  status?: number;
}

/**
 * 查询参数对象
 */
export interface BillQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  type?: number | string;
  type2?: string;
}

/**
 * 表单数据对象
 */
export interface BillForm {
  id?: number;
}
