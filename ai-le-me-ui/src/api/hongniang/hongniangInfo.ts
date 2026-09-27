import { shejiaoService } from '@/utils/request';
import { HongniangInfoForm, HongniangInfoQuery, HongniangInfoVO } from './types';

// 查询红娘信息列表
export function listHongniangInfo(query: HongniangInfoQuery) {
  return shejiaoService({
    url: '/admin/hongniang/list',
    method: 'get',
    params: query
  });
}

// 查询红娘信息详细
export function getHongniangInfo(id: number | string) {
  return shejiaoService({
    url: `/admin/hongniang/info/${id}`,
    method: 'get'
  });
}

// 新增红娘信息
export function addHongniangInfo(data: HongniangInfoForm) {
  return shejiaoService({
    url: '/admin/hongniang/save',
    method: 'post',
    data: data
  });
}

// 修改红娘信息
export function updateHongniangInfo(data: HongniangInfoForm) {
  return shejiaoService({
    url: '/admin/hongniang/update',
    method: 'post',
    data: data
  });
}

// 删除红娘信息
export function delHongniangInfo(ids: (number | string)[]) {
  return shejiaoService({
    url: '/admin/hongniang/delete',
    method: 'post',
    data: ids
  });
}

// 导入用户（PDF/DOCX）
export function importUsers(file: File, hongniangId: number | string) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('hongniangId', hongniangId.toString());
  
  return shejiaoService({
    url: '/admin/hongniang/importUsers',
    method: 'post',
    data: formData
  });
}

// 更新统计数据
export function updateStatistics(id: number | string) {
  return shejiaoService({
    url: `/admin/hongniang/updateStatistics/${id}`,
    method: 'post'
  });
}

// 导出红娘信息（暂未实现后端接口）
export function exportHongniangInfo(query: HongniangInfoQuery) {
  console.warn('导出功能暂未实现后端接口');
  return Promise.reject(new Error('导出功能暂未实现'));
}

// 修改红娘信息状态（暂未实现后端接口）
export function changeHongniangInfoStatus(id: number | string, status: number) {
  console.warn('状态修改功能暂未实现后端接口');
  return Promise.reject(new Error('状态修改功能暂未实现'));
}

// 获取所有红娘列表（用于下拉选择等场景）
export function getAllHongniangs() {
  return shejiaoService({
    url: '/admin/hongniang/list',
    method: 'get',
    params: {
      pageNum: 1,
      pageSize: 9999,
      status: 1 // 只获取启用状态的红娘
    }
  });
}

// 根据ID获取红娘详情（别名方法，保持API一致性）
export function getHongniangById(hongniangId: number | string) {
  return getHongniangInfo(hongniangId);
}