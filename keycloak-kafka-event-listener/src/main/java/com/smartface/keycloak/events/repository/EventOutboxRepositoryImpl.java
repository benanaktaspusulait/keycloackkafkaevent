package com.smartface.keycloak.events.repository;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class EventOutboxRepositoryImpl implements EventOutboxRepository {
    @Override
    public void persist(EventOutbox entity) {
        PanacheRepository.super.persist(entity);
    }

    @Override
    public List<EventOutbox> findPendingEvents() {
        return find("status = ?1 AND (retryCount < 3 OR retryCount IS NULL) ORDER BY createdAt",
                EventStatus.PENDING)
                .page(0, 10)
                .list();
    }

    @Override
    public void updateStatus(String id, EventStatus status, String error) {
        EventOutbox event = findById(id);
        if (event != null) {
            event.setStatus(status);
            event.setLastError(error);
            if (status == EventStatus.FAILED) {
                event.setRetryCount(event.getRetryCount() + 1);
            } else if (status == EventStatus.PUBLISHED) {
                event.setPublishedAt(java.time.Instant.now());
            }
            persist(event);
        }
    }
} 