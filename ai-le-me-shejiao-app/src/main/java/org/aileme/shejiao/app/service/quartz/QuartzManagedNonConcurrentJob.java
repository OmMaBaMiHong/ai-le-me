package org.aileme.shejiao.app.service.quartz;

import org.quartz.DisallowConcurrentExecution;

@DisallowConcurrentExecution
public class QuartzManagedNonConcurrentJob extends QuartzManagedJob {
}
