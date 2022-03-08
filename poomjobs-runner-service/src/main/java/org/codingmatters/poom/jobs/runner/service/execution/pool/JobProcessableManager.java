package org.codingmatters.poom.jobs.runner.service.execution.pool;

import org.codingmatters.poom.jobs.runner.service.jobs.JobManager;
import org.codingmatters.poom.jobs.runner.service.jobs.JobProcessorRunner;
import org.codingmatters.poom.pattern.execution.pool.processable.ProcessableManager;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.LockingFailed;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.UnlockingFailed;
import org.codingmatters.poomjobs.api.types.Job;

public class JobProcessableManager implements ProcessableManager<Job> {
    private final JobManager jobManager;

    public JobProcessableManager(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public Job lock(Job job) throws LockingFailed {
        try {
            return this.jobManager.reserve(job);
        } catch (JobProcessorRunner.JobUpdateFailure e) {
            throw new LockingFailed("while reserving, failed updating job", e);
        }
    }

    @Override
    public Job release(Job job) throws UnlockingFailed {
        try {
            return this.jobManager.release(job);
        } catch (JobProcessorRunner.JobUpdateFailure e) {
            throw new UnlockingFailed("while releasing, failed updating job", e);
        }
    }
}
