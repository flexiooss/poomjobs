package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.jobs.runner.service.jobs.JobManager;
import org.codingmatters.poom.jobs.runner.service.jobs.JobProcessorRunner;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.LockingFailed;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.UnlockingFailed;
import org.codingmatters.poomjobs.api.types.Job;

public interface JobLocker {
    Job lock(Job job) throws LockingFailed;
    Job release(Job job) throws UnlockingFailed;

    static JobLocker wrapped(JobManager jobManager) {
        return new JobLocker() {
            @Override
            public Job lock(Job job) throws LockingFailed {
                try {
                    return jobManager.reserve(job);
                } catch (JobProcessorRunner.JobUpdateFailure e) {
                    throw new LockingFailed("job manager failed locking job", e);
                }
            }

            @Override
            public Job release(Job job) throws UnlockingFailed {
                try {
                    return jobManager.release(job);
                } catch (JobProcessorRunner.JobUpdateFailure e) {
                    throw new UnlockingFailed("job manager failed unlocking job", e);
                }
            }
        };
    }
}
