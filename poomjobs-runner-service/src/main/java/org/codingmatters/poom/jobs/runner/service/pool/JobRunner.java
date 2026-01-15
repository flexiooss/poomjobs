package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.jobs.runner.service.jobs.JobProcessorRunner;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poomjobs.api.types.Job;

public interface JobRunner {
    void runWith(Job job) throws JobProcessingException, JobProcessorRunner.JobUpdateFailure;
}
