package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.job.SysQuartzJob;

import java.util.List;
import java.util.Map;

public interface SysQuartzJobService extends IService<SysQuartzJob> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryLogPage(Map<String, Object> params);

    SysQuartzJob getDetail(Long id);

    void saveJob(SysQuartzJob job);

    void updateJob(SysQuartzJob job);

    void deleteJobs(List<Long> ids);

    void changeStatus(Long id, Integer status);

    void runOnce(Long id);

    void syncAllJobs();

    Map<String, Object> getJobRuntime(String jobCode);

    void recordExecution(Long jobId, String jobCode, String result, String errorMessage, long startTimestamp);
}
