package org.codingmatters.poom.pattern.execution.pool.exceptions;

public class PoolBusyException extends Exception {
    public PoolBusyException(String message) {
        super(message);
    }

    public PoolBusyException(String message, Throwable cause) {
        super(message, cause);
    }
}
