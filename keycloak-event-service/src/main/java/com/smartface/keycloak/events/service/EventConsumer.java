package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@ApplicationScoped
public class EventConsumer {

    private final EventService eventService;

    @Inject
    public EventConsumer(EventService eventService) {
        this.eventService = eventService;
    }

    @Transactional
    public void consume(String eventId, String eventType, String details) {
        log.info("Consuming event with ID: {}", eventId);

        EventOutbox event = createEventOutbox(eventId, eventType, details);

        try {
            log.info("Processing event with ID: {}, type: {}", event.getEventId(), event.getEventType());
            eventService.processEvent(
                    event.getEventId(),
                    event.getEventType(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    event.getDetails()
            );
            log.info("Successfully processed event with ID: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Error processing event with ID: {}", event.getEventId(), e);
            throw e;
        }
    }

    private EventOutbox createEventOutbox(String eventId, String eventType, String details) {
        EventOutbox event = new EventOutbox();
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setDetails(details);
        event.setStatus(EventStatus.PENDING);
        event.setRetryCount(0);
        event.setCreatedAt(Instant.now());
        return event;
    }
}
