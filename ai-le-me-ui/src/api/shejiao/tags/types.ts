/**
 * 数据展示对象
 */
export interface TagsVO {
  id?: number;
  tagName?: string;
  tagCategory?: string;
  tagType?: number; // 1-系统标签 2-自定义标签
  icon?: string;
  color?: string;
  sort?: number;
  usageCount?: number;
  status?: number;
  createTime?: string;
}

/**
 * 查询参数对象
 */
export interface TagsQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  status?: number;
}

/**
 * 表单数据对象
 */
export interface TagsForm {
  id?: number;
  tagName?: string;
  tagCategory?: string;
  tagType?: number;
  icon?: string;
  color?: string;
  sort?: number;
  status?: number;
}
