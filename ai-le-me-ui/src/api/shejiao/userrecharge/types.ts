/**
 * 数据展示对象
 */
export interface UserRechargeVO {
  id?: number;
  uid?: number;
  nickname?: string;
  orderId?: string;
  title?: string;
  bizId?: string;
  price?: number | string;
  givePrice?: number | string;
  coinAmount?: number;
  rechargeType?: string;
  status?: number;
  payTime?: string;
  addTime?: string;
  updateTime?: string;
  refundAmount?: number | string | null;
  transactionId?: string | null;
  outTradeNo?: string | null;
  type?: number;
  channel?: string | null;
  remark?: string | null;
}

export interface UserRechargeRefundVO {
  id?: number;
  rechargeId?: number;
  uid?: number;
  orderId?: string;
  type?: number;
  refundNo?: string;
  refundAmount?: number | string | null;
  coinAmount?: number;
  refundStatus?: number;
  reason?: string | null;
  operatorName?: string | null;
  transactionId?: string | null;
  addTime?: string;
  refundTime?: string;
}

/**
 * 查询参数对象
 */
export interface UserRechargeQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  type?: number | string;
  type2?: number | string;
}

/**
 * 表单数据对象
 */
export interface UserRechargeForm {
  id?: number;
}

export interface UserRechargeRefundForm {
  orderId?: string;
  refundAmount?: number | string;
  reason?: string;
}
