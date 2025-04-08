package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class EventServiceImpl implements EventService {
    private static final Logger LOG = Logger.getLogger(EventServiceImpl.class);


    private final EventOutboxRepository eventOutboxRepository;

    @Inject
    public EventServiceImpl(EventOutboxRepository eventOutboxRepository) {
        this.eventOutboxRepository = eventOutboxRepository;
    }

    @Override
    @Transactional
    public void processEvent(String eventId, String eventType, String realmId, String clientId, 
                           String userId, String sessionId, String ipAddress, String error, String details) {
        LOG.infof("Processing event with ID: %s", eventId);
        
        EventOutbox eventOutbox = new EventOutbox();
        eventOutbox.setEventId( eventId);
        eventOutbox.setEventType(eventType);
        eventOutbox.setRealmId (realmId);
        eventOutbox.setClientId (clientId);
        eventOutbox.setUserId ( userId);
        eventOutbox.setSessionId (sessionId);
        eventOutbox.setIpAddress (ipAddress);
        eventOutbox.setError(error);
        eventOutbox.setDetails ( details);
        eventOutbox.setStatus(EventStatus.PENDING);
        
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