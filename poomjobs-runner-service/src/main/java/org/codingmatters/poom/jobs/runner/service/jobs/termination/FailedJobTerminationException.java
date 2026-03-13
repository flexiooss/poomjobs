package org.codingmatters.poom.jobs.runner.service.jobs.termination;

public class FailedJobTerminationException extends Exception {

    public FailedJobTerminationException(String message) {
        super(message);
    }

    public FailedJobTerminationException(String message, Throwable cause) {
        super(message, cause);
    }
}
