package org.codingmatters.poom.jobs.runner.service.jobs.termination;

import org.codingmatters.poomjobs.api.types.Job;

public interface JobTerminator {

    void terminateJob(Job job) throws FailedJobTerminationException;

}
