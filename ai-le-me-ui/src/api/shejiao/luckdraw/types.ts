/**
 * 数据展示对象
 */
export interface LuckdrawVO {
  id?: number;
  title?: string;
  prizeList?: string;
  condition?: string;
  startTime?: string;
  endTime?: string;
  status?: number;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface LuckdrawQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  status?: number;
}

/**
 * 表单数据对象
 */
export interface LuckdrawForm {
  id?: number;
  title?: string;
  prizeList?: string;
  condition?: string;
  startTime?: string;
  endTime?: string;
  status?: number;
}
