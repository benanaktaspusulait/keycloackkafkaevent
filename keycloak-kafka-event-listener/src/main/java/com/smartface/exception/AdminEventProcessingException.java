package com.smartface.exception;

// Custom exception class for admin events
public class AdminEventProcessingException extends RuntimeException {
    public AdminEventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
