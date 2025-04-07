package com.smartface.keycloak.events.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "keycloak_events")
public class KeycloakEvent extends PanacheEntity {
    @Column(name = "id", unique = true)
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