package org.codingmatters.poom.jobs.runner.service.exception;

public class JobNotReservedException extends Exception {
    public JobNotReservedException(String message) {
        super(message);
    }

    public JobNotReservedException(String message, Throwable cause) {
        super(message, cause);
    }
}
