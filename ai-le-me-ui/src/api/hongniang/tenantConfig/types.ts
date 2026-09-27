// 租户配置类型定义
export interface TenantConfigVO {
  /** 主键 */
  id: string | number;
  /** 租户ID */
  tenantId: string | number;
  /** 配置键 */
  configKey: string;
  /** 配置值 */
  configValue: string;
  /** 配置描述 */
  configDesc: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

export interface TenantConfigQuery extends PageQuery {
  /** 租户ID */
  tenantId?: string | number;
  /** 配置键 */
  configKey?: string;
}

export interface TenantConfigForm {
  /** 主键 */
  id?: string | number;
  /** 租户ID */
  tenantId: string | number;
  /** 配置键 */
  configKey: string;
  /** 配置值 */
  configValue: string;
  /** 配置描述 */
  configDesc: string;
}