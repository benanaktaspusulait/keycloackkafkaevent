package com.smartface;

import com.smartface.keycloak.events.repository.EventDetailsRepository;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import com.smartface.keycloak.events.repository.EventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory class to provide KafkaEventListenerProvider instances.
 */
@ApplicationScoped
public class KafkaEventListenerProviderFactory implements EventListenerProviderFactory {

    private final EventRepository eventRepository;
    private final EventDetailsRepository detailsRepository;
    private final EventOutboxRepository outboxRepository;

    @Inject
    public KafkaEventListenerProviderFactory(
            EventRepository eventRepository,
            EventDetailsRepository detailsRepository,
            EventOutboxRepository outboxRepository
    ) {
        this.eventRepository = eventRepository;
        this.detailsRepository = detailsRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {


        return new KafkaEventListenerProvider(eventRepository,
                detailsRepository,
                outboxRepository
        );
    }

    @Override
    public void init(Config.Scope config) {
        // Nothing to do
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public String getId() {
        return "kafka";
    }
}
