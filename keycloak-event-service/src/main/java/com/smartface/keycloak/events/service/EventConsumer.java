package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import com.smartface.keycloak.grpc.EventRequest;
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
        logger.info(() -> String.format("Consuming event with ID: %s, type: %s", eventId, eventType));

        EventOutbox event = createEventOutbox(eventId, eventType, details);

        try {
            logger.info(() -> String.format("Processing event with ID: %s, type: %s", event.getEventId(), event.getEventType()));

            EventRequest eventRequest = EventRequest.newBuilder()
                        .setEventId(eventId)
                        .setEventType(eventType)
                    .setDetails(event.getDetails())
                    .build();

            eventService.processEvent(eventRequest
            );

            logger.info(() -> String.format("Successfully processed event with ID: %s", event.getEventId()));
        } catch (Exception e) {
            String errorMessage = String.format("Error processing event with ID: %s, type: %s", event.getEventId(), event.getEventType());
            logger.severe(() -> errorMessage + ": " + e.getMessage());

            // Update event status to failed
            event.setStatus(EventStatus.FAILED);
            event.setLastError(e.getMessage());

            // Rethrow with contextual information
            throw new EventProcessingException(errorMessage, e);
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