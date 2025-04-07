package com.smartface.keycloak.events.repository;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@ApplicationScoped
public interface EventOutboxRepository extends PanacheRepository<EventOutbox> {
    List<EventOutbox> findPendingEvents();
    void updateStatus(String eventId, EventStatus status, String error);
} 