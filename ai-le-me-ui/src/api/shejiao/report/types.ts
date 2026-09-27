/**
 * 数据展示对象
 */
export interface ReportVO {
  id?: number;
  reporterUid?: number;
  reporterName?: string;
  reportedUid?: number;
  reportedName?: string;
  type?: number;
  reason?: string;
  status?: number;
  handleNote?: string;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface ReportQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  type?: number;
  status?: number;
}

/**
 * 表单数据对象
 */
export interface ReportForm {
  id?: number;
  status?: number;
  handleNote?: string;
}
