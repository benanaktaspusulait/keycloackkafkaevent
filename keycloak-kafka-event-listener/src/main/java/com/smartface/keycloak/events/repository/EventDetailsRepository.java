package com.smartface.keycloak.events.repository;

import com.smartface.keycloak.events.entity.EventDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.logging.Logger;

public class EventDetailsRepository {
    private static final Logger LOGGER = Logger.getLogger(EventDetailsRepository.class.getName());
    private static final String ERROR_MESSAGE = "Failed to perform repository operation: ";

    private final EntityManager entityManager;

    public EventDetailsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void save(EventDetails details) {
        try {
            entityManager.persist(details);
        } catch (Exception e) {
            LOGGER.severe(ERROR_MESSAGE + e.getMessage());
            throw new RuntimeException(ERROR_MESSAGE + "save", e);
        }
    }

    public List<EventDetails> findByEventId(String eventId) {
        try {
            return entityManager.createQuery(
                    "SELECT d FROM EventDetails d WHERE d.eventId = :eventId", EventDetails.class)
                    .setParameter("eventId", eventId)
                    .getResultList();
        } catch (Exception e) {
            LOGGER.severe(ERROR_MESSAGE + e.getMessage());
            throw new RuntimeException(ERROR_MESSAGE + "findByEventId", e);
        }
    }
} 