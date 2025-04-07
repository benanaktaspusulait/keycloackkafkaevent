package com.smartface.keycloak.events.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "event_outbox")
public class EventOutbox extends PanacheEntity {

    @Column(name = "event_id", unique = true)
    public String eventId;

    @Column(name = "event_type")
    public String eventType;

    @Column(name = "realm_id")
    public String realmId;

    @Column(name = "client_id")
    public String clientId;

    @Column(name = "user_id")
    public String userId;

    @Column(name = "session_id")
    public String sessionId;

    @Column(name = "ip_address")
    public String ipAddress;

    @Column(name = "error")
    public String error;

    @Column(name = "details", columnDefinition = "jsonb")
    public String details;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    public EventStatus status;

    @Column(name = "retry_count")
    public Integer retryCount;

    @Column(name = "last_error")
    public String lastError;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @Column(name = "published_at")
    public Instant publishedAt;

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