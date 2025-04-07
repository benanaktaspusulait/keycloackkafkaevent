package com.smartface.keycloak.events;

import com.smartface.keycloak.events.repository.EventDetailsRepository;
import com.smartface.keycloak.events.repository.EventOutboxRepository;
import com.smartface.keycloak.events.repository.KeycloakEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory class to provide KafkaEventListenerProvider instances.
 */
public class KafkaEventListenerProviderFactory implements EventListenerProviderFactory {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        String bootstrapServers = System.getenv("KC_EVENTS_LISTENER_KAFKA_BOOTSTRAP_SERVERS");
        String topic = System.getenv("KC_EVENTS_LISTENER_KAFKA_TOPIC");
        String clientId = System.getenv("KC_EVENTS_LISTENER_KAFKA_CLIENT_ID");
        
        // Initialize repositories with entity manager
        KeycloakEventRepository eventRepository = new KeycloakEventRepository(entityManager);
        EventDetailsRepository detailsRepository = new EventDetailsRepository(entityManager);
        EventOutboxRepository outboxRepository = new EventOutboxRepository(entityManager);
        
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
