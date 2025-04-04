package com.smartface.keycloak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;

@Slf4j
public class EventSerializer {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private EventSerializer() {
        throw new IllegalStateException("Utility class");
    }

    public static String serialize(Event event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize user event", e);
            throw new EventSerializationException("Failed to serialize user event", e);
        }
    }

    public static String serialize(AdminEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize admin event", e);
            throw new EventSerializationException("Failed to serialize admin event", e);
        }
    }

    public static class EventSerializationException extends RuntimeException {
        public EventSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 