package org.aileme.shejiao.app.service.agent;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.RecommendLoveService;
import org.aileme.shejiao.api.service.SysQuartzJobService;
import org.aileme.shejiao.domain.entity.job.SysQuartzJob;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class SmartMatchReplenishService {

    public static final String JOB_CODE = "smart_match_replenish";

    @Autowired
    private RecommendLoveService recommendLoveService;

    @Autowired
    @Lazy
    private SysQuartzJobService sysQuartzJobService;

    @Value("${shejiao.smart-match.replenish.batch-size:30}")
    private int batchSize;

    public String replenishDailyQuota() {
        String result = recommendLoveService.replenishDailySmartMatches(Math.max(5, batchSize));
        log.info("[smart-match-replenish] {}", result);
        return result;
    }

    @DSTransactional
    public Map<String, Object> ensureDefaultQuartzJob() {
        SysQuartzJob job = sysQuartzJobService.lambdaQuery()
                .eq(SysQuartzJob::getDeleted, 0)
                .eq(SysQuartzJob::getJobCode, JOB_CODE)
                .last("limit 1")
                .one();
        boolean created = false;
        if (job == null) {
            job = new SysQuartzJob();
            job.setJobName("智能红娘每日补量");
            job.setJobGroup("AI");
            job.setJobCode(JOB_CODE);
            job.setCronExpression("0 0/30 * * * ?");
            job.setJobParams("");
            job.setAllowConcurrent(0);
            job.setStatus(SysQuartzJob.STATUS_ENABLED);
            job.setRemark("默认启用，自动补足当日未消耗完的智能红娘推荐名额。");
            sysQuartzJobService.saveJob(job);
            created = true;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("jobId", job.getId());
        result.put("jobCode", job.getJobCode());
        result.put("status", job.getStatus());
        result.put("cronExpression", job.getCronExpression());
        return result;
    }
}
