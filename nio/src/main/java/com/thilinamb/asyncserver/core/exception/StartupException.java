package com.thilinamb.asyncserver.core.exception;

/**
 * @author Thilina Buddhika
 */
@SuppressWarnings("unused")
public class StartupException extends Exception{
    public StartupException() {
    }

    public StartupException(String message) {
        super(message);
    }

    public StartupException(String message, Throwable cause) {
        super(message, cause);
    }
}
