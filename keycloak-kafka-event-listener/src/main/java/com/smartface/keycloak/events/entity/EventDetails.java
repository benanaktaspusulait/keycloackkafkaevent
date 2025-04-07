package com.smartface.keycloak.events.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "event_details")
public class EventDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "detail_key")
    private String key;

    @Column(name = "detail_value")
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "id", insertable = false, updatable = false)
    private KeycloakEvent event;
} 