// 红娘用户关系类型定义
export interface HongniangUserRelationVO {
  /** 主键 */
  id: string | number;
  /** 红娘ID */
  hongniangId: string | number;
  /** 用户ID */
  userId: string | number;
  /** 关系类型 */
  relationType: number;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
  /** 红娘姓名 */
  hongniangName: string;
  /** 用户姓名 */
  userName: string;
  /** 用户手机号 */
  userPhone: string;
}

export interface HongniangUserRelationQuery extends PageQuery {
  /** 红娘ID */
  hongniangId?: string | number;
  /** 用户ID */
  userId?: string | number;
  /** 关系类型 */
  relationType?: number;
}

export interface HongniangUserRelationForm {
  /** 主键 */
  id?: string | number;
  /** 红娘ID */
  hongniangId: string | number;
  /** 用户ID */
  userId: string | number;
  /** 关系类型 */
  relationType: number;
}