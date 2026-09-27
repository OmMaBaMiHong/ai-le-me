/**
 * 数据展示对象
 */
export interface SensitiveVO {
  id?: number;
  sensitiveWord?: string;
  state?: number;
  handleMeasures?: number;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface SensitiveQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
}

/**
 * 表单数据对象
 */
export interface SensitiveForm {
  id?: number;
  sensitiveWord?: string;
  state?: number;
  handleMeasures?: number;
}
