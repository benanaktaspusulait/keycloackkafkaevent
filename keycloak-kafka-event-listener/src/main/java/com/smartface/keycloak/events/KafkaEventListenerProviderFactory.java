package com.smartface.keycloak.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.logging.Logger;

/**
 * Factory class to provide KafkaEventListenerProvider instances.
 */
public class KafkaEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(KafkaEventListenerProviderFactory.class.getName());

    private static final String ENV_BOOTSTRAP_SERVERS = "KC_EVENTS_LISTENER_KAFKA_BOOTSTRAP_SERVERS";
    private static final String ENV_TOPIC = "KC_EVENTS_LISTENER_KAFKA_TOPIC";
    private static final String ENV_CLIENT_ID = "KC_EVENTS_LISTENER_KAFKA_CLIENT_ID";

    private String bootstrapServers;
    private String topic;
    private String clientId;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        if (bootstrapServers == null || topic == null || clientId == null) {
            LOGGER.severe("Kafka configuration is incomplete. Cannot create KafkaEventListenerProvider.");
            return null;
        }
        return new KafkaEventListenerProvider(bootstrapServers, topic, clientId);
    }

    @Override
    public void init(Config.Scope config) {
        bootstrapServers = System.getenv(ENV_BOOTSTRAP_SERVERS);
        topic = System.getenv(ENV_TOPIC);
        clientId = System.getenv(ENV_CLIENT_ID);

        if (bootstrapServers == null || topic == null || clientId == null) {
            LOGGER.warning("One or more required environment variables are not set for KafkaEventListenerProviderFactory.");
        } else LOGGER.info("KafkaEventListenerProviderFactory initialized with topic: {}%s".formatted(topic));
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        LOGGER.fine("KafkaEventListenerProviderFactory postInit called.");
    }

    @Override
    public void close() {
        LOGGER.fine("KafkaEventListenerProviderFactory closed.");
    }

    @Override
    public String getId() {
        return "kafka";
    }
}
