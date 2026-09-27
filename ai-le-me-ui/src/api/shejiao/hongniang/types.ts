/**
 * 数据展示对象
 */
export interface HongniangVO {
  id?: number;
  hongniangName?: string;
  phone?: string;
  wechat?: string;
  companyName?: string;
  serviceArea?: string;
  level?: number;
  certificationStatus?: number;
  status?: number;
  intro?: string;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface HongniangQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  status?: number;
  certificationStatus?: number;
}

/**
 * 表单数据对象
 */
export interface HongniangForm {
  id?: number;
  hongniangName?: string;
  phone?: string;
  wechat?: string;
  companyName?: string;
  serviceArea?: string;
  level?: number;
  certificationStatus?: number;
  status?: number;
  intro?: string;
}
