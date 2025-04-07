package com.smartface.repository;

import com.smartface.entity.EventStatus;
import com.smartface.entity.OutboxEvent;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class OutboxEventRepository implements PanacheRepository<OutboxEvent> {

    public List<OutboxEvent> findPendingEvents() {
        return find("status = ?1 AND (retryCount < 3 OR retryCount IS NULL) ORDER BY createdAt",
                EventStatus.PENDING)
                .page(0, 10)
                .list();
    }

    @Transactional
    public void updateStatus(Long id, EventStatus status, String error) {
        OutboxEvent event = findById(id);
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