package org.codingmatters.poom.pattern.execution.pool;

import org.codingmatters.poom.pattern.execution.pool.exceptions.PoolBusyException;
import org.codingmatters.poom.pattern.execution.pool.processable.ProcessableManager;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.LockingFailed;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.UnlockingFailed;
import org.codingmatters.poom.pattern.execution.pool.workers.Worker;
import org.codingmatters.poom.pattern.execution.pool.workers.WorkerListener;
import org.codingmatters.poom.pattern.execution.pool.workers.WorkerProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerProcessingPool<P> implements ProcessingPool<P>, WorkerListener {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(WorkerProcessingPool.class);

    private final int poolSize;
    private final AtomicInteger workingCount = new AtomicInteger(0);

    private final List<Worker<P>> workers = new LinkedList<>();

    private final ProcessableManager<P> manager;
    private final ProcessingPoolListener listener;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public WorkerProcessingPool(int poolSize, ProcessableManager<P> manager, WorkerProcessor<P> workerProcessor, ProcessingPoolListener listener) {
        this.poolSize = poolSize;
        this.manager = manager;
        this.listener = listener;
        for (int i = 0; i < poolSize; i++) {
            this.workers.add(new Worker<>(workerProcessor, this));
        }
    }

    public int poolSize() {
        return poolSize;
    }

    public AtomicInteger workingCount() {
        return workingCount;
    }

    @Override
    public void process(P p, String reason) throws LockingFailed, PoolBusyException {
        P locked = this.manager.lock(p);
        log.debug("processable locked : {}", locked);

        boolean submitted = false;
        Iterator<Worker<P>> wks = this.workers.iterator();
        while (wks.hasNext() && !submitted) {
            submitted = wks.next().submit(locked, reason);
        }

        if (submitted) {
            log.debug("process {} - processable submitted : {}", reason, locked);
        } else {
            try {
                log.debug("process {} - processable submission failed, will release : {}", reason, locked);
                p = this.manager.release(locked);
                log.debug("process {} - processable released : {}", reason, locked);
            } catch (UnlockingFailed e) {
                log.error("[GRAVE] process " + reason + " - failed unlocking processable " + locked, e);
            }
            throw new PoolBusyException("all workers are busy");
        }
    }

    @Override
    public Status status() {
        if (running.get()) {
            return this.workingCount.get() >= this.poolSize ? Status.FULL : Status.ACCEPTING;
        } else {
            return Status.FULL;
        }
    }

    @Override
    public void busy() {
        if (this.workingCount.incrementAndGet() == this.poolSize) {
            this.listener.full();
        }
    }

    @Override
    public void idle() {
        if (this.workingCount.decrementAndGet() == this.poolSize - 1) {
            this.listener.accepting();
        }
    }


    private ExecutorService pool;
    private Map<Worker, Future> workerTasks = new HashMap<>();

    @Override
    public void start() {
        if (!this.running.getAndSet(true)) {
            this.pool = Executors.newFixedThreadPool(this.poolSize, runnable -> new Thread(new ThreadGroup("runner-job-processing-workers"), runnable));
            for (Worker worker : this.workers) {
                this.pool.submit(worker);
            }
        }
    }

    @Override
    public void stop(long timeout) {
        try {
            log.info("Stopping Worker Pool");
            if (this.running.getAndSet(false)) {
                log.info("Stop all workers");
                for (Worker<P> worker : this.workers) {
                    worker.stop();
                    Future task = this.workerTasks.get(worker);
                    if (task != null) {
                        task.cancel(true);
                    }
                }
                log.info("shutdown pool");
                this.pool.shutdown();
                try {
                    log.info("Stopping workers " + timeout + " ms");
                    boolean terminated = this.pool.awaitTermination(timeout, TimeUnit.MILLISECONDS);
                    log.info("Pool terminated after timeout: " + terminated);
                } catch (Exception e) {
                    log.error("Interrupted while awaiting shutdown", e);
                }
                if (!this.pool.isTerminated()) {
                    log.info("Force pool shutdown now");
                    this.pool.shutdownNow();
                }
            } else {
                log.info("Not running");
            }
            log.info("Worker pool stopped");
        } catch (Throwable t) {
            log.error("Error shutting down", t);
        }
    }
}
