package com.smartface.keycloak.events.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "event_details")
public class EventDetails extends PanacheEntity {

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