package org.codingmatters.poom.jobs.runner.service.manager;

import org.codingmatters.poom.jobs.runner.service.exception.JobNotSubmitableException;
import org.codingmatters.poom.jobs.runner.service.exception.NotificationFailedException;
import org.codingmatters.poom.jobs.runner.service.exception.RunnerBusyException;
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
import org.codingmatters.poomjobs.runner.domain.RunnerToken;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RunnerManager implements JobRunnerRunnable.JobRunnerRunnableErrorListener {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RunnerManager.class);

    private final RunnerStatusMonitor statusMonitor;
    private final RunnerStatusManager statusManager;
    private final RunnerPool runnerPool;
    private final RunnerStatusNotifier notifier = null;

    public RunnerManager(
            String runnerId,
            ScheduledExecutorService scheduler,
            long ttl,
            int concurrentJobCount,
            JobManager jobManager,
            JobProcessor.Factory processorFactory,
            JobContextSetup contextSetup
    ) {
        this.statusMonitor = new RunnerStatusMonitor("job-runner-" + runnerId);
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

    public void submit(Job job) throws RunnerBusyException, JobNotSubmitableException {
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
    public void unexpectedExceptionThrown(RunnerToken token, Exception e) {
        log.error("unexpected error in runner service, unrecoverable, shutting down");
        this.shutdown();
    }

    @Override
    public void processingExceptionThrown(RunnerToken token, JobProcessingException e) {
        log.error("error processing job on runner " + token, e);
    }
}
