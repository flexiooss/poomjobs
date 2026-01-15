package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.LockingFailed;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.UnlockingFailed;
import org.codingmatters.poomjobs.api.types.Job;

public class NOOPJobLocker implements JobLocker {
    @Override
    public Job lock(Job job) throws LockingFailed {
        return job;
    }

    @Override
    public Job release(Job job) throws UnlockingFailed {
        return job;
    }
}
