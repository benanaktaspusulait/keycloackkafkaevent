package com.smartface.keycloak.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class KafkaEventListenerProviderFactory implements EventListenerProviderFactory {
    private String bootstrapServers;
    private String topic;
    private String clientId;

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new KafkaEventListenerProvider(bootstrapServers, topic, clientId);
    }

    @Override
    public void init(Config.Scope config) {
        bootstrapServers = System.getenv("KC_EVENTS_LISTENER_KAFKA_BOOTSTRAP_SERVERS");
        topic = System.getenv("KC_EVENTS_LISTENER_KAFKA_TOPIC");
        clientId = System.getenv("KC_EVENTS_LISTENER_KAFKA_CLIENT_ID");
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "kafka";
    }
} 