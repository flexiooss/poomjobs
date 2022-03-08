package org.codingmatters.poom.pattern.execution.pool.processable.exceptions;

public class LockingFailed extends Exception {
    public LockingFailed(String message) {
        super(message);
    }

    public LockingFailed(String message, Throwable cause) {
        super(message, cause);
    }
}
