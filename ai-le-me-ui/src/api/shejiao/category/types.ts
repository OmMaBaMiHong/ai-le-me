/**
 * 分类管理 VO 对象
 */
export interface CategoryVO {
  cateId?: number;
  cateName?: string;
  isTop?: number; // 0未置顶 1已置顶
  coverImage?: string;
  createTime?: string;
  updateTime?: string;
}

/**
 * 分类管理查询对象
 */
export interface CategoryQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  isTop?: number;
}

/**
 * 分类管理表单对象
 */
export interface CategoryForm {
  cateId?: number;
  cateName?: string;
  isTop?: number;
  coverImage?: string;
}
