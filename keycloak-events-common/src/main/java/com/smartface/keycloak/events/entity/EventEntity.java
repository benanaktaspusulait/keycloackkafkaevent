<<<<<<<< HEAD:keycloak-event-service/src/main/java/com/smartface/event/entity/EventEntity.java
package com.smartface.event.entity;
========
package com.smartface.keycloak.events.entity;
>>>>>>>> 58a7f57 (jpa update):keycloak-events-common/src/main/java/com/smartface/keycloak/events/entity/EventEntity.java

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "keycloak_events")
public class EventEntity {
    @Id
    @Column(name = "id")
    private String id;

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