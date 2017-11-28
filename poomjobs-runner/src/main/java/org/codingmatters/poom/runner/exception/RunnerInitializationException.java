package org.codingmatters.poom.runner.exception;

public class RunnerInitializationException extends Exception {
    public RunnerInitializationException(String s) {
        super(s);
    }

    public RunnerInitializationException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
