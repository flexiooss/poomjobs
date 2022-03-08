package org.codingmatters.poom.pattern.execution.pool.workers;

import org.codingmatters.poom.pattern.execution.pool.workers.exceptions.WorkerProcessorException;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Worker<P> implements Runnable {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(Worker.class);

    enum Status {
        BUSY, IDLE
    }

    class ProcessableWithReason<P> {
        public final P processable;
        public final String reason;

        public ProcessableWithReason(P processable, String reason) {
            this.processable = processable;
            this.reason = reason;
        }
    }

    private final AtomicReference<Status> status = new AtomicReference<>(Status.IDLE);
    private final AtomicReference<ProcessableWithReason<P>> current = new AtomicReference<>(null);
    private final WorkerProcessor<P> workerProcessor;
    private final WorkerListener listener;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public Worker(WorkerProcessor<P> workerProcessor, WorkerListener listener) {
        this.workerProcessor = workerProcessor;
        this.listener = listener;
    }

    public boolean isBusy() {
        return Status.BUSY.equals(this.status.get());
    }

    public boolean submit(P processable, String reason) {
        if(this.status.getAndSet(Status.BUSY) == Status.IDLE) {
            this.listener.busy();
            this.current.set(new ProcessableWithReason<>(processable, reason));
            synchronized (this.current) {
                this.current.notify();
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void run() {
        if(! this.running.getAndSet(true)) {
            while(this.running.get()) {
                synchronized (this.current) {
                    if (this.current.get() == null) {
                        try {
                            this.current.wait(100);
                        } catch (InterruptedException e) {
                            log.error("interrupted while waiting for processable assignment");
                        }
                    }
                    ProcessableWithReason<P> processable = this.current.getAndSet(null);
                    if(processable != null) {
                        try {
                            log.debug("run {} - delegating processable processing : {}", processable.reason, processable.processable);
                            this.workerProcessor.process(processable.processable);
                        } catch (Exception e) {
                            log.error("[GRAVE] unexpected exception while processing " + processable, e);
                        } finally {
                            this.status.set(Status.IDLE);
                            this.listener.idle();
                        }
                    }
                }
            }
        }
    }

    public void stop() {
        this.running.set(false);
    }
}
