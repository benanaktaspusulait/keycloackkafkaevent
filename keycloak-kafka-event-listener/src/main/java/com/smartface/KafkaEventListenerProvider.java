package com.smartface;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartface.keycloak.events.entity.EventDetails;
import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import com.smartface.keycloak.events.entity.KeycloakEvent;
import com.smartface.keycloak.events.repository.EventDetailsRepository;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import com.smartface.keycloak.events.repository.KeycloakEventRepository;
import jakarta.transaction.Transactional;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import java.time.Instant;
import java.util.Map;
import java.util.logging.Logger;

public class KafkaEventListenerProvider implements EventListenerProvider {

    private static final Logger LOGGER = Logger.getLogger(KafkaEventListenerProvider.class.getName());
    private static final String ERROR_MESSAGE = "Failed to process event: ";

    private final String bootstrapServers;
    private final String topic;
    private final String clientId;
    private final ObjectMapper objectMapper;
    private final KeycloakEventRepository eventRepository;
    private final EventDetailsRepository detailsRepository;
    private final EventOutboxRepository outboxRepository;

    public KafkaEventListenerProvider(String bootstrapServers, String topic, String clientId,
                                    KeycloakEventRepository eventRepository,
                                    EventDetailsRepository detailsRepository,
                                    EventOutboxRepository outboxRepository) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.clientId = clientId;
        this.objectMapper = new ObjectMapper();
        this.eventRepository = eventRepository;
        this.detailsRepository = detailsRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    private void storeEventDetails(String eventId, Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : details.entrySet()) {
            EventDetails eventDetails = new EventDetails();
            eventDetails.setEventId(eventId);
            eventDetails.setKey(entry.getKey());
            eventDetails.setValue(entry.getValue());
            detailsRepository.persist(eventDetails);
        }
    }

    @Transactional
    private void storeInOutbox(String eventId, String eventJson) {
        EventOutbox outbox = new EventOutbox();
        outbox.eventId = eventId;
        outbox.eventType = "EVENT";
        outbox.details = eventJson;
        outbox.status = EventStatus.PENDING;
        outbox.retryCount = 0;
        outbox.createdAt = Instant.now();
        outboxRepository.persist(outbox);
    }

    @Transactional
    private void processEvent(Event event) {
        try {
            KeycloakEvent keycloakEvent = new KeycloakEvent();
            keycloakEvent.setId(event.getId());
            keycloakEvent.setTime(Instant.ofEpochMilli(event.getTime() <= 0 ? System.currentTimeMillis() : event.getTime()));
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
                LOGGER.warning("Failed to serialize event details: " + e.getMessage());
                keycloakEvent.setDetails("{}");
            }
            
            eventRepository.persist(keycloakEvent);
            storeEventDetails(event.getId(), event.getDetails());
            storeInOutbox(event.getId(), objectMapper.writeValueAsString(event));

            LOGGER.info("Successfully processed event: " + event.getId());
        } catch (Exception e) {
            LOGGER.severe(ERROR_MESSAGE + e.getMessage());
            throw new RuntimeException(ERROR_MESSAGE + event.getId(), e);
        }
    }

    @Transactional
    private void processAdminEvent(AdminEvent adminEvent) {
        try {
            KeycloakEvent keycloakEvent = new KeycloakEvent();
            keycloakEvent.setId(adminEvent.getId());
            keycloakEvent.setTime(Instant.ofEpochMilli(adminEvent.getTime()));
            keycloakEvent.setType("ADMIN_" + adminEvent.getOperationType().name());
            keycloakEvent.setRealmId(adminEvent.getRealmId());
            keycloakEvent.setClientId(adminEvent.getResourceType().name());
            keycloakEvent.setUserId(adminEvent.getAuthDetails().getUserId());
            
            try {
                keycloakEvent.setDetails(objectMapper.writeValueAsString(adminEvent.getRepresentation()));
            } catch (JsonProcessingException e) {
                LOGGER.warning("Failed to serialize admin event representation: " + e.getMessage());
                keycloakEvent.setDetails("{}");
            }
            
            eventRepository.persist(keycloakEvent);

            if (adminEvent.getRepresentation() != null) {
                Map<String, String> details = Map.of(
                    "resourceType", adminEvent.getResourceType().name(),
                    "operationType", adminEvent.getOperationType().name(),
                    "representation", adminEvent.getRepresentation()
                );
                storeEventDetails(adminEvent.getId(), details);
            }

            storeInOutbox(adminEvent.getId(), objectMapper.writeValueAsString(adminEvent));
            LOGGER.info("Successfully processed admin event: " + adminEvent.getId());
        } catch (Exception e) {
            LOGGER.severe(ERROR_MESSAGE + e.getMessage());
            throw new RuntimeException(ERROR_MESSAGE + adminEvent.getId(), e);
        }
    }

    @Override
    public void onEvent(Event event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            LOGGER.severe(ERROR_MESSAGE + e.getMessage());
            throw new RuntimeException(ERROR_MESSAGE + event.getId(), e);
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        try {
            processAdminEvent(adminEvent);
        } catch (Exception e) {
            LOGGER.severe(ERROR_MESSAGE + e.getMessage());
            throw new RuntimeException(ERROR_MESSAGE + adminEvent.getId(), e);
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
