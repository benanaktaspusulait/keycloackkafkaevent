package com.smartface.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class EventConsumer {
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public EventConsumer(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Incoming("keycloak_events")
    @Transactional
    public void consume(String eventJson) {
        try {
            log.info("Received event: {}", eventJson);
            JsonNode eventNode = objectMapper.readTree(eventJson);
            
            EventEntity event = new EventEntity();
            event.setId(eventNode.get("id").asText());
            event.setTime(Instant.ofEpochMilli(eventNode.get("time").asLong()));
            event.setType(eventNode.get("type").asText());
            event.setRealmId(eventNode.get("realmId").asText());
            
            if (eventNode.has("clientId")) {
                event.setClientId(eventNode.get("clientId").asText());
            }
            if (eventNode.has("userId")) {
                event.setUserId(eventNode.get("userId").asText());
            }
            if (eventNode.has("sessionId")) {
                event.setSessionId(eventNode.get("sessionId").asText());
            }
            if (eventNode.has("ipAddress")) {
                event.setIpAddress(eventNode.get("ipAddress").asText());
            }
            if (eventNode.has("error")) {
                event.setError(eventNode.get("error").asText());
            }
            
            // Handle details map
            if (eventNode.has("details")) {
                Map<String, String> details = new HashMap<>();
                JsonNode detailsNode = eventNode.get("details");
                Iterator<Map.Entry<String, JsonNode>> fields = detailsNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    details.put(field.getKey(), field.getValue().asText());
                }
                event.setDetails(details);
            }

            eventRepository.persist(event);
            log.info("Successfully persisted event with id: {}", event.getId());
        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
            throw new EventProcessingException("Failed to process event", e);
        }
    }
}

