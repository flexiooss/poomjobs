package org.codingmatters.poom.pattern.execution.pool;

import org.codingmatters.poom.pattern.execution.pool.exceptions.PoolBusyException;
import org.codingmatters.poom.pattern.execution.pool.processable.ProcessableManager;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.LockingFailed;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.UnlockingFailed;
import org.codingmatters.poom.pattern.execution.pool.workers.Worker;
import org.codingmatters.poom.pattern.execution.pool.workers.WorkerListener;
import org.codingmatters.poom.pattern.execution.pool.workers.WorkerProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
            this.workers.add(new Worker(workerProcessor, this));
        }
    }

    @Override
    public void process(P p, String reason) throws LockingFailed, PoolBusyException {
        P locked = this.manager.lock(p);
        log.debug("processable locked : {}", locked);

        boolean submitted = false;
        Iterator<Worker<P>> wks = this.workers.iterator();
        while(wks.hasNext() && ! submitted) {
            submitted = wks.next().submit(locked, reason);
        }

        if(submitted) {
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
        return this.workingCount.get() >= this.poolSize ? Status.FULL : Status.ACCEPTING;
    }

    @Override
    public void busy() {
        if(this.workingCount.incrementAndGet() == this.poolSize) {
            this.listener.full();
        }
    }

    @Override
    public void idle() {
        if(this.workingCount.decrementAndGet() == this.poolSize - 1) {
            this.listener.accepting();
        }
    }


    private ExecutorService pool;
    @Override
    public void start() {
        if(! this.running.getAndSet(true)) {
            this.pool = Executors.newFixedThreadPool(this.poolSize);
            for (Worker worker : this.workers) {
                this.pool.submit(worker);
            }
        }
    }

    @Override
    public void stop(long timeout) {
        if(this.running.getAndSet(false)) {
            this.workers.forEach(w -> w.stop());
            this.pool.shutdown();
            try {
                this.pool.awaitTermination(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error("interrupted while awaiting shutdown");
            }
            if(! this.pool.isTerminated()) {
                this.pool.shutdownNow();
            }
        }
    }
}
