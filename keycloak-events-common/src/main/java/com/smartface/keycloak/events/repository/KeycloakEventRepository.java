package com.smartface.keycloak.events.repository;

import com.smartface.keycloak.events.entity.KeycloakEvent;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class KeycloakEventRepository implements PanacheRepository<KeycloakEvent> {
}