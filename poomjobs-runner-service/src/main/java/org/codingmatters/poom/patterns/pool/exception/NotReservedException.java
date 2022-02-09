package org.codingmatters.poom.patterns.pool.exception;

public class NotReservedException extends Exception {
    public NotReservedException(String message) {
        super(message);
    }

    public NotReservedException(String message, Throwable cause) {
        super(message, cause);
    }
}
