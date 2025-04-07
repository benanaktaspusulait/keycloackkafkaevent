package com.smartface.keycloak.events.repository;

import com.smartface.keycloak.events.entity.EventOutbox;
import com.smartface.keycloak.events.entity.EventStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class EventOutboxRepositoryImpl implements EventOutboxRepository {
    private final PanacheRepository<EventOutbox> repository;

    public EventOutboxRepositoryImpl() {
        this.repository = new PanacheRepository<EventOutbox>() {};
    }

    @Transactional
    public void save(EventOutbox outbox) {
        repository.persist(outbox);
    }

    @Override
    public List<EventOutbox> findPendingEvents() {
        return repository.find("status = ?1 AND (retryCount < 3 OR retryCount IS NULL) ORDER BY createdAt",
                EventStatus.PENDING)
                .page(0, 10)
                .list();
    }

    @Override
    @Transactional
    public void updateStatus(String id, EventStatus status, String error) {
        EventOutbox event = repository.findById(Long.valueOf(id));
        if (event != null) {
            event.setStatus(status);
            event.setLastError(error);
            if (status == EventStatus.FAILED) {
                event.setRetryCount(event.getRetryCount() + 1);
            } else if (status == EventStatus.PUBLISHED) {
                event.setPublishedAt(java.time.Instant.now());
            }
            repository.persist(event);
        }
    }
} 