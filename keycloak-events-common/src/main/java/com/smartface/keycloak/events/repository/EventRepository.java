<<<<<<<< HEAD:keycloak-event-service/src/main/java/com/smartface/event/repository/EventRepository.java
package com.smartface.event.repository;

import com.smartface.event.entity.EventEntity;
========
package com.smartface.keycloak.events.repository;

import com.smartface.keycloak.events.entity.EventEntity;
>>>>>>>> 58a7f57 (jpa update):keycloak-events-common/src/main/java/com/smartface/keycloak/events/repository/EventRepository.java
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class EventRepository implements PanacheRepository<EventEntity> {
} 