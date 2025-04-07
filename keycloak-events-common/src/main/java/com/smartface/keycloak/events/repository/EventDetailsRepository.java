package com.smartface.keycloak.events.repository;

import com.smartface.keycloak.events.entity.EventDetails;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventDetailsRepository implements PanacheRepository<EventDetails> {
}