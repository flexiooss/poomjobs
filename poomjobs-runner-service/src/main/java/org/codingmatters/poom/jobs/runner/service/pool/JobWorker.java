package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.jobs.runner.service.jobs.JobProcessorRunner;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.LockingFailed;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class JobWorker implements Runnable {
    static private CategorizedLogger log = CategorizedLogger.getLogger(JobWorker.class);

    static private final long WAIT_TIMEOUT = Env.optional("JOB_WORKER_WAIT_TIMEOUT").orElse(new Env.Var("1000")).asLong();

    private final PendingWorkers pendingWorkers;
    private final JobRunner jobRunner;
    private final JobLocker jobLocker;

    private AtomicReference<Job> currentJob = new AtomicReference<>();
    private AtomicBoolean running = new AtomicBoolean(true);
    private AtomicBoolean stopped = new AtomicBoolean(false);

    public JobWorker(PendingWorkers pendingWorkers, JobRunner jobRunner, JobLocker jobLocker) {
        this.pendingWorkers = pendingWorkers;
        this.jobRunner = jobRunner;
        this.jobLocker = jobLocker;
    }

    public void submit(Job job) {
        log.debug("submitting job {}", job.name());
        synchronized (this.currentJob) {
            this.currentJob.set(job);
            this.currentJob.notify();
        }
    }

    public void stop() {
        this.running.set(false);
    }

    public boolean stopped() {
        return this.stopped.get();
    }

    @Override
    public void run() {
        this.currentJob.set(null);
        while(this.running.get()) {
            synchronized (this.currentJob) {
                this.pendingWorkers.idle(this);
                try {
                    this.currentJob.wait(WAIT_TIMEOUT);
                } catch (InterruptedException e) {
                    log.warn("job worker interrupted while waiting", e);
                }
                Job job = this.currentJob.getAndSet(null);
                if(job != null) {
                    log.debug("running job {}", job.name());
                    this.process(job);
                }
            }
        }
        this.stopped.set(true);
    }

    private void process(Job job) {
        try {
            job = this.jobLocker.lock(job);
        } catch (LockingFailed e) {
            log.error("job locking failed : " + job, e);
            return;
        }
        try {
            log.info("processing job {}", job.name());
            this.jobRunner.runWith(job);
        } catch (JobProcessingException e) {
            log.error("[GRAVE] job processing exception : " + job.withStatus((Status) null), e);
        } catch (JobProcessorRunner.JobUpdateFailure e) {
            log.error("[GRAVE] job was executed, but got update failure, job final status may be wrong : " + job.withStatus((Status) null), e);
        } catch (Throwable e) {
            log.error("[GRAVE] an unexpected exception was thrown while executing job, should be catched, job final status may be wrong : " + job.withStatus((Status) null), e);
        }
    }
}
