package com.smartface.event;

class EventProcessingException extends RuntimeException {
    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
