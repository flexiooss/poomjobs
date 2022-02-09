package org.codingmatters.poom.patterns.pool.exception;

public class NotIdleException extends Exception {
    public NotIdleException(String message) {
        super(message);
    }

    public NotIdleException(String message, Throwable cause) {
        super(message, cause);
    }
}
