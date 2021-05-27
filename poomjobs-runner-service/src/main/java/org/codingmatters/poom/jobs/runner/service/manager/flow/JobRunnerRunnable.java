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
    private final JobConsumer.NextJobSupplier jobSupplier;
    private final JobRunnerStatusStore statusListener;
    private final JobRunnerRunnableErrorListener errorListener;

    private JobConsumer jobConsumer;

    private AtomicReference<Job> jobAssignement = new AtomicReference<>(null);
    private long waitForAssignementTime = 1000L;

    public JobRunnerRunnable(
            RunnerToken token,
            JobProcessorRunner.JobUpdater jobUpdater,
            JobProcessor.Factory processorFactory,
            JobConsumer.NextJobSupplier jobSupplier,
            JobRunnerStatusStore statusListener,
            JobRunnerRunnableErrorListener errorListener,
            JobContextSetup contextSetup
    ) {
        this.token = token;
        this.jobSupplier = jobSupplier;
        this.statusListener = statusListener;
        this.errorListener = errorListener;

        this.jobConsumer = new JobConsumer(new JobProcessorRunner(jobUpdater, processorFactory, contextSetup), this.jobSupplier);
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
        Thread.currentThread().setName(this.token.label());
        log.info("starting job runner thread with token : {}", this.token);
        try {
            this.statusListener.statusFor(this.token, RunnerStatus.BUSY);
            log.info("running available jobs");
            this.runAvailable();
        } catch (JobProcessingException e) {
            this.errorListener.processingExceptionThrown(this.token, e);
        } catch (Exception e) {
            log.error("unrecoverable error thrown while running available jobs by runner with token " + this.token, e);
            this.errorListener.unrecoverableExceptionThrown(e);
            this.inError();
        }
        log.info("now processing jobs as they are assigned");
        while(! this.shuttingDown.get()) {
            try {
                this.runWhenAssigned();
            } catch (JobProcessingException e) {
                this.errorListener.processingExceptionThrown(this.token, e);
            } catch (JobProcessorRunner.JobUpdateFailure | UnregisteredTokenException | InterruptedException e) {
                this.inError();
                log.error("unrecoverable error thrown while running assigned jobs by runner with token " + this.token, e);
                this.errorListener.unrecoverableExceptionThrown(e);
            }
        }
        log.info("stopping job runner thread with token : {}", this.token);
        this.running.set(false);
    }

    private void inError() {
        this.shuttingDown.set(true);
        this.error.set(true);
    }

    public void assign(Job job) {
        synchronized (this.jobAssignement) {
            this.jobAssignement.set(job);
            log.debug("job assigned {}, notifying", job);
            this.jobAssignement.notify();
        }
    }

    private void runWhenAssigned() throws UnregisteredTokenException, JobProcessorRunner.JobUpdateFailure, JobProcessingException, InterruptedException {
        Job job = this.jobAssignement.getAndSet(null);
        if(job != null) {
            this.statusListener.statusFor(this.token, RunnerStatus.BUSY);
            this.jobConsumer.runWith(job);
            this.statusListener.statusFor(this.token, RunnerStatus.IDLE);
        } else {
            this.statusListener.statusFor(this.token, RunnerStatus.IDLE);
            synchronized (this.jobAssignement) {
                this.jobAssignement.wait(this.waitForAssignementTime);
            }
        }
    }

    private void runAvailable() throws JobProcessorRunner.JobUpdateFailure, JobProcessingException {
        Job job = this.jobSupplier.nextJob();
        if(job != null) {
            this.jobConsumer.runWith(job);
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
