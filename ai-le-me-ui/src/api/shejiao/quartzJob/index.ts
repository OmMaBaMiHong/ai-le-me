import { shejiaoService } from '@/utils/request';
import type { QuartzJobLogQuery, QuartzJobQuery, QuartzJobVO } from './types';

export function listQuartzJobs(params: QuartzJobQuery) {
  return shejiaoService({
    url: '/admin/quartzJob/list',
    method: 'get',
    params
  });
}

export function listQuartzJobLogs(params: QuartzJobLogQuery) {
  return shejiaoService({
    url: '/admin/quartzJob/logs',
    method: 'get',
    params
  });
}

export function listQuartzJobHandlers() {
  return shejiaoService({
    url: '/admin/quartzJob/handlers',
    method: 'get'
  });
}

export function getQuartzJobInfo(id: string | number) {
  return shejiaoService({
    url: `/admin/quartzJob/info/${id}`,
    method: 'get'
  });
}

export function saveQuartzJob(data: QuartzJobVO) {
  return shejiaoService({
    url: '/admin/quartzJob/save',
    method: 'post',
    data
  });
}

export function updateQuartzJob(data: QuartzJobVO) {
  return shejiaoService({
    url: '/admin/quartzJob/update',
    method: 'post',
    data
  });
}

export function deleteQuartzJobs(ids: Array<string | number>) {
  return shejiaoService({
    url: '/admin/quartzJob/delete',
    method: 'post',
    data: ids
  });
}

export function changeQuartzJobStatus(id: string | number, status: number) {
  return shejiaoService({
    url: `/admin/quartzJob/changeStatus/${id}/${status}`,
    method: 'post'
  });
}

export function runQuartzJobOnce(id: string | number) {
  return shejiaoService({
    url: `/admin/quartzJob/runOnce/${id}`,
    method: 'post'
  });
}

export function ensureRobotSinglePostJob() {
  return shejiaoService({
    url: '/admin/robotSeed/ensureSinglePostJob',
    method: 'post'
  });
}

export function runRobotSinglePost(createUserIfNeeded = true) {
  return shejiaoService({
    url: '/admin/robotSeed/singlePost',
    method: 'post',
    params: {
      createUserIfNeeded
    }
  });
}

export function runRobotBatchSeed(userCount: number, minPostsPerUser: number, maxPostsPerUser: number) {
  return shejiaoService({
    url: '/admin/robotSeed/batchUsers',
    method: 'post',
    params: {
      userCount,
      minPostsPerUser,
      maxPostsPerUser
    }
  });
}
