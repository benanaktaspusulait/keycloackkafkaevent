package com.smartface.keycloak.kafka;

import com.smartface.keycloak.entity.KeycloakEvent;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Slf4j
@ApplicationScoped
public class EventProducer {

    @Channel("events")
    Emitter<Record<String, KeycloakEvent>> emitter;

    public void send(KeycloakEvent event) {
        log.debug("Sending event to Kafka: {}", event.getEventId());
        emitter.send(Record.of(event.getEventId(), event));
    }
} 