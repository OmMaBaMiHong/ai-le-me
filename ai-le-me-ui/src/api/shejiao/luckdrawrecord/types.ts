/**
 * 数据展示对象
 */
export interface LuckdrawrecordVO {
  id?: number;
  uid?: number;
  username?: string;
  luckdrawId?: number;
  prize?: string;
  drawTime?: string;
  receiveStatus?: number;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface LuckdrawrecordQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  receiveStatus?: number;
}

/**
 * 表单数据对象
 */
export interface LuckdrawrecordForm {
  id?: number;
}
