package com.smartface.keycloak.events.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "event_outbox")
public class EventOutbox {
    @Id
    private String id;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "topic")
    private String topic;

    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "id", insertable = false, updatable = false)
    private KeycloakEvent event;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}