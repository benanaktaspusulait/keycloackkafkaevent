package com.smartface.keycloak.events.repository;

import com.smartface.keycloak.events.entity.EventDetails;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public interface EventDetailsRepository extends PanacheRepository<EventDetails> {
    List<EventDetails> findByEventId(String eventId);
} 