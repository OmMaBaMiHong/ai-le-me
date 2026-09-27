/**
 * 数据展示对象
 */
export interface SignconfigVO {
  id?: number;
  continuousDays?: number;
  rewardPoints?: number;
  rewardCoins?: number;
  description?: string;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface SignconfigQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
}

/**
 * 表单数据对象
 */
export interface SignconfigForm {
  id?: number;
  continuousDays?: number;
  rewardPoints?: number;
  rewardCoins?: number;
  description?: string;
}
