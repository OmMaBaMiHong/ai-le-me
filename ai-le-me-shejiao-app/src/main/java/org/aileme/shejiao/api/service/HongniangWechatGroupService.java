package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.HongniangGroupTouchTaskEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangWechatGroupEntity;

import java.util.List;
import java.util.Map;

public interface HongniangWechatGroupService extends IService<HongniangWechatGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    Map<String, Object> getDetail(Integer groupId);

    void saveGroup(HongniangWechatGroupEntity entity);

    void updateGroup(HongniangWechatGroupEntity entity);

    void bindUsers(Integer groupId, List<Integer> userIds);

    void bindCases(Integer groupId, List<Integer> caseIds);

    Map<String, Object> syncGroup(Integer groupId);

    HongniangGroupTouchTaskEntity createTouchTask(HongniangGroupTouchTaskEntity task);

    PageUtils listTouchTasks(Map<String, Object> params);

    PageUtils listTouchLogs(Map<String, Object> params);

    Map<String, Object> retryTouchTask(Integer taskId);

    List<Map<String, Object>> searchBindableUsers(Integer groupId, Integer hongniangId, String keyword, Integer limit);

    List<Map<String, Object>> searchBindableCases(Integer groupId, Integer hongniangId, String keyword, Integer limit);

    List<Map<String, Object>> listTaskExecutions(Integer taskId, Integer groupId, Integer limit);
}
