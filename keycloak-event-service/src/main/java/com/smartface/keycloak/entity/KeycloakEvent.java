package com.smartface.keycloak.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "keycloak_events")
@Data
@EqualsAndHashCode(callSuper = false)
public class KeycloakEvent extends PanacheEntityBase {
    
    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "realm_id", nullable = false)
    private String realmId;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "event_time")
    private Instant eventTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_details", 
        joinColumns = @JoinColumn(name = "event_id"))
    @MapKeyColumn(name = "detail_key")
    @Column(name = "detail_value")
    private Map<String, String> details;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @Version
    private Long version;
} 