// HongniangInfo 相关类型定义

export interface HongniangInfoVO {
  id?: number;
  hongniangName?: string;
  phone?: string;
  wechat?: string;
  companyName?: string;
  serviceArea?: string;
  level?: number;
  totalUsers?: number;
  totalActivities?: number;
  certificationStatus?: number;
  status?: number;
  intro?: string;
  createTime?: Date;
  updateTime?: Date;
}

export interface HongniangInfoForm {
  id?: number;
  hongniangName: string;
  phone: string;
  wechat?: string;
  companyName?: string;
  serviceArea?: string;
  level?: number;
  certificationStatus?: number;
  status?: number;
  intro?: string;
}

export interface HongniangInfoQuery {
  pageNum?: number;
  pageSize?: number;
  hongniangName?: string;
  phone?: string;
  status?: number;
}

export interface HongniangUserVO {
  id?: number;
  hongniangId?: number;
  hongniangUserNo?: number;
  userId?: number;
  hongniangName?: string;
  hongniangPhone?: string;
  username?: string;
  userMobile?: string;
  userGender?: number;
  userAge?: number;
  relationTime?: Date;
  createTime?: Date;
  updateTime?: Date;
}

export interface HongniangUserForm {
  id?: number | string;
  hongniangId: number | string;
  userId: number | string;
  hongniangUserNo?: number | string;
}

export interface HongniangUserQuery {
  pageNum?: number;
  pageSize?: number;
  hongniangName?: string;
  userName?: string;
  userKeyword?: string;
  hongniangUserNo?: number | string;
}

export interface TenantConfigVO {
  id?: number;
  tenantId?: string;
  configKey?: string;
  configValue?: string;
  configType?: string;
  remark?: string;
  createTime?: Date;
  updateTime?: Date;
}

export interface TenantConfigForm {
  id?: number;
  tenantId: string;
  configKey: string;
  configValue: string;
  configType: string;
  remark?: string;
}

export interface TenantConfigQuery {
  pageNum?: number;
  pageSize?: number;
  tenantId?: string;
  configKey?: string;
}

export interface HongniangMatchCaseVO {
  id?: number;
  hongniangId?: number;
  hongniangName?: string;
  maleUserId?: number;
  maleUsername?: string;
  maleMobile?: string;
  femaleUserId?: number;
  femaleUsername?: string;
  femaleMobile?: string;
  currentStage?: number;
  currentStageLabel?: string;
  sourceType?: number;
  sourceRefId?: number | string;
  sourceDisplay?: string;
  nextFollowTime?: string;
  lastFollowTime?: string;
  closeReason?: string;
  remark?: string;
  groupCount?: number;
  createTime?: string;
  updateTime?: string;
}

export interface HongniangMatchCaseForm {
  id?: number | string;
  hongniangId: number | string;
  maleUserId: number | string;
  femaleUserId: number | string;
  currentStage?: number;
  sourceType?: number;
  sourceRefId?: number | string | null;
  nextFollowTime?: string;
  remark?: string;
}

export interface HongniangMatchCaseQuery {
  pageNum?: number;
  pageSize?: number;
  hongniangId?: number | string;
  hongniangName?: string;
  maleKeyword?: string;
  femaleKeyword?: string;
  currentStage?: number;
  sourceType?: number;
  nextFollowDate?: string;
}

export interface HongniangMatchProgressForm {
  caseId: number | string;
  progressType?: number;
  content?: string;
  plannedFollowTime?: string;
  actualFollowTime?: string;
  attachments?: string;
}

export interface HongniangMatchRequestVO {
  id?: number;
  caseId?: number;
  fromUserId?: number;
  fromUserName?: string;
  toUserId?: number;
  toUserName?: string;
  requestChannel?: number;
  requestChannelLabel?: string;
  requestStatus?: number;
  requestStatusLabel?: string;
  requestMessage?: string;
  intentRequestId?: string;
  wechatShareSnapshot?: string;
  expireTime?: string;
  createTime?: string;
}

export interface HongniangWechatGroupVO {
  id?: number;
  hongniangId?: number;
  hongniangName?: string;
  groupName?: string;
  groupType?: number;
  providerType?: number;
  ownerName?: string;
  ownerWechat?: string;
  tagJson?: string;
  city?: string;
  purpose?: string;
  qrCodeUrl?: string;
  joinLink?: string;
  externalGroupId?: string;
  syncStatus?: number;
  syncStatusLabel?: string;
  lastSyncTime?: string;
  remark?: string;
  boundUserCount?: number;
  boundCaseCount?: number;
  createTime?: string;
  updateTime?: string;
}

export interface HongniangWechatGroupForm {
  id?: number | string;
  hongniangId: number | string;
  groupName: string;
  groupType: number;
  providerType?: number;
  ownerName?: string;
  ownerWechat?: string;
  tagJson?: string;
  city?: string;
  purpose?: string;
  qrCodeUrl?: string;
  joinLink?: string;
  externalGroupId?: string;
  remark?: string;
}

export interface HongniangWechatGroupQuery {
  pageNum?: number;
  pageSize?: number;
  hongniangId?: number | string;
  hongniangName?: string;
  keyword?: string;
  city?: string;
  groupType?: number;
  syncStatus?: number;
  tagKeyword?: string;
}

export interface HongniangGroupTouchTaskForm {
  id?: number | string;
  hongniangId?: number | string;
  groupId: number | string;
  taskName: string;
  sendScopeType?: number;
  contentType?: number;
  contentPayload?: string;
  scheduleTime?: string;
  taskStatus?: number;
  providerType?: number;
  providerTaskId?: string;
  resultSummary?: string;
}

export interface HongniangPrivateDomainOverviewVO {
  poolUserCount?: number;
  caseCount?: number;
  openCaseCount?: number;
  successCaseCount?: number;
  wechatGroupCount?: number;
  activeTouchTaskCount?: number;
  pendingMatchRequestCount?: number;
}
