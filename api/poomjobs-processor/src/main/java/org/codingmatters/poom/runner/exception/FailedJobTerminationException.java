package org.codingmatters.poom.runner.exception;

public class FailedJobTerminationException extends Exception {

    public FailedJobTerminationException(String message) {
        super(message);
    }

    public FailedJobTerminationException(String message, Throwable cause) {
        super(message, cause);
    }
}
