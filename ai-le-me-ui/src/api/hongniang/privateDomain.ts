import { shejiaoService } from '@/utils/request';

export function getHongniangPrivateDomainOverview(hongniangId?: number | string) {
  return shejiaoService({
    url: '/admin/hongniang-private-domain/overview',
    method: 'get',
    params: hongniangId ? { hongniangId } : {}
  });
}

export function getHongniangPrivateDomainKanban(params: Record<string, any> = {}) {
  return shejiaoService({
    url: '/admin/hongniang-private-domain/kanban',
    method: 'get',
    params
  });
}
