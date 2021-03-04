package org.codingmatters.poom.jobs.runner.service.manager.exception;

public class NotificationFailedException extends Exception {
    public NotificationFailedException(String message) {
        super(message);
    }

    public NotificationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
