package org.aileme.shejiao.admin.service.group;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.admin.dao.HongniangGroupTouchTaskDao;
import org.aileme.shejiao.api.service.HongniangWechatGroupService;
import org.aileme.shejiao.domain.entity.admin.HongniangGroupTouchTaskEntity;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class HongniangGroupTouchTaskScheduler {

    private final HongniangGroupTouchTaskDao touchTaskDao;
    private final HongniangWechatGroupService hongniangWechatGroupService;

    public HongniangGroupTouchTaskScheduler(HongniangGroupTouchTaskDao touchTaskDao,
                                           HongniangWechatGroupService hongniangWechatGroupService) {
        this.touchTaskDao = touchTaskDao;
        this.hongniangWechatGroupService = hongniangWechatGroupService;
    }

    @Scheduled(initialDelay = 60000L, fixedDelay = 60000L)
    public void dispatchDueTasks() {
        Date now = new Date();
        List<HongniangGroupTouchTaskEntity> dueTasks = touchTaskDao.selectList(new LambdaQueryWrapper<HongniangGroupTouchTaskEntity>()
            .eq(HongniangGroupTouchTaskEntity::getTaskStatus, 0)
            .isNotNull(HongniangGroupTouchTaskEntity::getScheduleTime)
            .le(HongniangGroupTouchTaskEntity::getScheduleTime, now)
            .orderByAsc(HongniangGroupTouchTaskEntity::getScheduleTime)
            .last("limit 20"));
        for (HongniangGroupTouchTaskEntity task : dueTasks) {
            try {
                hongniangWechatGroupService.retryTouchTask(task.getId());
            } catch (Exception e) {
                log.warn("dispatch hongniang group touch task failed, taskId={}", task.getId(), e);
                task.setTaskStatus(3);
                task.setResultSummary("计划任务执行失败：" + e.getMessage());
                task.setUpdateTime(new Date());
                touchTaskDao.updateById(task);
            }
        }
    }
}
