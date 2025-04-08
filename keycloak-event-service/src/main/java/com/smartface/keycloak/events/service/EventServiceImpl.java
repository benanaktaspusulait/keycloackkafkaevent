package com.smartface.keycloak.events.service;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import com.smartface.keycloak.grpc.EventRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

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
    public void processEvent(EventRequest eventRequest) {
        String eventId = eventRequest.getEventId();
        LOG.infof("Processing event with ID: %s", eventId);
        
        EventOutbox eventOutbox = new EventOutbox();
        eventOutbox.setEventId( eventId );
        eventOutbox.setEventType(eventRequest.getEventType());
        eventOutbox.setRealmId (eventRequest.getRealmId());
        eventOutbox.setClientId (eventRequest.getClientId());
        eventOutbox.setUserId ( eventRequest.getUserId());
        eventOutbox.setSessionId (eventRequest.getSessionId());
        eventOutbox.setIpAddress (eventRequest.getIpAddress());
        eventOutbox.setError(eventRequest.getError());
        eventOutbox.setDetails (eventOutbox.getDetails());
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