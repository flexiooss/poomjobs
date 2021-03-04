package org.codingmatters.poom.jobs.runner.service.exception;

public class UnregisteredTokenException extends Exception {
    public UnregisteredTokenException(String message) {
        super(message);
    }

    public UnregisteredTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
