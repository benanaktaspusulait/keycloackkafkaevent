package com.smartface;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartface.exception.AdminEventProcessingException;
import com.smartface.exception.EventOutboxException;
import com.smartface.exception.EventPersistenceException;
import com.smartface.exception.EventProcessingException;
import com.smartface.keycloak.events.entity.EventDetails;
import com.smartface.keycloak.events.entity.EventEntity;
import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import com.smartface.keycloak.events.repository.EventDetailsRepository;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import com.smartface.keycloak.events.repository.EventRepository;
import jakarta.transaction.Transactional;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KafkaEventListenerProvider implements EventListenerProvider {

    private static final Logger LOGGER = Logger.getLogger(KafkaEventListenerProvider.class.getName());
    private static final String ERROR_MESSAGE = "Failed to process event: ";
    private static final String EVENT_TYPE = "EVENT";
    private static final String DETAILS_JSON_EMPTY = "{}";
    private static final String ADMIN =   "ADMIN_";

    private final ObjectMapper objectMapper;
    private final EventRepository eventRepository;
    private final EventDetailsRepository detailsRepository;
    private final EventOutboxRepository outboxRepository;

    public KafkaEventListenerProvider(EventRepository eventRepository,
                                      EventDetailsRepository detailsRepository,
                                      EventOutboxRepository outboxRepository) {
        this.objectMapper = new ObjectMapper();
        this.eventRepository = eventRepository;
        this.detailsRepository = detailsRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    private void storeEventDetails(String eventId, Map<String, String> details) {
        if (details != null && !details.isEmpty()) {
            for (Map.Entry<String, String> entry : details.entrySet()) {
                EventDetails eventDetails = new EventDetails();
                eventDetails.setEventId(eventId);
                eventDetails.setKey(entry.getKey());
                eventDetails.setValue(entry.getValue());
                detailsRepository.persist(eventDetails);
            }
        }
    }

    @Transactional
    private void processEvent(Event event) throws Exception {
        // 1. Create and persist main event entity
        EventEntity keycloakEvent = createEventEntity(event);
        persistEventEntity(keycloakEvent);

        // 2. Store event details (handled gracefully if fails)
        storeEventDetailsGracefully(event);

        // 3. Store in outbox (critical operation)
        storeEventInOutboxWithRetry(event);

        LOGGER.log(Level.INFO, "Successfully processed event: {0}", event.getId());
    }

    private void persistEventEntity(EventEntity entity) throws EventPersistenceException {
        try {
            eventRepository.persist(entity);
        } catch (Exception e) {
            throw new EventPersistenceException("Failed to persist event entity", e);
        }
    }

    private void storeEventDetailsGracefully(Event event) {
        storeEventDetails(event.getId(), event.getDetails());
    }

    private void storeEventInOutboxWithRetry(Event event) throws EventOutboxException {
        try {
            storeEventInOutbox(event);
        } catch (Exception e) {
            try {
                LOGGER.log(Level.WARNING, "First attempt failed, retrying outbox storage for event {0}", event.getId());
                storeEventInOutbox(event); // Single retry attempt
            } catch (Exception retryEx) {
                throw new EventOutboxException("Failed to store in outbox after retry: " + event.getId(), retryEx);
            }
        }
    }


    private EventEntity createEventEntity(Event event) {
        EventEntity keycloakEvent = new EventEntity();
        keycloakEvent.setId(event.getId());
        keycloakEvent.setTime(Instant.ofEpochMilli(Math.max(event.getTime(), System.currentTimeMillis())));
        keycloakEvent.setType(event.getType().name());
        keycloakEvent.setRealmId(event.getRealmId());
        keycloakEvent.setClientId(event.getClientId());
        keycloakEvent.setUserId(event.getUserId());
        keycloakEvent.setSessionId(event.getSessionId());
        keycloakEvent.setIpAddress(event.getIpAddress());
        keycloakEvent.setError(event.getError());

        try {
            keycloakEvent.setDetails(objectMapper.writeValueAsString(event.getDetails()));
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, "Failed to serialize event details for event {0}: {1}",
                    new Object[]{event.getId(), e.getMessage()});
            keycloakEvent.setDetails(DETAILS_JSON_EMPTY);
        }

        return keycloakEvent;
    }

    private void storeEventInOutbox(Event event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            EventOutbox outbox = new EventOutbox();
            outbox.setEventId(event.getId());
            outbox.setEventType(EVENT_TYPE);
            outbox.setDetails(eventJson);
            outbox.setStatus(EventStatus.PENDING);
            outbox.setRetryCount(0);
            outbox.setCreatedAt(Instant.now());
            outboxRepository.persist(outbox);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Failed to serialize event {0} for outbox: {1}",
                    new Object[]{event.getId(), e.getMessage()});
            createFailedOutboxEntry(event, e);
        }
    }

    private void createFailedOutboxEntry(Event event, Exception cause) {
        EventOutbox outbox = new EventOutbox();
        outbox.setEventId(event.getId());
        outbox.setEventType(EVENT_TYPE);
        outbox.setDetails("Serialization Error: " + cause.getMessage());
        outbox.setStatus(EventStatus.FAILED);
        outbox.setRetryCount(0);
        outbox.setCreatedAt(Instant.now());
        outboxRepository.persist(outbox);
    }

    @Transactional
    private void processAdminEvent(AdminEvent adminEvent) {
        EventEntity keycloakEvent = createAdminEventEntity(adminEvent);
        eventRepository.persist(keycloakEvent);

        storeAdminEventDetails(adminEvent);
        storeAdminEventInOutbox(adminEvent);

        LOGGER.log(Level.INFO, "Successfully processed admin event: {0}", adminEvent.getId());
    }

    private EventEntity createAdminEventEntity(AdminEvent adminEvent) {
        EventEntity keycloakEvent = new EventEntity();
        keycloakEvent.setId(adminEvent.getId());
        keycloakEvent.setTime(Instant.ofEpochMilli(adminEvent.getTime()));
        keycloakEvent.setType(ADMIN + adminEvent.getOperationType().name());
        keycloakEvent.setRealmId(adminEvent.getRealmId());
        keycloakEvent.setClientId(adminEvent.getResourceType().name());
        keycloakEvent.setUserId(adminEvent.getAuthDetails().getUserId());

        try {
            keycloakEvent.setDetails(objectMapper.writeValueAsString(adminEvent.getRepresentation()));
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, "Failed to serialize admin event representation for event {0}: {1}",
                    new Object[]{adminEvent.getId(), e.getMessage()});
            keycloakEvent.setDetails(DETAILS_JSON_EMPTY);
        }

        return keycloakEvent;
    }

    private void storeAdminEventDetails(AdminEvent adminEvent) {
        if (adminEvent.getRepresentation() != null) {
            Map<String, String> details = Map.of(
                    "resourceType", adminEvent.getResourceType().name(),
                    "operationType", adminEvent.getOperationType().name(),
                    "representation", adminEvent.getRepresentation()
            );
            storeEventDetails(adminEvent.getId(), details);
        } else {
            storeEventDetails(adminEvent.getId(), Collections.emptyMap());
        }
    }

    private void storeAdminEventInOutbox(AdminEvent adminEvent) {
        try {
            String eventJson = objectMapper.writeValueAsString(adminEvent);
            EventOutbox outbox = new EventOutbox();
            outbox.setEventId(adminEvent.getId());
            outbox.setEventType(ADMIN + adminEvent.getOperationType().name());
            outbox.setDetails(eventJson);
            outbox.setStatus(EventStatus.PENDING);
            outbox.setRetryCount(0);
            outbox.setCreatedAt(Instant.now());
            outboxRepository.persist(outbox);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Failed to serialize admin event {0} for outbox: {1}",
                    new Object[]{adminEvent.getId(), e.getMessage()});
            createFailedAdminOutboxEntry(adminEvent, e);
        }
    }

    private void createFailedAdminOutboxEntry(AdminEvent adminEvent, Exception cause) {
        EventOutbox outbox = new EventOutbox();
        outbox.setEventId(adminEvent.getId());
        outbox.setEventType(ADMIN + adminEvent.getOperationType().name());
        outbox.setDetails("Serialization Error: " + cause.getMessage());
        outbox.setStatus(EventStatus.FAILED);
        outbox.setRetryCount(0);
        outbox.setCreatedAt(Instant.now());
        outboxRepository.persist(outbox);
    }

    @Override
    public void onEvent(Event event) {
        try {
            processEvent(event);
        } catch (EventProcessingException e) {
            // Already our custom exception, just rethrow
            throw e;
        } catch (Exception e) {
            String errorMessage = String.format("%s: %s", ERROR_MESSAGE, event.getId());
            LOGGER.log(Level.SEVERE, errorMessage, e);
            throw new EventProcessingException(errorMessage, e);
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) throws AdminEventProcessingException {
        try {
            processAdminEvent(adminEvent);
        } catch (AdminEventProcessingException e) {
            // Already our custom exception, just rethrow
            throw e;
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }
}