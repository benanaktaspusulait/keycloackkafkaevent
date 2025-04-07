package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class EventServiceImpl implements EventService {
    private static final Logger LOG = Logger.getLogger(EventServiceImpl.class);

    @Inject
    EventOutboxRepository eventOutboxRepository;

    @Override
    @Transactional
    public void processEvent(String eventId, String eventType, String realmId, String clientId, 
                           String userId, String sessionId, String ipAddress, String error, String details) {
        LOG.infof("Processing event with ID: %s", eventId);
        
        EventOutbox eventOutbox = new EventOutbox();
        eventOutbox.eventId = eventId;
        eventOutbox.eventType = eventType;
        eventOutbox.realmId = realmId;
        eventOutbox.clientId = clientId;
        eventOutbox.userId = userId;
        eventOutbox.sessionId = sessionId;
        eventOutbox.ipAddress = ipAddress;
        eventOutbox.error = error;
        eventOutbox.details = details;
        eventOutbox.status = EventStatus.PENDING;
        
        eventOutboxRepository.persist(eventOutbox);
        LOG.infof("Event with ID %s has been processed and saved", eventId);
    }

    @Override
    @Transactional
    public List<EventOutbox> findPendingEvents() {
        LOG.info("Finding pending events");
        return eventOutboxRepository.findPendingEvents();
    }

    @Override
    @Transactional
    public void updateEventStatus(String eventId, EventStatus status, String error) {
        LOG.infof("Updating event status for event ID: %s to %s", eventId, status);
        eventOutboxRepository.updateStatus(eventId, status, error);
    }
}