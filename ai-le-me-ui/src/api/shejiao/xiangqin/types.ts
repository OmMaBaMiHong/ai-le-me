export interface XiangqinActivityVO {
  id?: number;
  hongniangId?: number;
  title: string;
  coverImg?: string;
  description?: string;
  activityType?: number;
  address: string;
  longitude?: number;
  latitude?: number;
  startTime: string;
  endTime: string;
  enrollStartTime?: string;
  enrollEndTime?: string;
  maxParticipants: number;
  maleCount?: number;
  femaleCount?: number;
  feeType?: number;
  maleFee?: number;
  femaleFee?: number;
  contactPhone?: string;
  contactWechat?: string;
  ageMin?: number;
  ageMax?: number;
  requirement?: string;
  status: number;
  viewCount?: number;
  createTime?: string;
  updateTime?: string;
}

export interface XiangqinActivityQuery extends PageQuery {
  title?: string;
  status?: number;
}

export interface XiangqinActivityForm {
  id?: number;
  hongniangId?: number;
  title: string;
  coverImg?: string;
  description?: string;
  activityType?: number;
  address: string;
  longitude?: number;
  latitude?: number;
  startTime: string;
  endTime: string;
  enrollStartTime?: string;
  enrollEndTime?: string;
  maxParticipants: number;
  feeType?: number;
  maleFee?: number;
  femaleFee?: number;
  contactPhone?: string;
  contactWechat?: string;
  ageMin?: number;
  ageMax?: number;
  requirement?: string;
  status: number;
}

export interface XiangqinEnrollmentVO {
  id?: number;
  activityId?: number;
  uid?: number;
  realName?: string;
  username?: string;
  avatar?: string;
  gender?: number;
  age?: number;
  phone?: string;
  status?: number;
  createTime?: string;
}
