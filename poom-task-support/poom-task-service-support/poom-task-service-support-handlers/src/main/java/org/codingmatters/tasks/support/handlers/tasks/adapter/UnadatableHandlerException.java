package org.codingmatters.tasks.support.handlers.tasks.adapter;

public class UnadatableHandlerException extends Exception {
    public UnadatableHandlerException(String message) {
        super(message);
    }

    public UnadatableHandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
