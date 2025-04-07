package com.smartface.keycloak.events.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "keycloak_events")
public class KeycloakEvent extends PanacheEntity {

    @Column(name = "time", nullable = false)
    private Instant time = Instant.now();

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