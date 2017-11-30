package org.codingmatters.poom.runner.exception;

public class JobProcessingException extends Exception {
    public JobProcessingException(String s) {
        super(s);
    }

    public JobProcessingException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
