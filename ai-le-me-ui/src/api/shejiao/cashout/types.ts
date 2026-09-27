/**
 * 数据展示对象
 */
export interface CashoutVO {
  id?: number;
  uid?: number;
  username?: string;
  amount?: number;
  status?: number;
  applyTime?: string;
  handleTime?: string;
  rejectReason?: string;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface CashoutQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  status?: number;
}

/**
 * 表单数据对象
 */
export interface CashoutForm {
  id?: number;
  status?: number;
  rejectReason?: string;
}
