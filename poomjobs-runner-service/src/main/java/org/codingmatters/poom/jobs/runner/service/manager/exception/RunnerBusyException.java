package org.codingmatters.poom.jobs.runner.service.manager.exception;

public class RunnerBusyException extends Exception {
    public RunnerBusyException(String message) {
        super(message);
    }

    public RunnerBusyException(String message, Throwable cause) {
        super(message, cause);
    }
}
