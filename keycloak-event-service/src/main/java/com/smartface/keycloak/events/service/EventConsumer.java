package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import java.time.Instant;

@ApplicationScoped
public class EventConsumer {
    private static final Logger LOG = Logger.getLogger(EventConsumer.class);

    @Inject
    EventService eventService;

    @Transactional
    public void consume(String eventId, String eventType, String details) {
        LOG.infof("Consuming event with ID: %s", eventId);

        EventOutbox event = new EventOutbox();
        event.eventId = eventId;
        event.eventType = eventType;
        event.details = details;
        event.status = EventStatus.PENDING;
        event.retryCount = 0;
        event.createdAt = Instant.now();

        try {
            LOG.infof("Processing event with ID: %s, type: %s", event.eventId, event.eventType);
            eventService.processEvent(
                event.eventId,
                event.eventType,
                null,
                null,
                null,
                null,
                null,
                null,
                event.details
            );
            LOG.infof("Successfully processed event with ID: %s", event.eventId);
        } catch (Exception e) {
            LOG.errorf("Error processing event with ID: %s - %s", event.eventId, e.getMessage());
            throw e;
        }
    }
}