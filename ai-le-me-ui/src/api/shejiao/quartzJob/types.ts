export interface QuartzJobQuery {
  pageNum?: number;
  pageSize?: number;
  key?: string;
  status?: string | number;
}

export interface QuartzJobLogQuery {
  pageNum?: number;
  pageSize?: number;
  jobId?: string | number;
}

export interface QuartzJobVO {
  id?: number;
  jobName: string;
  jobGroup: string;
  jobCode: string;
  cronExpression: string;
  jobParams?: string;
  allowConcurrent: number;
  status: number;
  remark?: string;
  nextFireTime?: string;
  previousFireTime?: string;
  createTime?: string;
  updateTime?: string;
}

export interface QuartzJobLogVO {
  id: number;
  jobId: number;
  jobName: string;
  jobCode: string;
  executeStatus: number;
  resultSummary?: string;
  errorMessage?: string;
  durationMs?: number;
  startTime?: string;
  endTime?: string;
  createTime?: string;
}

export interface QuartzJobHandlerVO {
  jobCode: string;
  jobName: string;
  description?: string;
  defaultCronExpression?: string;
  defaultJobGroup?: string;
  defaultAllowConcurrent?: number;
}
