package com.smartface.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartface.event.entity.EventEntity;
import com.smartface.event.repository.EventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import io.quarkus.logging.Log;

import java.time.Instant;

@Slf4j
@ApplicationScoped
public class EventConsumer {

    private static final String EVENT_IN_CHANNEL = "keycloak-events-in";

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public EventConsumer(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Incoming(EVENT_IN_CHANNEL)
    @Transactional
    public void consume(String eventJson) {
        try {
            processEvent(eventJson);
        } catch (Exception e) {
            handleProcessingException(e, eventJson);
        }
    }

    private void processEvent(String eventJson) throws JsonProcessingException {
        Log.info("Received event: {}");
        JsonNode eventNode = objectMapper.readTree(eventJson);
        EventEntity event = mapEventNodeToEntity(eventNode);
        eventRepository.persist(event);
        log.info("Successfully persisted event with id: {}", event.getId());
    }

    private EventEntity mapEventNodeToEntity(JsonNode eventNode) {
        EventEntity event = new EventEntity();
        event.setId(eventNode.get("id").asText());
        event.setTime(getEventTime(eventNode));
        event.setType(eventNode.get("type").asText());
        event.setRealmId(eventNode.get("realmId").asText());

        event.setClientId(getOptionalField(eventNode, "clientId"));
        event.setUserId(getOptionalField(eventNode, "userId"));
        event.setSessionId(getOptionalField(eventNode, "sessionId"));
        event.setIpAddress(getOptionalField(eventNode, "ipAddress"));
        event.setError(getOptionalField(eventNode, "error"));
        event.setDetails(getOptionalField(eventNode, "details"));

        return event;
    }

    private Instant getEventTime(JsonNode eventNode) {
        return eventNode.has("time") ? Instant.ofEpochMilli(eventNode.get("time").asLong()) : Instant.now();
    }

    private String getOptionalField(JsonNode eventNode, String field) {
        return eventNode.has(field) ? eventNode.get(field).asText() : null;
    }

    private void handleProcessingException(Exception e, String eventJson) {
        log.error("Error processing event: {}", e.getMessage(), e);
        throw new EventProcessingException("Failed to process event: " + eventJson, e);
    }
}