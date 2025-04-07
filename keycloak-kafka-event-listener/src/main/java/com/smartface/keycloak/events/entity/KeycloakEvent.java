package com.smartface.keycloak.events.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "keycloak_events")
public class KeycloakEvent {
    @Id
    private String id;

    @Column(name = "time")
    private Instant time;

    @Column(name = "type")
    private String type;

    @Column(name = "realm_id")
    private String realmId;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "error")
    private String error;

    @Column(name = "details", columnDefinition = "jsonb")
    private String details;
} 