package com.smartface.keycloak;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@Slf4j
public class KafkaEventListenerProviderFactory implements EventListenerProviderFactory {
    private String bootstrapServers;
    private String topic;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new KafkaEventListenerProvider(bootstrapServers, topic);
    }

    @Override
    public void init(Config.Scope config) {
        bootstrapServers = config.get("bootstrapServers", System.getenv("KC_EVENTS_KAFKA_BOOTSTRAP_SERVERS"));
        topic = config.get("topic", "keycloak_events");
        log.info("Initialized Kafka event listener provider with bootstrap servers: {} and topic: {}", bootstrapServers, topic);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post initialization needed
    }

    @Override
    public void close() {
        log.info("Closing Kafka event listener provider factory");
    }

    @Override
    public String getId() {
        return "kafka_event_listener";
    }
} 