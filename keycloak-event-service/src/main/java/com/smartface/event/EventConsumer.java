package com.smartface.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.time.Instant;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class EventConsumer {
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Incoming("keycloak-events")
    public void consume(Record<String, String> eventRecord) {
        try {
            log.info("Received event: key={}, value={}", eventRecord.key(), eventRecord.value());
            
            // Parse the event JSON
            EventEntity event = objectMapper.readValue(eventRecord.value(), EventEntity.class);
            event.setTime(Instant.now());
            
            // Persist the event
            eventRepository.persist(event);
            
            log.info("Event persisted successfully: {}", event.getId());
        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
        }
    }
} 