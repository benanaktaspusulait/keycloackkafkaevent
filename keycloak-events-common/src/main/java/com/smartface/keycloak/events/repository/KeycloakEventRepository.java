package com.smartface.keycloak.events.repository;

import com.smartface.keycloak.events.entity.KeycloakEvent;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.logging.Logger;

public class KeycloakEventRepository {
    private static final Logger LOGGER = Logger.getLogger(KeycloakEventRepository.class.getName());
    private static final String ERROR_MESSAGE = "Failed to perform repository operation: ";

    private final EntityManager entityManager;

    public KeycloakEventRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void save(KeycloakEvent event) {
        try {
            entityManager.persist(event);
        } catch (Exception e) {
            LOGGER.severe(ERROR_MESSAGE + e.getMessage());
            throw new RuntimeException(ERROR_MESSAGE + "save", e);
        }
    }

} 