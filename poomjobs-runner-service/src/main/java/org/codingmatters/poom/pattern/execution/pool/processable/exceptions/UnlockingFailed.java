package org.codingmatters.poom.pattern.execution.pool.processable.exceptions;

public class UnlockingFailed extends Exception {
    public UnlockingFailed(String message) {
        super(message);
    }

    public UnlockingFailed(String message, Throwable cause) {
        super(message, cause);
    }
}
