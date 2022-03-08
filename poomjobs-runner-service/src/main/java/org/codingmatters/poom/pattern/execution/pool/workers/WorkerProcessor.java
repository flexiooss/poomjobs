package org.codingmatters.poom.pattern.execution.pool.workers;

import org.codingmatters.poom.pattern.execution.pool.workers.exceptions.WorkerProcessorException;

@FunctionalInterface
public interface WorkerProcessor<P> {
    void process(P processable) throws WorkerProcessorException;
}
