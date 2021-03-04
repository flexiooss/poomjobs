package org.codingmatters.poom.jobs.runner.service.exception;

public class JobNotSubmitableException extends Exception {
    public JobNotSubmitableException(String message) {
        super(message);
    }

    public JobNotSubmitableException(String message, Throwable cause) {
        super(message, cause);
    }
}
