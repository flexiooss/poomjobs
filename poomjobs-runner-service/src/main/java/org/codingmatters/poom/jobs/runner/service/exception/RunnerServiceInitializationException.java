package org.codingmatters.poom.jobs.runner.service.exception;

public class RunnerServiceInitializationException extends Exception {
    public RunnerServiceInitializationException(String message) {
        super(message);
    }

    public RunnerServiceInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
