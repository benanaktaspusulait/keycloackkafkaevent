package com.smartface.keycloak.events.repository;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class EventOutboxRepositoryImpl implements EventOutboxRepository, PanacheRepository<EventOutbox> {

    @Override
    public List<EventOutbox> findPendingEvents() {
        return list("status", EventStatus.PENDING);
    }

    @Override
    @Transactional
    public void updateStatus(String eventId, EventStatus status, String error) {
        EventOutbox event = find("eventId", eventId).firstResult();
        if (event != null) {
            event.setStatus( status);
            event.setLastError(error);
            persist(event);
        }
    }
} 