package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.jobs.runner.service.exception.JobNotSubmitableException;
import org.codingmatters.poom.jobs.runner.service.exception.RunnerBusyException;
import org.codingmatters.poom.jobs.runner.service.jobs.JobProcessorRunner;
import org.codingmatters.poom.jobs.runner.service.jobs.JobManager;
import org.codingmatters.poom.patterns.pool.Feeder;
import org.codingmatters.poom.patterns.pool.FeederPool;
import org.codingmatters.poom.patterns.pool.exception.NotIdleException;
import org.codingmatters.poom.patterns.pool.exception.NotReservedException;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FeederJobProcessorPool {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(FeederJobProcessorPool.class);

    private final JobProcessorRunner.JobUpdater jobUpdater;
    private final JobProcessorRunner.PendingJobManager pendingJobManager;
    private final JobProcessor.Factory factory;
    private final int poolSize;

    private final FeederPool<Job> feeders;
    private final IdleManager idleManager;
    private final IdleManagerRunnable idleManagerRunnable;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final JobContextSetup contextSetup;
    private final JobErrorListener errorListener;

    private ExecutorService pool;

    public interface JobErrorListener {
        void unrecoverableExceptionThrown(Job job, Exception e);
        void processingExceptionThrown(Job job, JobProcessingException e);
    }

    public boolean isIdle() {
        return feeders.isIdle();
    }

    public FeederJobProcessorPool(JobManager jobManager, JobProcessor.Factory factory, JobContextSetup contextSetup, JobErrorListener errorListener, int poolSize) {
        this.jobUpdater = jobManager;
        this.pendingJobManager = jobManager;
        this.factory = factory;
        this.contextSetup = contextSetup;
        this.errorListener = errorListener;
        this.poolSize = poolSize;
        this.feeders = new FeederPool<>(this.poolSize);
        this.idleManager = new IdleManager(this.pendingJobManager, this.feeders);
        this.idleManagerRunnable = new IdleManagerRunnable(this.idleManager);
        this.feeders.addFeederListener(this.idleManagerRunnable);
    }

    public void addPoolListener(FeederPool.Listener listener) {
        this.feeders.addPoolListener(listener);
    }

    public void incomingJob(Job job) throws RunnerBusyException, JobNotSubmitableException, JobProcessorRunner.JobUpdateFailure {
        if(this.running.get()) {
            if(! Status.Run.PENDING.equals(job.opt().status().run().orElse(null))) {
                throw new JobNotSubmitableException("job should be pending, cannot submit a job with status " + job.opt().status().run().orElse(null));
            }
            Feeder.Handle<Job> handle;
            try {
                log.info("processing incoming job : {}", job);
                handle = this.feeders.reserve();
            } catch (NotIdleException e) {
                throw new RunnerBusyException("feeder pool not idle anymore", e);
            }
            try {
                log.info("processing incoming job : {}", job);
                handle.feed(this.pendingJobManager.reserve(job));
            } catch (NotReservedException e) {
                handle.release();
                throw new RunnerBusyException("feeder pool not idle anymore", e);
            }
        } else {
            throw new RunnerBusyException("pool not started");
        }
    }

    private Runnable jobRunner(Feeder.Monitor<Job> monitor) {
        return () -> {
            while(this.running.get()) {
                Job job;
                synchronized (monitor) {
                    job = monitor.in();
                    if(job == null) {
                        try {
                            monitor.wait(100);
                            job = monitor.in();
                        } catch (InterruptedException e) {}
                    }
                }

                if(job != null) {
                    try {
                        log.debug("feeder running job {}  : {}", monitor, job);
                        new JobProcessorRunner(this.jobUpdater, this.factory, this.contextSetup).runWith(job);
                    } catch (JobProcessingException e) {
                        this.errorListener.processingExceptionThrown(job, e);
                    } catch (JobProcessorRunner.JobUpdateFailure e) {
                        this.errorListener.unrecoverableExceptionThrown(job, e);
                    } catch (Exception e) {
                        log.error("[GRAVE] unexpected exception thrown from job processor : " + job, e);
                    } finally {
                        monitor.done();
                        log.debug("feeder processed job {} (running status={}) : {}", monitor, this.running.get(), job);
                    }
                }
            }
            log.debug("job runner runnable stopping :: {}", this.running.get());
        };
    }

    public synchronized void start() {
        if(this.running.get()) {
            return;
        }
        this.running.set(true);

        this.pool = Executors.newFixedThreadPool(this.poolSize + 1);
        for (Feeder.Monitor<Job> monitor : this.feeders.monitors()) {
            this.pool.submit(this.jobRunner(monitor));
        }

        log.info("job processor pool started (pool size : {})", this.poolSize);

        log.info("starting feeding pool from pending jobs");

        this.idleManagerRunnable.start();
        this.pool.submit(this.idleManagerRunnable);
    }

    public synchronized void stop() {
        if(! this.running.get()) {
            return;
        }

        this.running.set(false);
        this.idleManagerRunnable.stop();
        this.pool.shutdown();
        try {
            this.pool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {}

        if(! this.pool.isTerminated()) {
            log.warn("job processor pool not stopped in the shutdown delay, forcing");
            this.pool.shutdownNow();
        }

        log.info("job processor pool stopped");
        this.running.set(false);
    }
}
