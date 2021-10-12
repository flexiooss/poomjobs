package org.codingmatters.poom.jobs.runner.service.manager;

import org.codingmatters.poom.jobs.runner.service.exception.JobNotSubmitableException;
import org.codingmatters.poom.jobs.runner.service.exception.RunnerBusyException;
import org.codingmatters.poom.jobs.runner.service.exception.UnregisteredTokenException;
import org.codingmatters.poom.jobs.runner.service.manager.flow.JobProcessorRunner;
import org.codingmatters.poom.jobs.runner.service.manager.flow.JobRunnerRunnable;
import org.codingmatters.poom.jobs.runner.service.manager.jobs.JobManager;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.codingmatters.poomjobs.runner.domain.RunnerToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RunnerPool {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RunnerPool.class);

    private final ExecutorService pool;
    private final int concurrentJobCount;
    private final RunnerStatusMonitor monitor;
    private final JobProcessor.Factory jobProcessorFactory;
    private final JobManager jobManager;
    private final JobRunnerRunnable.JobRunnerRunnableErrorListener errorListener;
    private final JobContextSetup contextSetup;

    private final Map<RunnerToken, JobRunnerRunnable> runnables = new HashMap<>();
    private AtomicBoolean running = new AtomicBoolean(false);

    public RunnerPool(
            int concurrentJobCount,
            JobManager jobManager,
            JobProcessor.Factory jobProcessorFactory,
            JobContextSetup contextSetup,
            JobRunnerRunnable.JobRunnerRunnableErrorListener errorListener,
            RunnerStatusMonitor monitor) {
        this.concurrentJobCount = concurrentJobCount;
        this.jobManager = jobManager;
        this.jobProcessorFactory = jobProcessorFactory;
        this.contextSetup = contextSetup;
        this.errorListener = errorListener;
        this.monitor = monitor;

        this.pool = Executors.newFixedThreadPool(this.concurrentJobCount);
        for (int i = 0; i < this.concurrentJobCount; i++) {
            RunnerToken token = this.monitor.addToken();
            JobRunnerRunnable jobRunnerRunnable = new JobRunnerRunnable(
                    token,
                    this.jobManager,
                    this.jobProcessorFactory,
                    this.jobManager,
                    this.monitor,
                    this.errorListener,
                    this.contextSetup
            );
            this.runnables.put(token, jobRunnerRunnable);

        }
    }


    public void start() {
        synchronized (this.running) {
            if(this.running.getAndSet(true)) {
                log.warn("useless call to start on runner pool, already running");
                return;
            }
            for (JobRunnerRunnable runnable : this.runnables.values()) {
                this.pool.submit(runnable);
            }
        }
    }

    public boolean awaitReady(long timeout, TimeUnit unit) {
        long start = System.currentTimeMillis();

        while(System.currentTimeMillis() - start < unit.toMillis(timeout)) {
            boolean ready = true;
            for (RunnerToken token : this.runnables.keySet()) {
                try {
                    if (RunnerStatus.UNKNOWN.equals(this.monitor.status(token))) {
                        ready = false;
                        break;
                    }
                } catch (UnregisteredTokenException e) {
                    throw new RuntimeException("unrecoverable error", e);
                }
            }
            if (ready) {
                return true;
            } else {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new RuntimeException("interrupted while waiting for ready", e);
                }
            }
        }
        return false;
    }

    public synchronized void submit(Job job) throws RunnerBusyException, JobNotSubmitableException, JobProcessorRunner.JobUpdateFailure {
        if(this.pool.isShutdown()) {
            throw new RunnerBusyException("runner pool is shutting down, cannot execute job");
        }
        if(this.pool.isTerminated()) {
            throw new RunnerBusyException("runner pool is terminated, cannot execute job");
        }
        if(! Status.Run.PENDING.equals(job.opt().status().run().orElse(null))) {
            throw new JobNotSubmitableException("job should be pending, cannot submit a job with status " + job.opt().status().run().orElse(null));
        }
        try {
            if (this.monitor.status().equals(RunnerStatus.BUSY)) {
                throw new RunnerBusyException("all runner threads are busy, cannot execute job");
            } else {
                for (Map.Entry<RunnerToken, JobRunnerRunnable> runnableEntry : this.runnables.entrySet()) {
                    if (this.monitor.status(runnableEntry.getKey()).equals(RunnerStatus.IDLE)) {
                        job = job.withStatus(Status.builder().run(Status.Run.RUNNING).build());
                        job = this.jobManager.update(job);
                        runnableEntry.getValue().assign(job);
                        log.debug("assigned job {} to {}", job, runnableEntry.getKey());
                        return;
                    }
                }
                throw new RunnerBusyException("no runner threads accepted job, assuming busy, cannot execute job");
            }
        } catch (UnregisteredTokenException e) {
            log.error("job runner token not registered, this should not occur, this is not recoverable", e);
            throw new RuntimeException("unrecoverable exception", e);
        }
    }

    public boolean running() {
        return this.running.get();
    }

    public void shutdown() {
        if(this.running.getAndSet(false)) {
            for (JobRunnerRunnable runnable : this.runnables.values()) {
                runnable.shutdown();
            }
        }
        pool.shutdown();
    }

    public List<Runnable> shutdownNow() {
        if(this.running.getAndSet(false)) {
            for (JobRunnerRunnable runnable : this.runnables.values()) {
                runnable.shutdown();
            }
        }
        return pool.shutdownNow();
    }

    public boolean isShutdown() {
        return pool.isShutdown();
    }

    public boolean isTerminated() {
        return pool.isTerminated();
    }

    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        return pool.awaitTermination(l, timeUnit);
    }
}
