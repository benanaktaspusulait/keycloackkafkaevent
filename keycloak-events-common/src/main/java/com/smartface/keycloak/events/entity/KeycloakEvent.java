package com.smartface.keycloak.events.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "keycloak_events")
public class KeycloakEvent extends PanacheEntity {
    @Column(name = "id", unique = true)
    public String id;

    @Column(name = "time")
    public Instant time;

    @Column(name = "type")
    public String type;

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
} 