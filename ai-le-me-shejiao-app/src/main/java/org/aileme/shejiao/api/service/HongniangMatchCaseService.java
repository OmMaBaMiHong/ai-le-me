package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchCaseEntity;

import java.util.List;
import java.util.Map;

public interface HongniangMatchCaseService extends IService<HongniangMatchCaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    Map<String, Object> getDetail(Integer caseId);

    void createCase(HongniangMatchCaseEntity entity, Long operatorId);

    void updateCase(HongniangMatchCaseEntity entity, Long operatorId);

    void advanceStage(Integer caseId, Integer targetStage, String content, java.util.Date actualFollowTime,
                      java.util.Date nextFollowTime, String attachments, Long operatorId);

    void addProgress(Integer caseId, Integer progressType, String content, java.util.Date plannedFollowTime,
                     java.util.Date actualFollowTime, String attachments, Long operatorId);

    void closeCase(Integer caseId, String closeReason, String content, Long operatorId);

    void bindGroups(Integer caseId, List<Integer> groupIds);

    List<Map<String, Object>> searchPoolUsers(Integer hongniangId, String keyword, Integer gender,
                                              Integer limit, Integer excludeUserId);

    List<Map<String, Object>> listSourceCandidates(Integer hongniangId, Integer sourceType, String keyword, Integer limit);

    Map<String, Object> createMatchRequest(Integer caseId,
                                           Integer fromUserId,
                                           Integer toUserId,
                                           Integer requestChannel,
                                           String requestMessage,
                                           String wechatShareSnapshot,
                                           java.util.Date expireTime,
                                           Long operatorId);

    int countSuccessCases(Integer hongniangId);
}
