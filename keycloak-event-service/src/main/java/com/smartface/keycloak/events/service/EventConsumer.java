package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.logging.Logger;

@ApplicationScoped
public class EventConsumer {

    private static final Logger logger = Logger.getLogger(EventConsumer.class.getName());
    private final EventService eventService;

    @Inject
    public EventConsumer(EventService eventService) {
        this.eventService = eventService;
    }

    @Transactional
    public void consume(String eventId, String eventType, String details) {
        logger.info("Consuming event with ID: " + eventId);

        EventOutbox event = createEventOutbox(eventId, eventType, details);

        try {
            logger.info("Processing event with ID: " + event.getEventId() + ", type: " + event.getEventType());
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
            logger.info("Successfully processed event with ID: " + event.getEventId());
        } catch (Exception e) {
            logger.severe("Error processing event with ID: " + event.getEventId() + ": " + e.getMessage());
            throw e;
        }
    }

    private EventOutbox createEventOutbox(String eventId, String eventType, String details) {
        EventOutbox event = new EventOutbox();
        event.setEventId(eventId);
        event.setEventType( eventType);
        event.setDetails(details);
        event.setStatus( EventStatus.PENDING);
        event.setRetryCount(0);
        event.setCreatedAt(Instant.now());
        return event;
    }
}
