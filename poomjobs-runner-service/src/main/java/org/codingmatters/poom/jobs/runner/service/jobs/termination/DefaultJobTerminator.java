package org.codingmatters.poom.jobs.runner.service.jobs.termination;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;

public class DefaultJobTerminator implements JobTerminator {

    private static final CategorizedLogger log = CategorizedLogger.getLogger(DefaultJobTerminator.class);

    @Override
    public void terminateJob(Job job) throws FailedJobTerminationException {
        // Do nothing
    }

}
