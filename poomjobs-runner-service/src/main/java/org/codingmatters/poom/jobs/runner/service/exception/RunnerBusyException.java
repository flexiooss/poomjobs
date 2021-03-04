package org.codingmatters.poom.jobs.runner.service.exception;

public class RunnerBusyException extends Exception {
    public RunnerBusyException(String message) {
        super(message);
    }

    public RunnerBusyException(String message, Throwable cause) {
        super(message, cause);
    }
}
