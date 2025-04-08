package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import io.quarkus.scheduler.Scheduled;
import java.util.List;

@ApplicationScoped
public class OutboxPoller {
    private static final Logger LOG = Logger.getLogger(OutboxPoller.class);
    private final EventOutboxRepository eventOutboxRepository;
    private final Emitter<Record<String, String>> emitter;

    @Inject
    public OutboxPoller(
            EventOutboxRepository eventOutboxRepository,
            @Channel("events") Emitter<Record<String, String>> emitter) {
        this.eventOutboxRepository = eventOutboxRepository;
        this.emitter = emitter;
    }

    @Scheduled(every = "30s")
    public void pollOutbox() {
        List<EventOutbox> pendingEvents = eventOutboxRepository.findPendingEvents();
        for (EventOutbox event : pendingEvents) {
            try {
                // Create a Kafka record with event ID as key and event details as value
                Record<String, String> record = Record.of(event.getEventId(), event.getDetails());
                emitter.send(record);

                // Update event status to PUBLISHED
                eventOutboxRepository.updateStatus(event.getEventId(), EventStatus.PUBLISHED, null);
                LOG.infof("Successfully published event: %s", event.getEventId());
            } catch (Exception e) {
                LOG.errorf("Error publishing event: %s - %s", event.getEventId(), e.getMessage());
                // Update event status to FAILED and increment retry count
                eventOutboxRepository.updateStatus(event.getEventId(), EventStatus.FAILED, e.getMessage());
            }
        }

        if (pendingEvents.isEmpty()) {
            LOG.debug("No pending events found to process");
        }
    }
} 