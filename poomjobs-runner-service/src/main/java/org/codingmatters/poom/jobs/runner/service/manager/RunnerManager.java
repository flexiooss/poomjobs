package org.codingmatters.poom.jobs.runner.service.manager;

import org.codingmatters.poom.jobs.runner.service.exception.JobNotSubmitableException;
import org.codingmatters.poom.jobs.runner.service.exception.NotificationFailedException;
import org.codingmatters.poom.jobs.runner.service.exception.RunnerBusyException;
import org.codingmatters.poom.jobs.runner.service.manager.flow.JobProcessorRunner;
import org.codingmatters.poom.jobs.runner.service.manager.flow.JobRunnerRunnable;
import org.codingmatters.poom.jobs.runner.service.manager.jobs.JobManager;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poom.jobs.runner.service.manager.status.RunnerStatusManager;
import org.codingmatters.poom.jobs.runner.service.manager.status.RunnerStatusNotifier;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.codingmatters.poomjobs.runner.domain.RunnerToken;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RunnerManager implements JobRunnerRunnable.JobRunnerRunnableErrorListener {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RunnerManager.class);

    private final RunnerStatusMonitor statusMonitor;
    private final RunnerStatusManager statusManager;
    private final RunnerPool runnerPool;
    private final RunnerStatusNotifier notifier;
    private final JobManager jobManager;
    private final boolean exitOnUnrecoverableError;

    public RunnerManager(
            ScheduledExecutorService scheduler,
            RunnerStatusMonitor runnerStatusMonitor,
            RunnerStatusNotifier notifier,
            long ttl,
            int concurrentJobCount,
            JobManager jobManager,
            JobProcessor.Factory processorFactory,
            JobContextSetup contextSetup,
            boolean exitOnUnrecoverableError
    ) {
        this.statusMonitor = runnerStatusMonitor;
        this.notifier = notifier;
        this.jobManager = jobManager;
        this.exitOnUnrecoverableError = exitOnUnrecoverableError;
        this.statusManager = new RunnerStatusManager(this.notifier, this.statusMonitor, scheduler, ttl * 9 / 10);
        this.runnerPool = new RunnerPool(
                concurrentJobCount,
                jobManager,
                processorFactory,
                contextSetup,
                this,
                this.statusMonitor
        );
    }

    public void submit(Job job) throws RunnerBusyException, JobNotSubmitableException, JobProcessorRunner.JobUpdateFailure {
        this.runnerPool.submit(job);
    }

    public void start() {
        this.runnerPool.start();
        this.runnerPool.awaitReady(30, TimeUnit.SECONDS);
        this.statusManager.start();
    }

    public void shutdown() {
        this.statusManager.stop();
        this.runnerPool.shutdown();
        try {
            this.notifier.notify(RunnerStatus.UNKNOWN);
        } catch (NotificationFailedException e) {
            log.error("failed notifying terminal status");
        }
        try {
            this.runnerPool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("interrupted while waiting runner pool termination");
        }
    }

    @Override
    public void unrecoverableExceptionThrown(Exception e) {
        if(this.exitOnUnrecoverableError) {
            log.error("[GRAVE] unexpected error in runner service, unrecoverable, shutting down", e);
            this.shutdown();
            System.exit(42);
        } else {
            log.error("[GRAVE] unexpected error in runner service, unrecoverable, but, still running as parametrized", e);
        }
    }

    @Override
    public void processingExceptionThrown(RunnerToken token, Job job, JobProcessingException e) {
        log.error("error processing job on runner " + token, e);
        job = job.withStatus(Status.builder().run(Status.Run.DONE).exit(Status.Exit.FAILURE).build());
        try {
            this.jobManager.update(job);
        } catch (JobProcessorRunner.JobUpdateFailure ex) {
            log.error("[GRAVE] failed updating errored job, final status is lost : ", job, e);
        }
    }
}
