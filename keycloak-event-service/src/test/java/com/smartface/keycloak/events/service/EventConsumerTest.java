package com.smartface.keycloak.events.service;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.UUID;

@QuarkusTest
class EventConsumerTest {

    @InjectMock
    EventService eventService;

    @Inject
    EventConsumer eventConsumer; // Inject the EventConsumer

    @Test
    void testConsumeEvent() {
        String eventId = UUID.randomUUID().toString();
        String eventType = "LOGIN";
        String details = """
            {
                "realmId": "test-realm",
                "clientId": "test-client",
                "userId": "test-user",
                "sessionId": "test-session",
                "ipAddress": "127.0.0.1",
                "timestamp": "%s"
            }
            """.formatted(Instant.now());

        eventConsumer.consume(eventId, eventType, details);

        Mockito.verify(eventService).processEvent(
                Mockito.eq(eventId),
                Mockito.eq(eventType),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.eq(details)
        );
    }

    @Test
    void testConsumeEventWithMinimalDetails() {
        String eventId = UUID.randomUUID().toString();
        String eventType = "LOGIN";
        String details = """
            {
                "realmId": "test-realm"
            }
            """;

        eventConsumer.consume(eventId, eventType, details);

        Mockito.verify(eventService).processEvent(
                Mockito.eq(eventId),
                Mockito.eq(eventType),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.eq(details)
        );
    }
}