package org.codingmatters.poom.jobs.runner.service.manager.flow;

import org.codingmatters.poom.jobs.runner.service.exception.UnregisteredTokenException;
import org.codingmatters.poom.jobs.runner.service.manager.JobRunnerStatusStore;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.runner.domain.RunnerToken;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class JobRunnerRunnable implements Runnable {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobProcessorRunner.class);

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean error = new AtomicBoolean(false);

    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private final RunnerToken token;
    private final JobRunnerStatusStore statusListener;
    private final JobRunnerRunnableErrorListener errorListener;
    private final JobProcessorRunner jobRunner;


    private final AtomicReference<Job> jobAssignement = new AtomicReference<>(null);
    private final long waitForAssignementTime = 1000L;

    public JobRunnerRunnable(
            RunnerToken token,
            JobProcessorRunner.JobUpdater jobUpdater,
            JobProcessor.Factory processorFactory,
            JobRunnerStatusStore statusListener,
            JobRunnerRunnableErrorListener errorListener,
            JobContextSetup contextSetup
    ) {
        this.token = token;
        this.statusListener = statusListener;
        this.errorListener = errorListener;

        this.jobRunner = new JobProcessorRunner(jobUpdater, processorFactory, contextSetup);
    }

    public void shutdown() {
        this.shuttingDown.set(true);
    }

    public boolean running() {
        return this.running.get();
    }

    public boolean error() {
        return this.error.get();
    }

    @Override
    public void run() {
        this.running.set(true);
        while(! this.shuttingDown.get()) {
            Job job;
            synchronized (this.jobAssignement) {
                job = this.jobAssignement.getAndSet(null);
                try {
                    if (job != null) {
                        this.statusListener.statusFor(this.token, RunnerStatus.BUSY);
                    } else {
                        this.statusListener.statusFor(this.token, RunnerStatus.IDLE);
                        try {
                            this.jobAssignement.wait(this.waitForAssignementTime);
                        } catch (InterruptedException e) {
                        }
                    }
                } catch (UnregisteredTokenException e) {
                    log.error("unrecoverable error thrown while waiting for assignment by runner with token " + this.token, e);
                    this.errorListener.unrecoverableExceptionThrown(e);
                }
            }
            if(job != null) {
                this.runJob(job);
            }
        }
        this.running.set(false);
    }

    private void runJob(Job job) {
        try {
            this.jobRunner.runWith(job);
        } catch (JobProcessingException e) {
            log.error("job processing exception, notifying", e);
            this.errorListener.processingExceptionThrown(this.token, e);
        } catch (JobProcessorRunner.JobUpdateFailure e) {
            log.error("unrecoverable error thrown while running assigned jobs by runner with token " + this.token, e);
            this.errorListener.unrecoverableExceptionThrown(e);
        } catch (Exception e) {
            log.error("[GRAVE] unexpected exception thrown from job processor : " + job, e);
        }
    }

    public void assign(Job job) throws JobAssignmentExcpetion {
        synchronized (this.jobAssignement) {
            if(this.jobAssignement.get() == null);
            this.jobAssignement.set(job);
            log.debug("job assigned {}, notifying", job);
            this.jobAssignement.notify();
        }
    }


    public interface JobRunnerRunnableErrorListener {
        void unrecoverableExceptionThrown(Exception e);
        void processingExceptionThrown(RunnerToken token, JobProcessingException e);

        JobRunnerRunnableErrorListener NOOP = new JobRunnerRunnableErrorListener() {
            @Override
            public void unrecoverableExceptionThrown(Exception e) {}

            @Override
            public void processingExceptionThrown(RunnerToken token, JobProcessingException e) {}
        };
    }
}
