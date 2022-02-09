package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.jobs.runner.service.manager.flow.JobProcessorRunner;
import org.codingmatters.poom.patterns.pool.Feeder;
import org.codingmatters.poom.patterns.pool.FeederPool;
import org.codingmatters.poom.patterns.pool.exception.NotIdleException;
import org.codingmatters.poom.patterns.pool.exception.NotReservedException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Job;

public class IdleManager {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(IdleManager.class);

    private final JobProcessorRunner.PendingJobManager jobManager;
    private final FeederPool<Job> feeders;

    public IdleManager(JobProcessorRunner.PendingJobManager jobManager, FeederPool<Job> feeders) {
        this.jobManager = jobManager;
        this.feeders = feeders;
    }

    public void becameIdle() {
        while(this.feeders.isIdle()) {
            ValueList<Job> jobs = this.jobManager.pendingJobs();
            if(jobs.isEmpty()) return ;
            Feeder.Handle<Job> handle;
            try {
                handle = this.feeders.reserve();
            } catch (NotIdleException e) {
                log.info("not idle feeder, must have change status");
                return;
            }
            Job reserved = null;
            for (Job pendingJob : jobs) {
                try {
                    reserved = this.jobManager.reserve(pendingJob);
                    break;
                } catch (JobProcessorRunner.JobUpdateFailure e) {
                    log.debug("job changed status, cannot reserve");
                }
            }
            if(reserved != null) {
                try {
                    handle.feed(reserved);
                } catch (NotReservedException e) {
                    log.error("[GRAVE] feeder busy, this one is very strange", e);
                }
            }
        }
    }
}
