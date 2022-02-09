package org.codingmatters.poom.jobs.runner.service.manager;

import org.codingmatters.poom.jobs.runner.service.exception.JobNotSubmitableException;
import org.codingmatters.poom.jobs.runner.service.exception.RunnerBusyException;
import org.codingmatters.poom.jobs.runner.service.exception.UnregisteredTokenException;
import org.codingmatters.poom.jobs.runner.service.manager.flow.JobAssignmentExcpetion;
import org.codingmatters.poom.jobs.runner.service.manager.flow.JobProcessorRunner;
import org.codingmatters.poom.jobs.runner.service.manager.flow.JobRunnerRunnable;
import org.codingmatters.poom.jobs.runner.service.manager.jobs.JobManager;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatusChangedListener;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.codingmatters.poomjobs.runner.domain.RunnerToken;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    private AtomicBoolean shouldProcessPending = new AtomicBoolean(false);

    private final BlockingQueue<JobAssignementFuture> awaitingJobAssignment;

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

        this.awaitingJobAssignment = new ArrayBlockingQueue<>(this.concurrentJobCount);
        this.pool = Executors.newFixedThreadPool(this.concurrentJobCount + 2);
        for (int i = 0; i < this.concurrentJobCount; i++) {
            RunnerToken token = this.monitor.addToken();
            JobRunnerRunnable jobRunnerRunnable = new JobRunnerRunnable(
                    token,
                    this.jobManager,
                    this.jobProcessorFactory,
                    this.monitor,
                    this.errorListener,
                    this.contextSetup
            );
            this.runnables.put(token, jobRunnerRunnable);
        }

        this.monitor.addRunnerStatusChangedListener(new RunnerStatusChangedListener() {
            @Override
            public void onIdle(RunnerStatus was) {
                synchronized (shouldProcessPending) {
                    log.debug("runner became idle, should process pending");
                    shouldProcessPending.set(true);
                    shouldProcessPending.notify();
                }
            }

            @Override
            public void onBusy(RunnerStatus was) {
                synchronized (shouldProcessPending) {
                    log.debug("runner became busy, should not process pending");
                    shouldProcessPending.set(false);
                }
            }
        });


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
            this.pool.submit(this::processAssignments);
            this.pool.submit(this::assignPendingJobs);
        }
    }

    private void processAssignement(JobAssignementFuture assignment) {
        try {
            this.assign(assignment.job());
            assignment.complete(JobAssignementFuture.Status.SUCCESS);
        } catch (RunnerBusyException | JobNotSubmitableException | JobProcessorRunner.JobUpdateFailure e) {
            log.error("status changed, cannot assign incoming job", e);
            assignment.complete(JobAssignementFuture.Status.FAILURE);
        }
    }

    private void processAssignments() {
        while(this.running.get()) {
            try {
                JobAssignementFuture assignment = null;
                try {
                    assignment = this.awaitingJobAssignment.poll(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                }
                if (assignment != null) {
                    log.debug("processing assignment :: {}", assignment.job().id());
                    this.processAssignement(assignment);
                }
            } catch(Exception e) {
                log.error("[GRAVE] unexpected exception caught in assignment processing runnable", e);
            }
        }
    }

    private void assignPendingJobs() {
        while(this.running.get()) {
            try {
                if (this.monitor.status().equals(RunnerStatus.IDLE)) {
                    LinkedList<Job> pendingJobs;
                    synchronized (this.shouldProcessPending) {
                        pendingJobs = new LinkedList<>(this.jobManager.pendingJobs().stream().collect(Collectors.toList()));
                        if (pendingJobs.isEmpty()) {
                            try {
                                this.shouldProcessPending.wait(2 * 1000L);
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                    if (!pendingJobs.isEmpty()) {
                        while (!pendingJobs.isEmpty() && this.monitor.status().equals(RunnerStatus.IDLE)) {
                            Job job = pendingJobs.pollFirst();
                            try {
                                this.requestAssignment(job).get();
                            } catch (InterruptedException | ExecutionException e) {
                                log.error("while requesting pending job assignment, got unexpected error", e);
                            } catch (JobNotSubmitableException e) {
                                log.error("[GRAVE] grave pending job is not submittable : " + job, e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[GRAVE] unexpected exception caught in pending job assignment runnable", e);
            }
        }
    }

    public synchronized boolean submit(Job job) throws JobNotSubmitableException {
        JobAssignementFuture assigned = this.requestAssignment(job);
        try {
            JobAssignementFuture.Status assignment = assigned.get();
            return assignment.equals(JobAssignementFuture.Status.SUCCESS);
        } catch (InterruptedException | ExecutionException e) {
            log.error("while requesting submitted job assignment, got unexpected error", e);
        }
        return false;
    }

    private JobAssignementFuture requestAssignment(Job job) throws JobNotSubmitableException {
        if(! Status.Run.PENDING.equals(job.opt().status().run().orElse(null))) {
            throw new JobNotSubmitableException("job should be pending, cannot submit a job with status " + job.opt().status().run().orElse(null));
        }

        JobAssignementFuture result = new JobAssignementFuture(job);
        if(this.pool.isShutdown() || this.pool.isTerminated()) {
            result.complete(JobAssignementFuture.Status.FAILURE);
        } else {
            try {
                this.awaitingJobAssignment.add(result);
            } catch (IllegalStateException e) {
                log.debug("submitted job cannot be assigned, queue full");
                result.complete(JobAssignementFuture.Status.FAILURE);
            }
        }
        return result;
    }

    private boolean assign(Job job) throws RunnerBusyException, JobNotSubmitableException, JobProcessorRunner.JobUpdateFailure{
        log.debug("job being assigned  : {}", job);
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
                        try {
                            job = this.jobManager.reserve(job);
                            runnableEntry.getValue().assign(job);
                            log.debug("assigned job {} to {}", job, runnableEntry.getKey());
                            return true;
                        } catch (JobProcessorRunner.JobUpdateFailure updateFailure) {
                            log.info("cannot set job to running, giving up : {}", job);
                            return false;
                        } catch (JobAssignmentExcpetion jobAssignmentExcpetion) {
                            log.info("while assigning, runnable became busy");
                        }
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
        log.info("shutting down runner pool...");
        if(this.running.getAndSet(false)) {
            for (JobRunnerRunnable runnable : this.runnables.values()) {
                runnable.shutdown();
            }
        }
        pool.shutdown();
        log.info("... runner pool shut down");
    }

    public List<Runnable> shutdownNow() {
        log.info("forcing runner pool shutdown...");
        if(this.running.getAndSet(false)) {
            for (JobRunnerRunnable runnable : this.runnables.values()) {
                runnable.shutdown();
            }
        }
        List<Runnable> result = pool.shutdownNow();
        log.info("runner pool forcibly shut down.");
        return result;
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

    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        return pool.awaitTermination(l, timeUnit);
    }
}
