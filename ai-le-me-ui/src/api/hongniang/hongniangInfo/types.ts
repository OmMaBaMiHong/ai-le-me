// 红娘信息类型定义
export interface HongniangInfoVO {
  /** 主键 */
  id: string | number;
  /** 红娘姓名 */
  hongniangName: string;
  /** 手机号 */
  phone: string;
  /** 微信号 */
  wechat: string;
  /** 所属机构 */
  companyName: string;
  /** 租户ID */
  tenantId?: string | number;
  /** 服务地区 */
  serviceArea: string;
  /** 红娘等级 */
  level: number;
  /** 认证状态 */
  certificationStatus: number;
  /** 状态 */
  status: number;
  /** 个人简介 */
  intro: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
  /** 管理用户数 */
  totalUsers: number;
  /** 组织活动数 */
  totalActivities: number;
}

export interface HongniangInfoQuery extends PageQuery {
  /** 红娘姓名 */
  hongniangName?: string;
  /** 手机号 */
  phone?: string;
  /** 状态 */
  status?: number;
  /** 认证状态 */
  certificationStatus?: number;
  /** 等级 */
  level?: number;
}

export interface HongniangInfoForm {
  /** 主键 */
  id?: string | number;
  /** 红娘姓名 */
  hongniangName: string;
  /** 手机号 */
  phone: string;
  /** 微信号 */
  wechat: string;
  /** 所属机构 */
  companyName: string;
  /** 租户ID */
  tenantId?: string | number;
  /** 服务地区 */
  serviceArea: string;
  /** 红娘等级 */
  level: number;
  /** 认证状态 */
  certificationStatus: number;
  /** 状态 */
  status: number;
  /** 个人简介 */
  intro: string;
}