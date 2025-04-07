package com.smartface.keycloak.events.repository;

import com.smartface.keycloak.events.entity.EventOutbox;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.logging.Logger;

public class EventOutboxRepository {
    private static final Logger LOGGER = Logger.getLogger(EventOutboxRepository.class.getName());
    private static final String ERROR_MESSAGE = "Failed to perform repository operation: ";

    private final EntityManager entityManager;

    public EventOutboxRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void save(EventOutbox outbox) {
        try {
            entityManager.persist(outbox);
        } catch (Exception e) {
            LOGGER.severe(ERROR_MESSAGE + e.getMessage());
            throw new RuntimeException(ERROR_MESSAGE + "save", e);
        }
    }


} 