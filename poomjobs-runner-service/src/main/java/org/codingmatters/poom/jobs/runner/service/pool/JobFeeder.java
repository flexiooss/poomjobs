package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.jobs.runner.service.jobs.JobProcessorRunner;
import org.codingmatters.poom.pattern.execution.pool.exceptions.PoolBusyException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Job;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class JobFeeder implements JobPoolListener {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobFeeder.class);

    private final JobPool jobPool;
    private final JobProcessorRunner.PendingJobManager pendingJobManager;
    private final ExecutorService pool;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final AtomicReference<State> state = new AtomicReference<>(State.ACCEPTING);

    public JobFeeder(JobPool jobPool, JobProcessorRunner.PendingJobManager pendingJobManager) {
        this.jobPool = jobPool;
        this.jobPool.addJobPoolListener(this);
        this.pendingJobManager = pendingJobManager;
        this.pool = Executors.newSingleThreadExecutor(runnable -> new Thread(new ThreadGroup("job-feeder"), runnable));
        this.pool.submit(this::feederLoop);
        log.info("job feeder starting...");
    }

    public void stop() {
        log.debug("stopping job feeder...");
        this.running.set(false);
        synchronized (this.state) {
            this.state.set(State.STOPPING);
            this.state.notifyAll();
            log.debug("job feeder stop requested.");
        }
    }

    public boolean stopped() {
        return this.stopped.get();
    }

    private void feederLoop() {
        log.info("job feeder started.");
        int fullCycles = 0;
        while(this.running.get()) {
            this.feederCycle();
            if(this.state.get() == State.FULL) {
                fullCycles++;
            } else {
                fullCycles = 0;
            }
            if(fullCycles > 100) {
                // try feed have we missed an event
                this.feed();
                fullCycles = 0;
            }
        }
        log.info("job feeder stopping...");
        this.stopped.set(true);
        this.pool.shutdown();
        log.info("job feeder stopped");
    }

    private void feederCycle() {
        //log.debug("feeder loop start {}", this.state.get());
        if (!State.ACCEPTING.equals(this.state.get())) {
            synchronized (this.state) {
                if (!State.ACCEPTING.equals(this.state.get())) {
                    try {
                        //log.debug("feeder waits a while on state : {}", this.state.get());
                        this.state.wait(100);
                        //log.debug("feeder wakes up with state : {}", this.state.get());
                    } catch (InterruptedException e) {
                        log.error("interrupted while waiting for state change", e);
                    }
                }
            }
        } else {
            log.debug("feeder feeding {}", this.state.get());
            this.feed();
        }
        //log.debug("feeder loop end {}", this.state.get());
    }

    private void feed() {
        ValueList<Job> jobs = this.pendingJobManager.pendingJobs();
        if (jobs.isEmpty()) {
            synchronized (this.state) {
                log.debug("nothing to feed from, sleeping");
                this.state.set(State.FULL.equals(this.state.get()) ? State.FULL : State.SLEEPING);
            }
        } else {
            log.debug("feeding from {}", jobs);
            for (Job job : jobs) {
                try {
                    this.jobPool.feed(job);
                } catch (PoolBusyException e) {
                    log.debug("pool is full, stop feeding it");
                    break;
                }
            }
        }
    }

    @Override
    public void poolIsFull() {
        synchronized (this.state) {
            this.state.set(State.FULL);
            log.debug("pool is full");
        }
    }

    @Override
    public void poolIsAcceptingJobs() {
        synchronized (this.state) {
            this.state.set(State.ACCEPTING);
            log.debug("pool is accepting jobs");
            this.state.notify();
        }
    }

    enum State {
        ACCEPTING,
        SLEEPING,
        FULL,
        STOPPING
    }
}
