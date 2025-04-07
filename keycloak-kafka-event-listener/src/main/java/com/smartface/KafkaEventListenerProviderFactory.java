package com.smartface;

import com.smartface.keycloak.events.repository.EventDetailsRepository;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import com.smartface.keycloak.events.repository.KeycloakEventRepository;
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

    @Inject
    KeycloakEventRepository eventRepository;

    @Inject
    EventDetailsRepository detailsRepository;

    @Inject
    EventOutboxRepository outboxRepository;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        String bootstrapServers = System.getenv("KC_EVENTS_LISTENER_KAFKA_BOOTSTRAP_SERVERS");
        String topic = System.getenv("KC_EVENTS_LISTENER_KAFKA_TOPIC");
        String clientId = System.getenv("KC_EVENTS_LISTENER_KAFKA_CLIENT_ID");
        
        return new KafkaEventListenerProvider(
            bootstrapServers, 
            topic, 
            clientId,
            eventRepository,
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
