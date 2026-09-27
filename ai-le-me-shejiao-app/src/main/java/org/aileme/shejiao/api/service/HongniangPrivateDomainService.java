package org.aileme.shejiao.api.service;

import org.aileme.shejiao.common.utils.PageUtils;

import java.util.Map;

public interface HongniangPrivateDomainService {

    Map<String, Object> overview(Integer hongniangId);

    Map<String, Object> kanban(Map<String, Object> params);

    Map<String, Object> workbenchOverview(Integer hongniangId);

    PageUtils workbenchUsers(Integer hongniangId, Map<String, Object> params);

    PageUtils workbenchCases(Integer hongniangId, Map<String, Object> params);

    Map<String, Object> workbenchCaseDetail(Integer hongniangId, Integer caseId);

    PageUtils workbenchGroups(Integer hongniangId, Map<String, Object> params);

    Map<String, Object> workbenchGroupDetail(Integer hongniangId, Integer groupId);
}
