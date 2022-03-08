package org.codingmatters.poom.pattern.execution.pool.workers.exceptions;

public class WorkerProcessorException extends Exception {
    public WorkerProcessorException(String message) {
        super(message);
    }

    public WorkerProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
