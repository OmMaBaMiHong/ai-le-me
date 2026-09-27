export interface RechargeVO {
  id?: number;
  price: number | string;
  givePrice: number | string;
  sort: number;
  status: number;
}

export interface RechargeQuery extends PageQuery {
  key?: string;
}

export interface RechargeForm {
  id?: number;
  price: number | string;
  givePrice: number | string;
  sort: number;
  status: number;
}
