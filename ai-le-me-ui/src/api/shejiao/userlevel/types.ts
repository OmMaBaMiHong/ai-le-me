export interface UserLevelVO {
  id?: number;
  name: string;
  minNum: number;
  maxNum: number;
  levelId: number;
  createTime?: string;
  updateTime?: string;
}

export interface UserLevelQuery extends PageQuery {
  key?: string;
}

export interface UserLevelForm {
  id?: number;
  name: string;
  minNum: number;
  maxNum: number;
  levelId: number;
}
