package org.aileme.shejiao.app.service.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class QuartzManagedJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        QuartzManagedJobSupport.execute(context);
    }
}
