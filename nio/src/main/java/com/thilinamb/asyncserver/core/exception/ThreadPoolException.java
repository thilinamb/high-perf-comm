package com.thilinamb.asyncserver.core.exception;

/**
 * @author Thilina Buddhika
 */
@SuppressWarnings("unused")
public class ThreadPoolException extends Exception {
    public ThreadPoolException() {
    }

    public ThreadPoolException(String message) {
        super(message);
    }

    public ThreadPoolException(String message, Throwable cause) {
        super(message, cause);
    }
}
