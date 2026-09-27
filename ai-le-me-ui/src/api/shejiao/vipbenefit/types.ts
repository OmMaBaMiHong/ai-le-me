export interface VipBenefitVO {
  id?: number;
  title: string;
  describes: string;
  icon: string;
  status: number;
  sort: number;
  createTime?: string;
  updateTime?: string;
}

export interface VipBenefitQuery extends PageQuery {
  key?: string;
}

export interface VipBenefitForm {
  id?: number;
  title: string;
  describes: string;
  icon: string;
  status: number;
  sort: number;
}
