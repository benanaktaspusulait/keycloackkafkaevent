package com.smartface.keycloak.events.repository;

import com.smartface.keycloak.events.entity.EventDetails;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class EventDetailsRepository implements PanacheRepository<EventDetails> {

    @Transactional
    public void save(EventDetails details) {
        persist(details);
    }

    public List<EventDetails> findByEventId(String eventId) {
        return find("eventId", eventId).list();
    }
}