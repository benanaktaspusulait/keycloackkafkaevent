package com.smartface.keycloak.events.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "event_details")
public class EventDetails extends PanacheEntity {
    @Column(name = "event_id")
    public String eventId;

    @Column(name = "detail_key")
    public String key;

    @Column(name = "detail_value")
    public String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "id", insertable = false, updatable = false)
    public KeycloakEvent event;
} 