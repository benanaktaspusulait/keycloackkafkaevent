package com.smartface.exception;

public class EventOutboxException extends Exception {
    public EventOutboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
