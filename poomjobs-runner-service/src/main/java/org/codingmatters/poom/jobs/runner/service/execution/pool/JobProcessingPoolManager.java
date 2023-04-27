package org.codingmatters.poom.jobs.runner.service.execution.pool;

import org.codingmatters.poom.jobs.runner.service.RunnerStatusManager;
import org.codingmatters.poom.jobs.runner.service.jobs.JobManager;
import org.codingmatters.poom.pattern.execution.pool.ProcessingPool;
import org.codingmatters.poom.pattern.execution.pool.ProcessingPoolListener;
import org.codingmatters.poom.pattern.execution.pool.WorkerProcessingPool;
import org.codingmatters.poom.pattern.execution.pool.exceptions.PoolBusyException;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.LockingFailed;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.RunningJobPutRequest;
import org.codingmatters.poomjobs.api.RunningJobPutResponse;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Job;

public class JobProcessingPoolManager implements ProcessingPoolListener {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobProcessingPoolManager.class);

    private final JobManager jobManager;
    private final ProcessingPool<Job> pool;
    private final String jobRequestEndpointUrl;
    private final RunnerStatusManager statusManager;

    public JobProcessingPoolManager(
            int poolSize,
            JobManager jobManager,
            JobProcessor.Factory processorFactory,
            JobContextSetup contextSetup,
            String jobRequestEndpointUrl,
            RunnerStatusManager statusManager
    ) {
        this.jobManager = jobManager;
        this.jobRequestEndpointUrl = jobRequestEndpointUrl;
        this.statusManager = statusManager;
        this.pool = new WorkerProcessingPool<>(
                poolSize,
                new JobProcessableManager(this.jobManager),
                new JobWorkerProcessor(this.jobManager, processorFactory, contextSetup),
                this
                );
    }

    @Override
    public void accepting() {
        this.processPendingJobs();
        if(this.pool.status().equals(ProcessingPool.Status.ACCEPTING)) {
            this.statusManager.becameIdle();
        }
    }

    @Override
    public void full() {
        this.statusManager.becameBusy();
    }

    public RunningJobPutResponse jobExecutionRequested(RunningJobPutRequest request) {
        log.debug("job execution requested : {}", request);
        Job job = request.payload();
        try {
            this.process(job, "incoming job");
        } catch (LockingFailed e) {
            return RunningJobPutResponse.builder().status409(status -> status.payload(error -> error
                    .code(Error.Code.ENTITY_UPDATE_NOT_ALLOWED)
                    .token(log.tokenized().info("cannot reserve job : " + job, e))
                    .description("failed reserving job")
            )).build();
        } catch (PoolBusyException e) {
            return RunningJobPutResponse.builder().status409(status -> status.payload(error -> error
                    .code(Error.Code.OVERLOADED)
                    .token(log.tokenized().info("runner became busy for job " + job, e))
                    .description("runner busy, come back later")
            )).build();
        }
        return RunningJobPutResponse.builder().status201(status -> status
                .location("%s/%s", this.jobRequestEndpointUrl, job.id())
        ).build();
    }

    private void processPendingJobs() {
        while(this.pool.status().equals(ProcessingPool.Status.ACCEPTING)) {
            ValueList<Job> jobs = this.jobManager.pendingJobs();
            if(jobs.isEmpty()) {
                return;
            }
            for (Job pendingJob : jobs) {
                try {
                    this.pool.process(pendingJob, "pending job");
                } catch (LockingFailed e) {
                    log.debug("process pending job - cannot lock job, ignoring", e);
                } catch (PoolBusyException e) {
                    log.debug("process pending job - pool became busy, stopping assigning pending", e);
                    return;
                }
            }
        }
    }

    private void process(Job job, String reason) throws LockingFailed, PoolBusyException {
        this.pool.process(job, reason);
    }

    public void start() {
        this.pool.start();
        this.statusManager.becameIdle();
        this.processPendingJobs();
    }

    public void stop(long timeout) {
        this.pool.stop(timeout);
    }
}
